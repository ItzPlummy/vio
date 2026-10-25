package com.plummy.vio.core.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.script.*;
import com.plummy.vio.core.script.ScriptStorage;
import com.plummy.vio.core.tool.BasicTool;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.requester.PlayerToolRequester;
import com.plummy.vio.core.tool.requester.ToolRequester;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.concurrent.ExecutionException;

// save_<type>_script: one per script type, since each has its own arguments. A basic tool, so compiling doesn't hold
// up the server; only saving the file and swapping the script in happen on the server thread.
public class SaveScriptTool implements BasicTool {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_SOURCE_LENGTH = 65536;

    private final ScriptType type;

    public SaveScriptTool(ScriptType type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return "save_%s_script".formatted(type.getValue());
    }

    @Override
    public String getDescription() {
        return ("Create or replace a %s script. %s "
                + "Scripts are Groovy 4, compiled with @CompileStatic on NeoForge 1.21.1, which uses Mojang's official names (ServerPlayer, ServerLevel, Component, BlockPos, ...), so wrong names or types fail right here with line numbers: fix them and save again. "
                + "A script is plain top-level statements, like the example: no class declarations, no main method. It's not Bukkit, Spigot, Paper, Fabric or old Forge: their classes don't exist. "
                + "Nothing is imported automatically: write every import. Besides the server's own API, scripts get server (the MinecraftServer) and log (log.info/warn/error(String); println goes to log.info too), read with read_script_log. "
                + "Scripts run on the server thread with full server authority: keep every run short, never sleep, block or load chunks, and never do what your tools couldn't, such as level 3-4 operator actions. "
                + "A new script is enabled right away; a replaced one stays enabled or disabled and its log is emptied. If compiling fails, nothing is saved and any previous version keeps running.\n"
                + "Example:\n%s").formatted(type, getTypeDescription(), getExample());
    }

    // Each one compiles as is; keep them that way when changing them.
    private String getExample() {
        return switch (type) {
            case ONE_SHOT -> """
                    import net.minecraft.server.level.ServerPlayer

                    List<String> names = []
                    for (ServerPlayer player : server.playerList.players) {
                        names << player.name.string
                    }
                    return names.join(', ')""";
            case PERIODIC -> """
                    import net.minecraft.server.level.ServerPlayer
                    import net.minecraft.world.effect.MobEffectInstance
                    import net.minecraft.world.effect.MobEffects

                    for (ServerPlayer player : server.playerList.players) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0))
                    }""";
            case LISTENER -> """
                    import net.minecraft.core.particles.ParticleTypes
                    import net.minecraft.network.chat.Component
                    import net.minecraft.server.level.ServerLevel
                    import net.minecraft.server.level.ServerPlayer
                    import net.minecraft.world.level.block.Blocks
                    import net.neoforged.neoforge.event.entity.player.PlayerEvent
                    import net.neoforged.neoforge.event.level.BlockEvent

                    on(PlayerEvent.PlayerLoggedInEvent) { e ->
                        e.entity.sendSystemMessage(Component.literal('Welcome back!'))
                    }

                    on(BlockEvent.EntityPlaceEvent) { e ->
                        if (e.entity instanceof ServerPlayer && e.placedBlock.is(Blocks.SNOW_BLOCK)) {
                            ServerLevel level = (ServerLevel) e.level
                            level.sendParticles(ParticleTypes.FIREWORK, e.pos.x + 0.5d, e.pos.y + 1.0d, e.pos.z + 0.5d, 30, 0.3d, 0.3d, 0.3d, 0.1d)
                        }
                    }""";
            case COMMAND -> """
                    import net.minecraft.commands.Commands
                    import net.minecraft.commands.arguments.EntityArgument
                    import net.minecraft.network.chat.Component
                    import net.minecraft.server.level.ServerPlayer

                    command(Commands.literal('heal')
                            .then(Commands.argument('target', EntityArgument.player())
                                    .executes { ctx ->
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, 'target')
                                        target.setHealth(target.maxHealth)
                                        ctx.source.sendSuccess({ Component.literal('Healed ' + target.name.string) }, false)
                                        return 1
                                    }))""";
        };
    }

