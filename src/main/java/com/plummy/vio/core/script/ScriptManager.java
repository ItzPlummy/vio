package com.plummy.vio.core.script;

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
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.core.script.dsl.CommandVioScript;
import com.plummy.vio.core.script.dsl.ListenerVioScript;
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

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// Owns every loaded script, by name, and every call into script code. Runs on the server thread only; compiling a
// script to save happens before, on the caller's thread.
//
// An enabled script is active: periodic scripts are scheduled, listener scripts have their handlers on the event
// bus, command scripts their command in the dispatcher. Activating runs the script's declaration and either fully
// succeeds or leaves nothing behind, so it always happens before the file is saved.
public class ScriptManager {

    private static final Pattern COMMAND_NAME = Pattern.compile("[a-z][a-z0-9_]{0,31}");

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

    // Compiles and activates every saved script. One that fails is loaded as ERRORED; nothing here may stop the
    // server from starting.
    public void load() {
        long start = System.nanoTime();

        try {
            for (String name : storage.getNames()) {
                ScriptFile file = storage.get(name);

                if (file != null) {
                    scripts.put(name, load(file));
                }
            }
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to load scripts", e);
        }

        long errored = scripts.values().stream().filter(script -> script.getState() == ScriptState.ERRORED).count();
        Vio.getLogger().info("Loaded {} script(s) in {} ms, {} errored", scripts.size(), (System.nanoTime() - start) / 1_000_000, errored);
    }

    public void shutdown() {
        for (Script script : scripts.values()) {
            deactivate(script);
            script.close();
        }

        scripts.clear();
    }

    public Script get(String name) {
        return scripts.get(name);
    }

    // Sorted by name.
    public List<Script> getAll() {
        return List.copyOf(scripts.values());
    }

    // Saves a script compiled beforehand in place of the previous version, which keeps running unless the new one is
    // saved and active. Returns whether a script was replaced. The compiled script is closed if it isn't used.
    public boolean save(ScriptFile file, CompiledScript compiled, boolean replace) {
        String name = file.getName();
        boolean exists = storage.has(name);

        if (exists && !replace) {
            compiled.close();
            throw new ScriptException("A script named \"%s\" already exists. Pass replace: true to replace it, or choose another name.".formatted(name));
        }

        Script previous = scripts.get(name);
        ScriptFile saved = previous == null ? file : new ScriptFile(name, file.getHeader().withToggleStateOf(previous.getHeader()), file.getSource());
        Script script = Script.create(saved, compiled, null);

        if (script.getState() == ScriptState.ENABLED) {
            try {
                activate(script);
            } catch (ScriptException e) {
                script.close();
                throw new ScriptException(e.getMessage() + "\nNothing was saved, and any previous version is unchanged.", e);
            }
        }

        try {
            storage.save(saved);
        } catch (RuntimeException e) {
            deactivate(script);
            script.close();
            restoreCommand(previous);
            throw e;
        }

        if (previous != null) {
            deactivate(previous);
            previous.close();
        }

        scripts.put(name, script);
        return exists;
    }

    // Also enables a script that was disabled after repeated failures. Returns false when nothing changed.
    public boolean setEnabled(Script script, boolean enabled, UUID playerId) {
        if (!(script instanceof Toggleable)) {
            throw new ScriptException("\"%s\" is a %s script, which can't be enabled or disabled.".formatted(script.getName(), script.getType()));
        }

        if (script.getCompiled() == null) {
            throw new ScriptException("\"%s\" doesn't compile, so it can't be enabled or disabled. Save a fixed version with replace: true.".formatted(script.getName()));
        }

        if (script.getHeader().isEnabled() == enabled && script.getError() == null) {
            return false;
        }

        ScriptFile file = new ScriptFile(script.getName(), script.getHeader().withEnabled(enabled, playerId, Instant.now()), script.getSource());

        if (enabled) {
            // An errored script was deactivated already; this only makes sure nothing is registered twice.
            deactivate(script);

            try {
                activate(script);
            } catch (ScriptException e) {
                throw new ScriptException(e.getMessage() + "\nThe script wasn't enabled.", e);
            }

            try {
                storage.save(file);
            } catch (RuntimeException e) {
                deactivate(script);
                throw e;
            }
        } else {
            storage.save(file);
            deactivate(script);
        }

        script.setFile(file);
        script.setError(null);
        script.resetFailures();

        return true;
    }

