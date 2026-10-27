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
import com.plummy.vio.script.dsl.CommandCompiledScript;
import com.plummy.vio.script.dsl.ListenerCompiledScript;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ScriptManager {

    private static final Pattern COMMAND_NAME = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Field CHILDREN = findCommandNodeField("children");
    private static final Field LITERALS = findCommandNodeField("literals");

    private static long deadline = Long.MAX_VALUE;

    private final MinecraftServer server;
    private final ScriptStorage storage;
    private final Map<String, Script> scripts = new TreeMap<>();

    public ScriptManager(MinecraftServer server, ScriptStorage storage) {
        this.server = server;
        this.storage = storage;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public static boolean isBudgetExceeded() {
        return System.nanoTime() > deadline;
    }

    public void load() {
        long start = System.nanoTime();

        try {
            for (String name : storage.getNames()) {
                Script script = storage.get(name);

                if (script == null) {
                    continue;
                }

                if (script.getState() == ScriptState.ENABLED) {
                    try {
                        activate(script);
                    } catch (ScriptException e) {
                        script.setError(e.getMessage());
                    }
                }

                if (script.getError() != null) {
                    Vio.getLogger().warn("Script '%s' is errored: %s".formatted(name, script.getError()));
                }

                scripts.put(name, script);
            }
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to load scripts", e);
        }

        long errored = scripts.values().stream().filter(script -> script.getState() == ScriptState.ERRORED).count();
        Vio.getLogger().info("Loaded %s script(s) in %s ms, %s errored".formatted(scripts.size(), (System.nanoTime() - start) / 1_000_000, errored));
    }

    public void shutdown() {
        for (Script script : scripts.values()) {
            deactivate(script);
            script.dispose();
        }

        scripts.clear();
    }

    public Script get(String name) {
        return scripts.get(name);
    }

    public List<Script> getAll() {
        return List.copyOf(scripts.values());
    }

    public boolean save(Script script, boolean replace) {
        String name = script.getName();

        if (!script.isCompiled()) {
            throw new IllegalStateException("Script '%s' must be compiled before it's saved".formatted(name));
        }

        boolean exists = storage.has(name);

        if (exists && !replace) {
            script.dispose();
            throw new ScriptException("A script named '%s' already exists. Pass replace: true to replace it, or choose another name".formatted(name));
        }

        Script previous = scripts.get(name);

        if (script instanceof Toggleable toggleable && previous instanceof Toggleable previousToggleable) {
            toggleable.setEnabled(previousToggleable.isEnabled(), previousToggleable.getToggledBy(), previousToggleable.getToggledAt());
        }

        if (script.getState() == ScriptState.ENABLED) {
            try {
                activate(script);
            } catch (ScriptException e) {
                script.dispose();
                throw new ScriptException("%s\nNothing was saved, and any previous version is unchanged".formatted(e.getMessage()), e);
            }
        }

        try {
            storage.save(script);
        } catch (RuntimeException e) {
            deactivate(script);
            script.dispose();

            if (previous instanceof CommandScript command && command.getNode() != null) {
                RootCommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();

                if (root.getChild(command.getNode().getName()) == null) {
                    root.addChild(command.getNode());
                    sendCommands();
                }
            }

            throw e;
        }

        if (previous != null) {
            deactivate(previous);
            previous.dispose();
        }

        scripts.put(name, script);
        return exists;
    }

    public boolean setEnabled(Script script, boolean enabled, UUID playerId) {
        if (!(script instanceof Toggleable toggleable)) {
            throw new ScriptException("'%s' is a %s script, which can't be enabled or disabled".formatted(script.getName(), script.getType()));
        }

        if (!script.isCompiled()) {
            throw new ScriptException("'%s' doesn't compile, so it can't be enabled or disabled. Save a fixed version with replace: true".formatted(script.getName()));
        }

        if (toggleable.isEnabled() == enabled && script.getError() == null) {
            return false;
        }

        boolean wasEnabled = toggleable.isEnabled();
        UUID previousToggledBy = toggleable.getToggledBy();
        Instant previousToggledAt = toggleable.getToggledAt();

        toggleable.setEnabled(enabled, playerId, Instant.now());

        if (enabled) {
            deactivate(script);

            try {
                activate(script);
            } catch (ScriptException e) {
                toggleable.setEnabled(wasEnabled, previousToggledBy, previousToggledAt);
                throw new ScriptException("%s\nThe script wasn't enabled".formatted(e.getMessage()), e);
            }
        }

        try {
            storage.save(script);
        } catch (RuntimeException e) {
            toggleable.setEnabled(wasEnabled, previousToggledBy, previousToggledAt);

            if (enabled) {
                deactivate(script);
            }

            throw e;
        }

        if (!enabled) {
            deactivate(script);
        }

        script.setError(null);
        script.resetFailures();

        return true;
    }

    public void delete(String name) {
        if (!storage.has(name)) {
            throw new ScriptException("No script named '%s'".formatted(name));
        }

        storage.delete(name);

        Script script = scripts.remove(name);

        if (script != null) {
            deactivate(script);
            script.dispose();
        }
    }

    public ScriptOutcome run(Script script) {
        if (!(script instanceof Executable executable)) {
            throw new ScriptException("'%s' is a %s script, which can't be run on demand".formatted(script.getName(), script.getType()));
        }

        if (!script.isCompiled()) {
            throw new ScriptException("'%s' doesn't compile, so it can't run. Save a fixed version with replace: true".formatted(script.getName()));
        }

        ScriptOutcome outcome = invoke(script, ServerConfig.RUN_BUDGET_MILLIS.get(), () -> executable.execute(server));

        if (!outcome.isSuccessful()) {
            onFailure(script, outcome.getError(), false);
        }

        return outcome;
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Script script : scripts.values()) {
            if (script instanceof CommandScript command && command.getNode() != null) {
                LiteralCommandNode<CommandSourceStack> node = command.getNode();

                if (dispatcher.getRoot().getChild(node.getName()) == null) {
                    dispatcher.getRoot().addChild(node);
                } else {
                    Vio.getLogger().warn("Command /%s of script '%s' is left out: another command with that name exists now".formatted(node.getName(), script.getName()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for (Script script : getAll()) {
            if (script instanceof PeriodicScript periodic && periodic.getState() == ScriptState.ENABLED && periodic.tick()) {
                ScriptOutcome outcome = invoke(periodic, ServerConfig.PERIODIC_BUDGET_MILLIS.get(), () -> periodic.execute(server));

                if (outcome.isSuccessful()) {
                    periodic.resetFailures();
                } else {
                    onFailure(periodic, outcome.getError(), true);
                }
            }
        }
    }

    private void activate(Script script) {
        switch (script) {
            case PeriodicScript periodic -> periodic.reset();
            case ListenerScript listener -> activateListener(listener);
            case CommandScript command -> activateCommand(command);
            default -> {
            }
        }
    }

    private void deactivate(Script script) {
        if (script instanceof ListenerScript listener) {
            listener.getRegistered().forEach(NeoForge.EVENT_BUS::unregister);
            listener.setRegistered(List.of());
        }

        if (script instanceof CommandScript command && command.getNode() != null) {
            removeCommand(server.getCommands().getDispatcher().getRoot(), command.getNode());
            command.setNode(null);
            sendCommands();
        }
    }

    private void activateCommand(CommandScript script) {
        LiteralCommandNode<CommandSourceStack> node = declareCommand(script);
        RootCommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();
        CommandNode<CommandSourceStack> existing = root.getChild(node.getName());

        if (existing != null) {
            boolean replacing = scripts.get(script.getName()) instanceof CommandScript previous && previous.getNode() == existing;

            if (!replacing) {
                throw new ScriptException("/%s already exists. Choose another command name".formatted(node.getName()));
            }

            removeCommand(root, existing);
        }

        root.addChild(node);
        script.setNode(node);
        sendCommands();
    }

    private void activateListener(ListenerScript script) {
        Map<Class<? extends Event>, Collection<Consumer<Event>>> handlers = declareHandlers(script);
        List<Consumer<?>> registered = new ArrayList<>();

        try {
            for (Map.Entry<Class<? extends Event>, Collection<Consumer<Event>>> entry : handlers.entrySet()) {
                for (Consumer<Event> handler : entry.getValue()) {
                    registered.add(registerHandler(script, entry.getKey(), handler));
                }
            }
        } catch (RuntimeException e) {
            registered.forEach(NeoForge.EVENT_BUS::unregister);
            throw new ScriptException("Couldn't register its handlers: %s".formatted(e.getMessage()), e);
        }

        script.setRegistered(List.copyOf(registered));
    }

    private void sendCommands() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    private static void removeCommand(RootCommandNode<CommandSourceStack> root, CommandNode<CommandSourceStack> node) {
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

    private LiteralCommandNode<CommandSourceStack> declareCommand(CommandScript script) {
        ScriptOutcome outcome = invoke(script, ScriptLimits.DECLARE_BUDGET_MILLIS, () -> {
            CommandCompiledScript instance = (CommandCompiledScript) script.newInstance();

            instance.init(server, script.getLog());
            instance.run();
            return instance.getCommand();
        });

        if (!outcome.isSuccessful()) {
            onFailure(script, outcome.getError(), false);
            throw new ScriptException("Couldn't declare its command: %s".formatted(outcome.getError()));
        }

        @SuppressWarnings("unchecked")
        LiteralArgumentBuilder<CommandSourceStack> declared = (LiteralArgumentBuilder<CommandSourceStack>) outcome.getResult();

        if (declared == null) {
            throw new ScriptException("A command script must declare its command with command(Commands.literal('name')...)");
        }

        String name = declared.getLiteral();

        if (!COMMAND_NAME.matcher(name).matches()) {
            throw new ScriptException("Invalid command name '%s': 1 to 32 lowercase letters, digits and underscores, starting with a letter".formatted(name));
        }

        int permissionLevel = script.getPermissionLevel();
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(name).requires(source -> source.hasPermission(permissionLevel));

        copyInto(script, declared.build(), builder);
        return builder.build();
    }

    private void copyInto(CommandScript script, CommandNode<CommandSourceStack> node, ArgumentBuilder<CommandSourceStack, ?> builder) {
        if (node.getRedirect() != null) {
            throw new ScriptException("Script commands can't use redirect or fork");
        }

        if (node.getCommand() != null) {
            builder.executes(wrapCommand(script, node.getCommand()));
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            builder.then(copy(script, child));
        }
    }

    private CommandNode<CommandSourceStack> copy(CommandScript script, CommandNode<CommandSourceStack> node) {
        ArgumentBuilder<CommandSourceStack, ?> builder = switch (node) {
            case LiteralCommandNode<CommandSourceStack> literal -> Commands.literal(literal.getLiteral());
            case ArgumentCommandNode<CommandSourceStack, ?> argument -> {
                if (!ArgumentTypeInfos.isClassRecognized(argument.getType().getClass())) {
                    throw new ScriptException("%s isn't a registered argument type: use the ones from Brigadier, Minecraft or NeoForge".formatted(argument.getType().getClass().getName()));
                }

                RequiredArgumentBuilder<CommandSourceStack, ?> required = Commands.argument(argument.getName(), argument.getType());

                if (argument.getCustomSuggestions() != null) {
                    required.suggests(wrapSuggestions(script, argument.getCustomSuggestions()));
                }

                yield required;
            }
            default -> throw new ScriptException("Unsupported command node %s".formatted(node.getName()));
        };

        copyInto(script, node, builder);
        return builder.build();
    }

    private Command<CommandSourceStack> wrapCommand(CommandScript script, Command<CommandSourceStack> command) {
        return context -> {
            ScriptOutcome outcome = invoke(script, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> command.run(context));

            if (outcome.getException() instanceof CommandSyntaxException syntaxError) {
                throw syntaxError;
            }

            if (!outcome.isSuccessful()) {
                onFailure(script, outcome.getError(), true);
                context.getSource().sendFailure(Component.literal("Couldn't run /%s. Its script's log has the details.".formatted(context.getNodes().getFirst().getNode().getName())));
                return 0;
            }

            script.resetFailures();
            return (Integer) outcome.getResult();
        };
    }

    private SuggestionProvider<CommandSourceStack> wrapSuggestions(CommandScript script, SuggestionProvider<CommandSourceStack> provider) {
        return (context, builder) -> {
            ScriptOutcome outcome = invoke(script, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> provider.getSuggestions(context, builder));

            if (outcome.getException() instanceof CommandSyntaxException) {
                return Suggestions.empty();
            }

            if (!outcome.isSuccessful()) {
                onFailure(script, outcome.getError(), true);
                return Suggestions.empty();
            }

            script.resetFailures();

            @SuppressWarnings("unchecked")
            CompletableFuture<Suggestions> suggestions = (CompletableFuture<Suggestions>) outcome.getResult();

            return suggestions == null ? Suggestions.empty() : suggestions;
        };
    }

    private Map<Class<? extends Event>, Collection<Consumer<Event>>> declareHandlers(ListenerScript script) {
        ScriptOutcome outcome = invoke(script, ScriptLimits.DECLARE_BUDGET_MILLIS, () -> {
            ListenerCompiledScript instance = (ListenerCompiledScript) script.newInstance();

            instance.init(server, script.getLog());
            instance.run();
            return instance.getHandlers();
        });

        if (!outcome.isSuccessful()) {
            onFailure(script, outcome.getError(), false);
            throw new ScriptException("Couldn't declare its handlers: %s".formatted(outcome.getError()));
        }

        @SuppressWarnings("unchecked")
        Map<Class<? extends Event>, Collection<Consumer<Event>>> handlers = (Map<Class<? extends Event>, Collection<Consumer<Event>>>) outcome.getResult();

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

        return handlers;
    }

    private <T extends Event> Consumer<T> registerHandler(ListenerScript script, Class<T> type, Consumer<Event> handler) {
        Consumer<T> wrapper = event -> handle(script, type, handler, event);

        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, wrapper);
        return wrapper;
    }

    private void handle(ListenerScript script, Class<? extends Event> type, Consumer<Event> handler, Event event) {
        if (!server.isSameThread()) {
            script.getLog().warn("Skipped %s, which fired off the server thread; handlers only run for events on the server thread".formatted(type.getName()));
            return;
        }

        if (script.getState() != ScriptState.ENABLED) {
            return;
        }

        ScriptOutcome outcome = invoke(script, ServerConfig.LISTENER_BUDGET_MILLIS.get(), () -> {
            handler.accept(event);
            return null;
        });

        if (outcome.isSuccessful()) {
            script.resetFailures();
        } else {
            onFailure(script, outcome.getError(), true);
        }
    }

    private static ScriptOutcome invoke(Script script, int budgetMillis, Callable<Object> call) {
        long start = System.nanoTime();
        long previous = deadline;
        boolean limitedByCaller = previous != Long.MAX_VALUE && previous - start < budgetMillis * 1_000_000L;

        deadline = Math.min(previous, start + budgetMillis * 1_000_000L);

        try {
            return new ScriptOutcome(call.call(), null, null, (System.nanoTime() - start) / 1_000_000);
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

    private static ScriptOutcome failure(Script script, long start, String message, Throwable exception) {
        String fileName = ScriptCompiler.getFileName(script.getName());

        for (StackTraceElement element : exception.getStackTrace()) {
            if (fileName.equals(element.getFileName()) && element.getLineNumber() > 0) {
                message = "%s (line %s)".formatted(message, element.getLineNumber());
                break;
            }
        }

        return new ScriptOutcome(null, message, exception, (System.nanoTime() - start) / 1_000_000);
    }

    private void onFailure(Script script, String error, boolean counted) {
        script.getLog().error(error);
        Vio.getLogger().warn("Failed to run script '%s': %s".formatted(script.getName(), error));

        if (!counted) {
            return;
        }

        int failures = script.recordFailure();

        if (failures >= ServerConfig.MAX_FAILURES.get() && script.getState() == ScriptState.ENABLED) {
            script.setError("Disabled after %s failures in a row, the last one: %s".formatted(failures, error));
            deactivate(script);
            script.getLog().error(script.getError());
            Vio.getLogger().warn("Script '%s' disabled after %s failures in a row".formatted(script.getName(), failures));
        }
    }
}