    private String getTypeDescription() {
        return switch (type) {
            case ONE_SHOT -> "The script body is the action: run it with run_script as often as needed, within %s ms per run; its return value comes back as the result."
                    .formatted(ServerConfig.RUN_BUDGET_MILLIS.get());
            case PERIODIC -> "The script body runs every interval_ticks ticks (20 ticks = 1 second) while the script is enabled, the first time one interval after it's enabled or the server starts, within %s ms per run; after %s failures in a row it's disabled. Every run starts fresh, so script variables don't keep values between runs. run_script runs it once right away for testing."
                    .formatted(ServerConfig.PERIODIC_BUDGET_MILLIS.get(), ServerConfig.MAX_FAILURES.get());
            case LISTENER -> ("The script body only declares event handlers, and runs whenever the script is enabled (also when it's saved enabled and when the server starts): on(EventClass) { e -> ... }, as many as needed; e is typed as the event. "
                    + "Only concrete NeoForge game events work: not abstract ones like PlayerEvent itself, mod loading events or client events. Handlers don't see events another mod already canceled. "
                    + "Common events, in net.neoforged.neoforge.event: entity.player.PlayerEvent.PlayerLoggedInEvent, PlayerRespawnEvent and ItemCraftedEvent; entity.player.PlayerInteractEvent.RightClickBlock; level.BlockEvent.BreakEvent and EntityPlaceEvent; entity.living.LivingDeathEvent and LivingIncomingDamageEvent; entity.EntityJoinLevelEvent; ServerChatEvent. Import the outer class for nested ones, as in the example. "
                    + "Each handler call runs on the server thread within %s ms, declaring within %s ms; after %s failures in a row the script is disabled. Prefer rare, specific events: frequent ones like every entity's tick cost a call each time.")
                    .formatted(ServerConfig.LISTENER_BUDGET_MILLIS.get(), 100, ServerConfig.MAX_FAILURES.get());
            case COMMAND -> ("The script body only declares the command, and runs whenever the script is enabled (also when it's saved enabled and when the server starts): command(Commands.literal('name')...) exactly once, with Brigadier builder calls chained inside the parentheses as in the example; .executes takes a closure { ctx -> ...; return 1 }. "
                    + "Import net.minecraft.commands.Commands and the argument types (com.mojang.brigadier.arguments.*, net.minecraft.commands.arguments.*); item and block arguments take CommandBuildContext.simple(server.registryAccess(), server.worldData.enabledFeatures()). "
                    + "The name is 1 to 32 lowercase letters, digits and underscores and can't be an existing command. permission_level decides who can use it, so .requires is ignored; redirect, fork and custom argument types aren't supported. "
                    + "Executes and suggestions run on the server thread within %s ms per call, declaring within %s ms; after %s failures in a row the script is disabled. For errors meant for the player, throw a CommandSyntaxException or call ctx.source.sendFailure(...): those aren't failures. "
                    + "The command runs with full server authority whoever types it, so choose permission_level deliberately.")
                    .formatted(ServerConfig.COMMAND_BUDGET_MILLIS.get(), 100, ServerConfig.MAX_FAILURES.get());
        };
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, "Script name, also its file name: %s.".formatted(ScriptStorage.NAME_RULE)));
        schema.put("description", new ArgumentDefinition(ArgumentType.STRING, "What the script does, one line of up to %s characters.".formatted(MAX_DESCRIPTION_LENGTH)));

        if (type == ScriptType.PERIODIC) {
            schema.put("interval_ticks", new ArgumentDefinition(ArgumentType.INTEGER, "Ticks between runs, %s to %s; 20 ticks = 1 second.".formatted(ScriptHeader.MIN_INTERVAL_TICKS, ScriptHeader.MAX_INTERVAL_TICKS)));
        }

        if (type == ScriptType.COMMAND) {
            schema.put("permission_level", new ArgumentDefinition(ArgumentType.INTEGER, "Who can use the command, 0 to 4: 0 every player, 1 moderators, 2 game masters (like command blocks), 3 admins, 4 owners."));
        }

        schema.put("source", new ArgumentDefinition(ArgumentType.STRING, "Groovy source, imports first. No header: Vio writes it."));
        schema.put("replace", new ArgumentDefinition(ArgumentType.BOOLEAN, "true to replace an existing script with this name; false fails if the name is taken."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public boolean isAvailable() {
        return ScriptTools.isAvailable();
    }

    @Override
    public ExecutionResult execute(ToolRequester requester, Arguments arguments) {
        // The header records who created the script.
        if (!(requester instanceof PlayerToolRequester player)) {
            return ExecutionResult.failure("Only players can save scripts");
        }

        ScriptManager manager = ScriptTools.getManager();
        String name = ScriptTools.getName(arguments);
        String description = arguments.getString("description").replaceAll("\\R", " ").strip();
        Integer intervalTicks = type == ScriptType.PERIODIC ? arguments.getInt("interval_ticks", ScriptHeader.MIN_INTERVAL_TICKS, ScriptHeader.MAX_INTERVAL_TICKS) : null;
        Integer permissionLevel = type == ScriptType.COMMAND ? arguments.getInt("permission_level", 0, ScriptHeader.MAX_PERMISSION_LEVEL) : null;
        String source = arguments.getString("source");
        boolean replace = arguments.getBoolean("replace");

        if (description.isEmpty() || description.length() > MAX_DESCRIPTION_LENGTH) {
            return ExecutionResult.invalidArguments("description must be 1 to %s characters, got %s".formatted(MAX_DESCRIPTION_LENGTH, description.length()));
        }

        // It would end the header comment early.
        if (description.contains("*/")) {
            return ExecutionResult.invalidArguments("description can't contain */");
        }

        if (source.isBlank() || source.length() > MAX_SOURCE_LENGTH) {
            return ExecutionResult.invalidArguments("source must be 1 to %s characters, got %s".formatted(MAX_SOURCE_LENGTH, source.length()));
        }

        CompiledScript compiled;

        try {
            compiled = ScriptCompiler.compile(name, type, source);
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments("%s\nNothing was saved, and any previous version is unchanged".formatted(e.getMessage()));
        }

        ScriptHeader header = new ScriptHeader(type, description, intervalTicks, permissionLevel, true, player.getPlayerId(), Instant.now(), null, null);
        ScriptFile file = new ScriptFile(name, header, source);
        MinecraftServer server = manager.getServer();

        if (!server.isRunning()) {
            compiled.close();
            return ExecutionResult.failure("Server isn't running");
        }

        try {
            return server.submit(() -> save(manager, file, compiled, replace)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Execution interrupted");
        } catch (ExecutionException e) {
            Vio.getLogger().error("Failed to save script %s".formatted(name), e.getCause());
            return ExecutionResult.failure("Couldn't save the script: %s".formatted(e.getCause().getMessage()));
        }
    }

    // On the server thread.
    private static ExecutionResult save(ScriptManager manager, ScriptFile file, CompiledScript compiled, boolean replace) {
        boolean replaced;

        try {
            replaced = manager.save(file, compiled, replace);
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        Script script = manager.get(file.getName());
        JsonObject json = new JsonObject();

        json.addProperty("name", script.getName());
        json.addProperty("type", script.getType().getValue());
        json.addProperty("state", script.getState().getValue());
        json.addProperty("replaced", replaced);

        return ExecutionResult.success(json);
    }
}
