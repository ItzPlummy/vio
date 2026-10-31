package com.plummy.vio.script;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.plummy.vio.Vio;
import com.plummy.vio.config.ScriptLimits;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.script.runtime.CommandRuntimeScript;
import com.plummy.vio.script.runtime.ListenerRuntimeScript;
import com.plummy.vio.script.runtime.RuntimeScript;
import com.plummy.vio.script.state.FileState;
import com.plummy.vio.script.state.MemoryState;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.transform.CompileStatic;
import groovy.transform.ConditionalInterrupt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class DefaultScriptManager implements ScriptManager {

    private static final Pattern COMMAND_NAME = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Field CHILDREN = findCommandNodeField("children");
    private static final Field LITERALS = findCommandNodeField("literals");

    private static long deadline = Long.MAX_VALUE;

    private final MinecraftServer server;
    private final ScriptStorage scriptStorage;
    private final ScriptSourceStorage scriptSourceStorage;
    private final MemoryState memoryState;
    private final FileState fileState;

    private final Map<ScriptKey, GroovyClassLoader> loaders = new HashMap<>();
    private final Map<ScriptKey, Class<? extends RuntimeScript>> runtimeClasses = new HashMap<>();
    private final Map<ScriptKey, ScriptLog> logs = new HashMap<>();
    private final Map<ScriptKey, Long> remainingTicks = new HashMap<>();
    private final Map<ScriptKey, LiteralCommandNode<CommandSourceStack>> commandNodes = new HashMap<>();
    private final Map<ScriptKey, List<Consumer<? extends Event>>> listenerHandlers = new HashMap<>();

    public DefaultScriptManager(MinecraftServer server) {
        this.server = server;
        this.scriptStorage = ScriptStorage.fromServer(server);
        this.scriptSourceStorage = ScriptSourceStorage.fromServer(server);
        this.memoryState = new MemoryState();
        this.fileState = FileState.fromServer(server);
    }

    public static boolean isBudgetExceeded() {
        return System.nanoTime() > deadline;
    }

    @Override
    public void load() {
        runOnServerThread(() -> {
            scriptSourceStorage.sync();

            for (Script script : scriptStorage.getItems()) {
                ScriptKey key = script.getKey();
                logs.put(key, new DefaultScriptLog(ScriptLimits.LOG_CAPACITY));

                try {
                    ScriptSource source = scriptSourceStorage.get(key);

                    if (source == null) {
                        throw new ScriptException("Its source file is missing");
                    }

                    compileAndInstall(script, source.getSource());

                    if (isEnabled(script)) {
                        activate(script);
                    }
                } catch (RuntimeException e) {
                    disposeLoader(key);
                    logs.get(key).error("Couldn't load the script: %s".formatted(e.getMessage()));
                    Vio.getLogger().warn("Failed to load script '%s': %s".formatted(key, e.getMessage()));
                }
            }
        });
    }

    @Override
    public void shutdown() {
        runOnServerThread(() -> {
            for (Script script : scriptStorage.getItems()) {
                if (isActive(script)) {
                    deactivate(script);
                }

                disposeLoader(script.getKey());
            }

            logs.clear();
        });
    }

    // Not hopped: at startup this fires before the server thread runs, so a hop would never finish
    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (LiteralCommandNode<CommandSourceStack> node : commandNodes.values()) {
            if (dispatcher.getRoot().getChild(node.getName()) == null) {
                dispatcher.getRoot().addChild(node);
            } else {
                Vio.getLogger().warn("Command /%s of a script is left out: another command with that name exists now".formatted(node.getName()));
            }
        }
    }

    @Override
    public void tick() {
        runOnServerThread(() -> {
            // A copy, since a running script may enable or disable scripts and change the map
            for (ScriptKey key : List.copyOf(remainingTicks.keySet())) {
                Long remaining = remainingTicks.get(key);

                if (remaining == null) {
                    continue;
                }

                if (remaining > 1) {
                    remainingTicks.put(key, remaining - 1);
                    continue;
                }

                PeriodicScript periodicScript = (PeriodicScript) scriptStorage.get(key);
                Class<? extends RuntimeScript> runtimeClass = runtimeClasses.get(key);

                remainingTicks.put(key, periodicScript.getPeriodTicks());
                invoke(periodicScript, ServerConfig.PERIODIC_BUDGET_MILLIS.get(), () -> createRuntimeScript(runtimeClass, logs.get(key)).run());
            }
        });
    }

    @Override
    public Script getScript(ScriptKey key) {
        return onServerThread(() -> scriptStorage.get(key));
    }

    @Override
    public ScriptSource getScriptSource(ScriptKey key) {
        return onServerThread(() -> scriptSourceStorage.get(key));
    }

    @Override
    public ScriptLog getScriptLog(ScriptKey key) {
        return onServerThread(() -> {
            ScriptLog log = logs.get(key);

            if (log == null) {
                throw new ScriptException("No script named '%s'".formatted(key));
            }

            return log;
        });
    }

    // Errored when it doesn't compile or couldn't be loaded; its log has the reason
    @Override
    public ScriptState getScriptState(ScriptKey key) {
        return onServerThread(() -> {
            Script script = requireScript(key);

            if (!runtimeClasses.containsKey(key)) {
                return ScriptState.ERRORED;
            }

            return script instanceof Toggleable toggleable && !toggleable.isEnabled() ? ScriptState.DISABLED : ScriptState.ENABLED;
        });
    }

    @Override
    public Collection<Script> getScripts() {
        return onServerThread(() -> List.copyOf(scriptStorage.getItems()));
    }

    @Override
    public void createScript(Script script, ScriptSource source) {
        runOnServerThread(() -> {
            ScriptKey key = script.getKey();

            if (!key.equals(source.getKey())) {
                throw new IllegalArgumentException("Script key '%s' doesn't match its source key '%s'".formatted(key, source.getKey()));
            }

            if (scriptStorage.has(key)) {
                throw new ScriptException("A script named '%s' already exists".formatted(key));
            }

            compileAndInstall(script, source.getSource());
            logs.put(key, new DefaultScriptLog(ScriptLimits.LOG_CAPACITY));

            boolean activated = false;

            try {
                if (isEnabled(script)) {
                    activate(script);
                    activated = true;
                }

                scriptSourceStorage.save(source);
                scriptStorage.save(script);
            } catch (RuntimeException e) {
                if (activated) {
                    deactivate(script);
                }

                disposeLoader(key);
                logs.remove(key);
                throw e;
            }
        });
    }

    @Override
    public void updateScriptSource(ScriptSource source) {
        runOnServerThread(() -> {
            ScriptKey key = source.getKey();
            Script script = requireScript(key);

            GroovyClassLoader loader = createLoader(script);
            Class<? extends RuntimeScript> runtimeClass;

            try {
                runtimeClass = createRuntimeClass(loader, script, source.getSource());
            } catch (RuntimeException e) {
                dispose(loader);
                throw e;
            }

            GroovyClassLoader previousLoader = loaders.get(key);
            Class<? extends RuntimeScript> previousClass = runtimeClasses.get(key);
            boolean wasActive = isActive(script);

            if (wasActive) {
                deactivate(script);
            }

            install(key, loader, runtimeClass);

            boolean activated = false;

            try {
                if (isEnabled(script)) {
                    activate(script);
                    activated = true;
                }

                scriptSourceStorage.save(source);
            } catch (RuntimeException e) {
                if (activated) {
                    deactivate(script);
                }

                dispose(loader);
                restore(key, previousLoader, previousClass);

                if (wasActive) {
                    try {
                        activate(script);
                    } catch (RuntimeException restoreError) {
                        disposeLoader(key);
                        logs.get(key).error("Couldn't restore the previous version: %s".formatted(restoreError.getMessage()));
                        Vio.getLogger().warn("Failed to restore script '%s': %s".formatted(key, restoreError.getMessage()));
                    }
                }

                throw e;
            }

            if (previousLoader != null) {
                dispose(previousLoader);
            }
        });
    }

    @Override
    public void deleteScript(ScriptKey key) {
        runOnServerThread(() -> {
            Script script = requireScript(key);
            boolean wasActive = isActive(script);

            if (wasActive) {
                deactivate(script);
            }

            try {
                scriptStorage.delete(key);
            } catch (RuntimeException e) {
                if (wasActive) {
                    activate(script);
                }

                throw e;
            }

            try {
                scriptSourceStorage.delete(key);
            } catch (RuntimeException e) {
                Vio.getLogger().warn("Failed to delete the source of script '%s'".formatted(key), e);
            }

            disposeLoader(key);
            logs.remove(key);
        });
    }

    @Override
    public ScriptOutcome execute(ScriptKey key) {
        return onServerThread(() -> {
            Script script = requireScript(key);

            if (!(script instanceof Executable)) {
                throw new ScriptException("'%s' is a %s script, which can't be run on demand".formatted(key, script.getType()));
            }

            Class<? extends RuntimeScript> runtimeClass = requireRuntimeClass(key);
            CapturingScriptLog output = new CapturingScriptLog(logs.get(key));

            ScriptOutcome outcome = invoke(script, ServerConfig.RUN_BUDGET_MILLIS.get(), () -> createRuntimeScript(runtimeClass, output).run());
            return outcome.withOutput(output.getEntries());
        });
    }

    @Override
    public void enable(ScriptKey key) {
        runOnServerThread(() -> {
            Toggleable toggleable = requireToggleable(key);

            if (toggleable.isEnabled()) {
                throw new ScriptException("'%s' is already enabled".formatted(key));
            }

            requireRuntimeClass(key);

            Toggleable enabled = toggleable.withEnabled(true);
            activate(enabled);

            try {
                scriptStorage.save(enabled);
            } catch (RuntimeException e) {
                deactivate(enabled);
                throw e;
            }
        });
    }

    @Override
    public void disable(ScriptKey key) {
        runOnServerThread(() -> {
            Toggleable toggleable = requireToggleable(key);

            if (!toggleable.isEnabled()) {
                throw new ScriptException("'%s' is already disabled".formatted(key));
            }

            boolean wasActive = isActive(toggleable);

            if (wasActive) {
                deactivate(toggleable);
            }

            Toggleable disabled = toggleable.withEnabled(false);

            try {
                scriptStorage.save(disabled);
            } catch (RuntimeException e) {
                if (wasActive) {
                    activate(toggleable);
                }

                throw e;
            }
        });
    }

    private <T> T onServerThread(Supplier<T> action) {
        if (server.isSameThread()) {
            return action.get();
        }

        try {
            return server.submit(action).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            if (e.getCause() instanceof Error cause) {
                throw cause;
            }

            throw e;
        }
    }

    private void runOnServerThread(Runnable action) {
        onServerThread(() -> {
            action.run();
            return null;
        });
    }

    private Script requireScript(ScriptKey key) {
        Script script = scriptStorage.get(key);

        if (script == null) {
            throw new ScriptException("No script named '%s'".formatted(key));
        }

        return script;
    }

    private Toggleable requireToggleable(ScriptKey key) {
        Script script = requireScript(key);

        if (!(script instanceof Toggleable toggleable)) {
            throw new ScriptException("'%s' is a %s script, which can't be enabled or disabled".formatted(key, script.getType()));
        }

        return toggleable;
    }

    private Class<? extends RuntimeScript> requireRuntimeClass(ScriptKey key) {
        Class<? extends RuntimeScript> runtimeClass = runtimeClasses.get(key);

        if (runtimeClass == null) {
            throw new ScriptException("'%s' doesn't compile, so it can't run. Fix its source first".formatted(key));
        }

        return runtimeClass;
    }

    private static boolean isEnabled(Script script) {
        return script instanceof Toggleable toggleable && toggleable.isEnabled();
    }

    private boolean isActive(Script script) {
        return isEnabled(script) && runtimeClasses.containsKey(script.getKey());
    }

    private void activate(Script script) {
        switch (script) {
            case PeriodicScript periodicScript -> remainingTicks.put(periodicScript.getKey(), periodicScript.getPeriodTicks());
            case CommandScript commandScript -> registerCommand(commandScript);
            case ListenerScript listenerScript -> registerListener(listenerScript);
            default -> {
            }
        }
    }

    private void deactivate(Script script) {
        switch (script) {
            case PeriodicScript periodicScript -> remainingTicks.remove(periodicScript.getKey());
            case CommandScript commandScript -> unregisterCommand(commandScript);
            case ListenerScript listenerScript -> unregisterListener(listenerScript);
            default -> {
            }
        }
    }

    private ScriptOutcome invoke(Script script, int budgetMillis, Callable<Object> call) {
        long start = System.nanoTime();
        long previous = deadline;
        boolean limitedByCaller = previous != Long.MAX_VALUE && previous - start < budgetMillis * 1_000_000L;

        deadline = Math.min(previous, start + budgetMillis * 1_000_000L);

        try {
            return new ScriptOutcome(call.call(), null, null, List.of(), (System.nanoTime() - start) / 1_000_000);
        } catch (ScriptTimeoutError e) {
            String message = limitedByCaller
                    ? "Stopped because the script code that triggered it ran out of its own budget"
                    : "Stopped after taking longer than its %s ms budget".formatted(budgetMillis);

            return failure(script, start, message, e);
        } catch (StackOverflowError e) {
            return failure(script, start, "Stack overflow, probably endless recursion", e);
        } catch (Exception | LinkageError e) {
            return failure(script, start, e.toString(), e);
        } finally {
            deadline = previous;
        }
    }

    private ScriptOutcome failure(Script script, long start, String message, Throwable exception) {
        logs.get(script.getKey()).error(message);
        Vio.getLogger().warn("Failed to run script '%s': %s".formatted(script.getKey(), message));

        return new ScriptOutcome(null, message, exception, List.of(), (System.nanoTime() - start) / 1_000_000);
    }

    private void compileAndInstall(Script script, String source) {
        GroovyClassLoader loader = createLoader(script);

        try {
            install(script.getKey(), loader, createRuntimeClass(loader, script, source));
        } catch (RuntimeException e) {
            dispose(loader);
            throw e;
        }
    }

    private void install(ScriptKey key, GroovyClassLoader loader, Class<? extends RuntimeScript> runtimeClass) {
        loaders.put(key, loader);
        runtimeClasses.put(key, runtimeClass);
    }

    private void restore(ScriptKey key, GroovyClassLoader loader, Class<? extends RuntimeScript> runtimeClass) {
        if (loader == null) {
            loaders.remove(key);
            runtimeClasses.remove(key);
        } else {
            install(key, loader, runtimeClass);
        }
    }

    private void disposeLoader(ScriptKey key) {
        GroovyClassLoader loader = loaders.remove(key);
        runtimeClasses.remove(key);

        if (loader != null) {
            dispose(loader);
        }
    }

    private static void dispose(GroovyClassLoader loader) {
        for (Class<?> loaded : loader.getLoadedClasses()) {
            InvokerHelper.removeClass(loaded);
        }

        loader.clearCache();

        try {
            loader.close();
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to close script loader", e);
        }
    }

    private static GroovyClassLoader createLoader(Script script) {
        ClosureExpression condition = GeneralUtils.closureX(GeneralUtils.stmt(GeneralUtils.callX(GeneralUtils.classX(DefaultScriptManager.class), "isBudgetExceeded")));
        condition.setVariableScope(new VariableScope());

        Map<String, Object> interrupt = new HashMap<>();
        interrupt.put("value", condition);
        interrupt.put("thrown", ScriptTimeoutError.class);

        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setScriptBaseClass(script.getRuntimeBaseClass().getName());
        configuration.addCompilationCustomizers(
                new ASTTransformationCustomizer(interrupt, ConditionalInterrupt.class),
                new ASTTransformationCustomizer(CompileStatic.class)
        );

        return new GroovyClassLoader(DefaultScriptManager.class.getClassLoader(), configuration);
    }

    private static Class<? extends RuntimeScript> createRuntimeClass(GroovyClassLoader loader, Script script, String source) {
        Class<?> compiled;

        try {
            compiled = loader.parseClass(new GroovyCodeSource(source, "script_%s.groovy".formatted(script.getKey()), "/groovy/script"));
        } catch (MultipleCompilationErrorsException e) {
            List<String> errors = new ArrayList<>();

            for (Message message : e.getErrorCollector().getErrors()) {
                if (message instanceof ExceptionMessage exceptionMessage) {
                    errors.add(String.valueOf(exceptionMessage.getCause()));
                } else {
                    errors.add(message.toString());
                }
            }

            throw new ScriptException("Couldn't compile the script:\n%s".formatted(String.join("\n", errors)), e);
        } catch (RuntimeException | LinkageError e) {
            throw new ScriptException("Couldn't compile the script: %s".formatted(e), e);
        }

        if (!script.getRuntimeBaseClass().isAssignableFrom(compiled)) {
            throw new ScriptException("The source must be a script with statements to run, not only class declarations");
        }

        return compiled.asSubclass(RuntimeScript.class);
    }

    private RuntimeScript createRuntimeScript(Class<? extends RuntimeScript> runtimeClass, ScriptLog log) {
        RuntimeScript runtimeScript;

        try {
            runtimeScript = runtimeClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            if (e.getCause() instanceof Error cause) {
                throw cause;
            }

            throw new IllegalStateException("Failed to create an instance of %s".formatted(runtimeClass.getName()), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create an instance of %s".formatted(runtimeClass.getName()), e);
        }

        runtimeScript.init(server, log, memoryState, fileState);
        return runtimeScript;
    }

    private void registerCommand(CommandScript commandScript) {
        Class<? extends RuntimeScript> runtimeClass = requireRuntimeClass(commandScript.getKey());

        ScriptOutcome outcome = invoke(commandScript, ScriptLimits.DECLARE_BUDGET_MILLIS, () -> {
            CommandRuntimeScript runtimeScript = (CommandRuntimeScript) createRuntimeScript(runtimeClass, logs.get(commandScript.getKey()));

            runtimeScript.run();
            return runtimeScript.getCommand();
        });

        if (!outcome.isSuccessful()) {
            throw new ScriptException("Couldn't declare its command: %s".formatted(outcome.error()));
        }

        @SuppressWarnings("unchecked")
        LiteralArgumentBuilder<CommandSourceStack> declared = (LiteralArgumentBuilder<CommandSourceStack>) outcome.result();

        if (declared == null) {
            throw new ScriptException("A command script must declare its command with command(Commands.literal('name')...)");
        }

        String name = declared.getLiteral();

        if (!COMMAND_NAME.matcher(name).matches()) {
            throw new ScriptException("Invalid command name '%s': 1 to 32 lowercase letters, digits and underscores, starting with a letter".formatted(name));
        }

        RootCommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();

        if (root.getChild(name) != null) {
            throw new ScriptException("/%s already exists. Choose another command name".formatted(name));
        }

        LiteralCommandNode<CommandSourceStack> node = (LiteralCommandNode<CommandSourceStack>) copyCommandNode(commandScript, declared.build());

        root.addChild(node);
        commandNodes.put(commandScript.getKey(), node);
        sendCommands();
    }

    private void unregisterCommand(CommandScript commandScript) {
        LiteralCommandNode<CommandSourceStack> node = commandNodes.remove(commandScript.getKey());

        if (node == null) {
            return;
        }

        removeCommandNode(server.getCommands().getDispatcher().getRoot(), node);
        sendCommands();
    }

    private void registerListener(ListenerScript listenerScript) {
        ScriptKey key = listenerScript.getKey();
        Class<? extends RuntimeScript> runtimeClass = requireRuntimeClass(key);

        ScriptOutcome outcome = invoke(listenerScript, ScriptLimits.DECLARE_BUDGET_MILLIS, () -> {
            ListenerRuntimeScript runtimeScript = (ListenerRuntimeScript) createRuntimeScript(runtimeClass, logs.get(key));

            runtimeScript.run();
            return runtimeScript.getHandlers();
        });

        if (!outcome.isSuccessful()) {
            throw new ScriptException("Couldn't declare its handlers: %s".formatted(outcome.error()));
        }

        @SuppressWarnings("unchecked")
        Map<Class<? extends Event>, Collection<Consumer<Event>>> handlers = (Map<Class<? extends Event>, Collection<Consumer<Event>>>) outcome.result();

        if (handlers.isEmpty()) {
            throw new ScriptException("A listener script must declare at least one handler with on(EventClass) { e -> ... }");
        }

        for (Class<? extends Event> type : handlers.keySet()) {
            if (Modifier.isAbstract(type.getModifiers())) {
                throw new ScriptException("%s is abstract and never fired itself: listen to one of its concrete subclasses instead".formatted(type.getName()));
            }

            if (IModBusEvent.class.isAssignableFrom(type)) {
                throw new ScriptException("%s is a mod loading event; scripts can only listen to game events".formatted(type.getName()));
            }

            if (type.getName().contains(".client.")) {
                throw new ScriptException("%s is a client event, which never fires for server scripts".formatted(type.getName()));
            }
        }

        List<Consumer<? extends Event>> registered = new ArrayList<>();

        try {
            for (Map.Entry<Class<? extends Event>, Collection<Consumer<Event>>> entry : handlers.entrySet()) {
                for (Consumer<Event> handler : entry.getValue()) {
                    registered.add(registerHandler(listenerScript, entry.getKey(), handler));
                }
            }
        } catch (RuntimeException e) {
            registered.forEach(NeoForge.EVENT_BUS::unregister);
            throw new ScriptException("Couldn't register its handlers: %s".formatted(e.getMessage()), e);
        }

        listenerHandlers.put(key, List.copyOf(registered));
    }

    private void unregisterListener(ListenerScript listenerScript) {
        List<Consumer<? extends Event>> registered = listenerHandlers.remove(listenerScript.getKey());

        if (registered != null) {
            registered.forEach(NeoForge.EVENT_BUS::unregister);
        }
    }

    private <T extends Event> Consumer<T> registerHandler(ListenerScript listenerScript, Class<T> type, Consumer<Event> handler) {
        Consumer<T> wrapper = event -> handleEvent(listenerScript, type, handler, event);

        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, wrapper);
        return wrapper;
    }

    private void handleEvent(ListenerScript listenerScript, Class<? extends Event> type, Consumer<Event> handler, Event event) {
        if (!server.isSameThread()) {
            logs.get(listenerScript.getKey()).warning("Skipped %s, which fired off the server thread; handlers only run for events on the server thread".formatted(type.getName()));
            return;
        }

        invoke(listenerScript, ServerConfig.LISTENER_BUDGET_MILLIS.get(), () -> {
            handler.accept(event);
            return null;
        });
    }

    private CommandNode<CommandSourceStack> copyCommandNode(CommandScript commandScript, CommandNode<CommandSourceStack> node) {
        if (node.getRedirect() != null) {
            throw new ScriptException("Script commands can't use redirect or fork");
        }

        ArgumentBuilder<CommandSourceStack, ?> builder = switch (node) {
            case LiteralCommandNode<CommandSourceStack> literal -> Commands.literal(literal.getLiteral());
            case ArgumentCommandNode<CommandSourceStack, ?> argument -> {
                if (!ArgumentTypeInfos.isClassRecognized(argument.getType().getClass())) {
                    throw new ScriptException("%s isn't a registered argument type: use the ones from Brigadier, Minecraft or NeoForge".formatted(argument.getType().getClass().getName()));
                }

                RequiredArgumentBuilder<CommandSourceStack, ?> required = Commands.argument(argument.getName(), argument.getType());

                if (argument.getCustomSuggestions() != null) {
                    required.suggests(wrapSuggestions(commandScript, argument.getCustomSuggestions()));
                }

                yield required;
            }
            default -> throw new ScriptException("Unsupported command node %s".formatted(node.getName()));
        };

        builder.requires(wrapRequirement(commandScript, node.getRequirement()));

        if (node.getCommand() != null) {
            builder.executes(wrapCommand(commandScript, node.getCommand()));
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            builder.then(copyCommandNode(commandScript, child));
        }

        return builder.build();
    }

    private Predicate<CommandSourceStack> wrapRequirement(CommandScript commandScript, Predicate<CommandSourceStack> requirement) {
        return source -> {
            ScriptOutcome outcome = invoke(commandScript, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> requirement.test(source));
            return outcome.isSuccessful() && Boolean.TRUE.equals(outcome.result());
        };
    }

    private Command<CommandSourceStack> wrapCommand(CommandScript commandScript, Command<CommandSourceStack> command) {
        return context -> {
            ScriptOutcome outcome = invoke(commandScript, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> command.run(context));

            if (outcome.exception() instanceof CommandSyntaxException syntaxError) {
                throw syntaxError;
            }

            if (!outcome.isSuccessful()) {
                context.getSource().sendFailure(Component.literal("Couldn't run /%s. Its script's log has the details".formatted(context.getNodes().getFirst().getNode().getName())));
                return 0;
            }

            return (Integer) outcome.result();
        };
    }

    private SuggestionProvider<CommandSourceStack> wrapSuggestions(CommandScript commandScript, SuggestionProvider<CommandSourceStack> provider) {
        return (context, builder) -> {
            ScriptOutcome outcome = invoke(commandScript, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> provider.getSuggestions(context, builder));

            if (!outcome.isSuccessful()) {
                return Suggestions.empty();
            }

            @SuppressWarnings("unchecked")
            CompletableFuture<Suggestions> suggestions = (CompletableFuture<Suggestions>) outcome.result();

            return suggestions == null ? Suggestions.empty() : suggestions;
        };
    }

    private void sendCommands() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    private static void removeCommandNode(RootCommandNode<CommandSourceStack> root, CommandNode<CommandSourceStack> node) {
        if (root.getChild(node.getName()) != node) {
            return;
        }

        try {
            ((Map<?, ?>) CHILDREN.get(root)).remove(node.getName());
            ((Map<?, ?>) LITERALS.get(root)).remove(node.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to remove command /%s".formatted(node.getName()), e);
        }
    }

    private static Field findCommandNodeField(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Brigadier's CommandNode.%s isn't accessible".formatted(name), e);
        }
    }

    private static final class CapturingScriptLog extends DefaultScriptLog {

        private final ScriptLog target;

        private CapturingScriptLog(ScriptLog target) {
            super(ScriptLimits.LOG_CAPACITY);
            this.target = target;
        }

        @Override
        public void log(String message, ScriptLogLevel level) {
            super.log(message, level);
            target.log(message, level);
        }
    }
}