    // Also deletes a file that couldn't be loaded.
    public void delete(String name) {
        if (!storage.has(name)) {
            throw new ScriptException("No script named \"%s\".".formatted(name));
        }

        storage.delete(name);

        Script script = scripts.remove(name);

        if (script != null) {
            deactivate(script);
            script.close();
        }
    }

    // Runs a one-shot script, or a periodic one out of schedule. Failures here don't count toward disabling it.
    public RunResult run(Script script) {
        if (!(script instanceof Executable executable)) {
            throw new ScriptException("\"%s\" is a %s script, which can't be run on demand.".formatted(script.getName(), script.getType()));
        }

        if (script.getCompiled() == null) {
            throw new ScriptException("\"%s\" doesn't compile, so it can't run. Save a fixed version with replace: true.".formatted(script.getName()));
        }

        long mark = script.getLog().mark();
        Outcome outcome = invoke(script, ServerConfig.RUN_BUDGET_MILLIS.get(), () -> executable.execute(server));
        List<ScriptLog.Entry> output = script.getLog().getEntriesSince(mark);

        if (!outcome.success()) {
            onFailure(script, outcome.error(), false);
        }

        return new RunResult(outcome.success(), outcome.result(), outcome.error(), outcome.durationMillis(), output);
    }

    // /reload replaces the dispatcher, so active script commands are added to the new one again.
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Script script : scripts.values()) {
            if (script instanceof CommandScript command && command.getRegistration() != null) {
                LiteralCommandNode<CommandSourceStack> node = command.getRegistration().node();

                if (dispatcher.getRoot().getChild(node.getName()) == null) {
                    dispatcher.getRoot().addChild(node);
                } else {
                    Vio.getLogger().warn("Command /{} of script {} is left out: another command with that name exists now", node.getName(), script.getName());
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // A copy, since script code may reach this manager too.
        for (Script script : getAll()) {
            if (script instanceof PeriodicScript periodic && periodic.getState() == ScriptState.ENABLED && periodic.tick()) {
                Outcome outcome = invoke(periodic, ServerConfig.PERIODIC_BUDGET_MILLIS.get(), () -> periodic.execute(server));

                if (outcome.success()) {
                    periodic.resetFailures();
                } else {
                    onFailure(periodic, outcome.error(), true);
                }
            }
        }
    }

    private Script load(ScriptFile file) {
        Script script;

        try {
            script = Script.create(file, ScriptCompiler.compile(file.getName(), file.getHeader().getType(), file.getSource()), null);
        } catch (ScriptException e) {
            Vio.getLogger().warn("Script {} is errored: {}", file.getName(), e.getMessage());
            return Script.create(file, null, e.getMessage());
        }

        if (script.getState() == ScriptState.ENABLED) {
            try {
                activate(script);
            } catch (ScriptException e) {
                Vio.getLogger().warn("Script {} is errored: {}", file.getName(), e.getMessage());
                script.setError(e.getMessage());
            }
        }

        return script;
    }

    // Throws a ScriptException, leaving nothing registered, when the script's declaration fails.
    private void activate(Script script) {
        switch (script) {
            case PeriodicScript periodic -> periodic.resetSchedule();
            case ListenerScript listener -> listener.setRegistered(register(listener, declare(listener)));
            case CommandScript command -> activateCommand(command);
            default -> {
            }
        }
    }

    // Safe to call on inactive scripts.
    private void deactivate(Script script) {
        if (script instanceof ListenerScript listener) {
            listener.getRegistered().forEach(NeoForge.EVENT_BUS::unregister);
            listener.setRegistered(List.of());
        }

        if (script instanceof CommandScript command && command.getRegistration() != null) {
            CommandScript.Registration registration = command.getRegistration();

            registration.active().set(false);
            BrigadierNodes.remove(server.getCommands().getDispatcher().getRoot(), registration.node());
            command.setRegistration(null);
            sendCommands();
        }
    }

    private void activateCommand(CommandScript script) {
        AtomicBoolean active = new AtomicBoolean(true);
        LiteralCommandNode<CommandSourceStack> node = declare(script, active);
        RootCommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();
        CommandNode<CommandSourceStack> existing = root.getChild(node.getName());

        if (existing != null) {
            // Only the version of this script being replaced may hold the name.
            boolean replacing = scripts.get(script.getName()) instanceof CommandScript previous && previous.getRegistration() != null && previous.getRegistration().node() == existing;

            if (!replacing) {
                throw new ScriptException("/%s already exists. Choose another command name.".formatted(node.getName()));
            }

            // Adding the new node over the old one would merge their children instead.
            if (!BrigadierNodes.remove(root, existing)) {
                throw new ScriptException("/%s can't be replaced until the server restarts.".formatted(node.getName()));
            }
        }

        root.addChild(node);
        script.setRegistration(new CommandScript.Registration(node, active));
        sendCommands();
    }

    // When saving a replacement fails after its command took over the previous version's.
    private void restoreCommand(Script previous) {
        if (previous instanceof CommandScript command && command.getRegistration() != null) {
            RootCommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();
            LiteralCommandNode<CommandSourceStack> node = command.getRegistration().node();

            if (root.getChild(node.getName()) == null) {
                root.addChild(node);
                sendCommands();
            }
        }
    }

    // Players' games only offer the commands they were last sent.
    private void sendCommands() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    // Runs the body of a command script, which only declares its command, and builds the node Vio registers from it.
    private LiteralCommandNode<CommandSourceStack> declare(CommandScript script, AtomicBoolean active) {
        Outcome outcome = invoke(script, 100, () -> {
            CommandVioScript instance = (CommandVioScript) script.getCompiled().newInstance();

            instance.init(server, script.getLog());
            instance.run();
            return instance.getCommand();
        });

        if (!outcome.success()) {
            onFailure(script, outcome.error(), false);
            throw new ScriptException("Declaring its command failed: " + outcome.error());
        }

        @SuppressWarnings("unchecked")
        LiteralArgumentBuilder<CommandSourceStack> declared = (LiteralArgumentBuilder<CommandSourceStack>) outcome.result();

        if (declared == null) {
            throw new ScriptException("A command script must declare its command with command(Commands.literal('name')...).");
        }

        String name = declared.getLiteral();

        if (!COMMAND_NAME.matcher(name).matches()) {
            throw new ScriptException("Invalid command name \"%s\": 1 to 32 lowercase letters, digits and underscores, starting with a letter.".formatted(name));
        }

        int permissionLevel = script.getPermissionLevel();
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(name).requires(source -> active.get() && source.hasPermission(permissionLevel));

        copyInto(script, declared.build(), builder);
        return builder.build();
    }

    // Copies a declared node's command and children, wrapping every call into script code. Requirements aren't
    // copied: the script's permission level alone decides who can use the command.
    private void copyInto(CommandScript script, CommandNode<CommandSourceStack> node, ArgumentBuilder<CommandSourceStack, ?> builder) {
        // A redirect would lead to the declared nodes, which aren't wrapped.
        if (node.getRedirect() != null) {
            throw new ScriptException("Script commands can't use redirect or fork.");
        }

        if (node.getCommand() != null) {
            builder.executes(wrap(script, node.getCommand()));
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            builder.then(copy(script, child));
        }
    }

    private CommandNode<CommandSourceStack> copy(CommandScript script, CommandNode<CommandSourceStack> node) {
        ArgumentBuilder<CommandSourceStack, ?> builder = switch (node) {
            case LiteralCommandNode<CommandSourceStack> literal -> Commands.literal(literal.getLiteral());
            case ArgumentCommandNode<CommandSourceStack, ?> argument -> copyArgument(script, argument);
            default -> throw new ScriptException("Unsupported command node " + node.getName());
        };

        copyInto(script, node, builder);
        return builder.build();
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> copyArgument(CommandScript script, ArgumentCommandNode<CommandSourceStack, ?> node) {
        // Players' games couldn't read a command tree with an argument type they don't know, and would disconnect.
        if (!ArgumentTypeInfos.isClassRecognized(node.getType().getClass())) {
            throw new ScriptException("%s isn't a registered argument type: use the ones from Brigadier, Minecraft or NeoForge.".formatted(node.getType().getClass().getName()));
        }

        RequiredArgumentBuilder<CommandSourceStack, ?> builder = Commands.argument(node.getName(), node.getType());

        if (node.getCustomSuggestions() != null) {
            builder.suggests(wrap(script, node.getCustomSuggestions()));
        }

        return builder;
    }

    // Syntax errors are meant for the player who typed the command, so they pass through and don't count as failures.
    private Command<CommandSourceStack> wrap(CommandScript script, Command<CommandSourceStack> command) {
        return context -> {
            CommandSyntaxException[] syntaxError = new CommandSyntaxException[1];
            Outcome outcome = invoke(script, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> {
                try {
                    return command.run(context);
                } catch (CommandSyntaxException e) {
                    syntaxError[0] = e;
                    return 0;
                }
            });

            if (syntaxError[0] != null) {
                throw syntaxError[0];
            }

            if (!outcome.success()) {
                onFailure(script, outcome.error(), true);
                context.getSource().sendFailure(Component.literal("/%s failed. Its script's log has the details.".formatted(context.getNodes().getFirst().getNode().getName())));
                return 0;
            }

            script.resetFailures();
            return (Integer) outcome.result();
        };
    }

    private SuggestionProvider<CommandSourceStack> wrap(CommandScript script, SuggestionProvider<CommandSourceStack> provider) {
        return (context, builder) -> {
            Outcome outcome = invoke(script, ServerConfig.COMMAND_BUDGET_MILLIS.get(), () -> {
                try {
                    return provider.getSuggestions(context, builder);
                } catch (CommandSyntaxException e) {
                    return null;
                }
            });

            if (!outcome.success()) {
                onFailure(script, outcome.error(), true);
                return Suggestions.empty();
            }

            script.resetFailures();

            @SuppressWarnings("unchecked")
            CompletableFuture<Suggestions> suggestions = (CompletableFuture<Suggestions>) outcome.result();

            return suggestions == null ? Suggestions.empty() : suggestions;
        };
    }

    // Runs the body of a listener script, which only collects its on(...) handlers, and checks them.
    private List<ListenerVioScript.Handler<?>> declare(ListenerScript script) {
        Outcome outcome = invoke(script, 100, () -> {
            ListenerVioScript instance = (ListenerVioScript) script.getCompiled().newInstance();

            instance.init(server, script.getLog());
            instance.run();
            return instance.getHandlers();
        });

        if (!outcome.success()) {
            onFailure(script, outcome.error(), false);
            throw new ScriptException("Declaring its handlers failed: " + outcome.error());
        }

        @SuppressWarnings("unchecked")
        List<ListenerVioScript.Handler<?>> handlers = (List<ListenerVioScript.Handler<?>>) outcome.result();

        if (handlers.isEmpty()) {
            throw new ScriptException("A listener script must declare at least one handler with on(EventClass) { e -> ... }.");
        }

        for (ListenerVioScript.Handler<?> handler : handlers) {
            Class<?> type = handler.type();

            if (type == null || handler.consumer() == null) {
                throw new ScriptException("on(...) needs an event class and a handler.");
            }

            // The bus refuses these too, but with less helpful messages.
            if (Modifier.isAbstract(type.getModifiers())) {
                throw new ScriptException("%s is abstract and never fired itself: listen to one of its concrete subclasses instead.".formatted(type.getName()));
            }

            if (IModBusEvent.class.isAssignableFrom(type)) {
                throw new ScriptException("%s is a mod loading event; scripts can only listen to game events.".formatted(type.getName()));
            }

            if (type.getName().contains(".client.")) {
                throw new ScriptException("%s is a client event, which never fires for server scripts.".formatted(type.getName()));
            }
        }

        return handlers;
    }

    // All or nothing: if the bus refuses one handler, the ones registered before it are removed again.
    private List<Consumer<?>> register(ListenerScript script, List<ListenerVioScript.Handler<?>> handlers) {
        List<Consumer<?>> registered = new ArrayList<>();

        try {
            for (ListenerVioScript.Handler<?> handler : handlers) {
                registered.add(register(script, handler));
            }
        } catch (RuntimeException e) {
            registered.forEach(NeoForge.EVENT_BUS::unregister);
            throw new ScriptException("Registering its handlers failed: " + e.getMessage(), e);
        }

        return List.copyOf(registered);
    }

    // Canceled events don't reach scripts.
    private <T extends Event> Consumer<T> register(ListenerScript script, ListenerVioScript.Handler<T> handler) {
        Consumer<T> wrapper = event -> handle(script, handler, event);

        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, handler.type(), wrapper);
        return wrapper;
    }

    private <T extends Event> void handle(ListenerScript script, ListenerVioScript.Handler<T> handler, T event) {
        // Script state is only safe to touch on the server thread.
        if (!server.isSameThread()) {
            if (script.reportOffThread()) {
                String message = "Skipped %s, which fired off the server thread; handlers only run for events on the server thread".formatted(handler.type().getName());

                script.getLog().warn(message);
                Vio.getLogger().warn("Script {}: {}", script.getName(), message);
            }

            return;
        }

        // Disabled by an earlier handler of the same event.
        if (script.getState() != ScriptState.ENABLED) {
            return;
        }

        Outcome outcome = invoke(script, ServerConfig.LISTENER_BUDGET_MILLIS.get(), () -> {
            handler.consumer().accept(event);
            return null;
        });

        if (outcome.success()) {
            script.resetFailures();
        } else {
            onFailure(script, outcome.error(), true);
        }
    }

    // Every call into script code goes through here. VirtualMachineErrors other than a stack overflow, such as running
    // out of memory, aren't the script's to handle and pass through.
    private static Outcome invoke(Script script, int budgetMillis, Supplier<Object> call) {
        long start = System.nanoTime();
        long previous = ScriptBudget.begin(budgetMillis);
        // Called from another script's code, e.g. a command it ran, so that call's deadline comes first.
        boolean limitedByCaller = previous != Long.MAX_VALUE && previous - start < budgetMillis * 1_000_000L;

        try {
            return new Outcome(true, call.get(), null, (System.nanoTime() - start) / 1_000_000);
        } catch (ScriptTimeoutError e) {
            String message = limitedByCaller
                    ? "Stopped because the script code that triggered it ran out of its own budget"
                    : "Stopped after taking longer than its %d ms budget".formatted(budgetMillis);

            return failure(script, start, message, e);
        } catch (StackOverflowError e) {
            return failure(script, start, "Stack overflow, probably endless recursion", e);
        } catch (Exception | LinkageError e) {
            return failure(script, start, e.toString(), e);
        } finally {
            ScriptBudget.end(previous);
        }
    }

    // Points at the script's own line where it can.
    private static Outcome failure(Script script, long start, String message, Throwable throwable) {
        String fileName = ScriptCompiler.getFileName(script.getName());

        for (StackTraceElement element : throwable.getStackTrace()) {
            if (fileName.equals(element.getFileName()) && element.getLineNumber() > 0) {
                message += " (line %d)".formatted(element.getLineNumber());
                break;
            }
        }

        return new Outcome(false, null, message, (System.nanoTime() - start) / 1_000_000);
    }

    private void onFailure(Script script, String error, boolean counted) {
        script.getLog().error(error);
        Vio.getLogger().warn("Script {} failed: {}", script.getName(), error);

        if (!counted) {
            return;
        }

        int failures = script.recordFailure();

        if (failures >= ServerConfig.MAX_FAILURES.get() && script.getState() == ScriptState.ENABLED) {
            script.setError("Disabled after %d failures in a row, the last one: %s".formatted(failures, error));
            deactivate(script);
            script.getLog().error(script.getError());
            Vio.getLogger().warn("Script {} disabled after {} failures in a row", script.getName(), failures);
        }
    }

    private record Outcome(boolean success, Object result, String error, long durationMillis) {
    }

    // Output is what the script logged during the run.
    public record RunResult(boolean success, Object result, String error, long durationMillis, List<ScriptLog.Entry> output) {
    }
}
