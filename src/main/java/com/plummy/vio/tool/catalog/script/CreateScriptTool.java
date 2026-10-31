package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.ScriptLimits;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.script.*;
import com.plummy.vio.tool.BasicTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.requester.PlayerToolRequester;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class CreateScriptTool implements BasicTool {

    private final ScriptType type;

    public CreateScriptTool(ScriptType type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return "create_%s_script".formatted(type.getValue());
    }

    @Override
    public String getDescription() {
        return ("Create a %s script. %s "
                + "Scripts are Groovy 4, compiled with @CompileStatic on NeoForge 1.21.1, which uses Mojang's official names (ServerPlayer, ServerLevel, Component, BlockPos, ...), so wrong names or types fail right here: fix them and create it again. "
                + "A script is plain top-level statements, like the example: no class declarations, no main method. It's not Bukkit, Spigot, Paper, Fabric or old Forge: their classes don't exist. "
                + "Nothing is imported automatically: write every import. Besides the server's own API, scripts get server (the MinecraftServer), and println writes to the script's log, read with read_script_log. "
                + "Scripts run on the server thread with full server authority: keep every run short, never sleep, block or load chunks, and never do what your tools couldn't, such as level 3-4 operator actions. "
                + "A new script is enabled right away. If compiling fails, nothing is saved. The name must be free: to change only an existing script's source use update_script_source, and to change anything else, delete_script it and create it again.\n"
                + "Example:\n%s").formatted(type, getTypeDescription(), getExample());
    }

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
                            .requires { source -> source.hasPermission(2) }
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
            case PERIODIC -> "The script body runs every interval_ticks ticks (20 ticks = 1 second) while the script is enabled, the first time one interval after it's enabled or the server starts, within %s ms per run. Every run starts fresh, so script variables don't keep values between runs. run_script runs it once right away for testing."
                    .formatted(ServerConfig.PERIODIC_BUDGET_MILLIS.get());
            case LISTENER -> ("The script body only declares event handlers, and runs whenever the script is enabled (also when it's saved enabled and when the server starts): on(EventClass) { e -> ... }, as many as needed; e is typed as the event. "
                    + "Only concrete NeoForge game events work: not abstract ones like PlayerEvent itself, mod loading events or client events. Handlers don't see events another mod already canceled. "
                    + "Common events, in net.neoforged.neoforge.event: entity.player.PlayerEvent.PlayerLoggedInEvent, PlayerRespawnEvent and ItemCraftedEvent; entity.player.PlayerInteractEvent.RightClickBlock; level.BlockEvent.BreakEvent and EntityPlaceEvent; entity.living.LivingDeathEvent and LivingIncomingDamageEvent; entity.EntityJoinLevelEvent; ServerChatEvent. Import the outer class for nested ones, as in the example. "
                    + "Each handler call runs on the server thread within %s ms, declaring within %s ms. A failing handler stays registered and logs every failure, so fix or disable it. Prefer rare, specific events: frequent ones like every entity's tick cost a call each time.")
                    .formatted(ServerConfig.LISTENER_BUDGET_MILLIS.get(), ScriptLimits.DECLARE_BUDGET_MILLIS);
            case COMMAND -> ("The script body only declares the command, and runs whenever the script is enabled (also when it's saved enabled and when the server starts): command(Commands.literal('name')...) exactly once, with Brigadier builder calls chained inside the parentheses as in the example; .executes takes a closure { ctx -> ...; return 1 }. "
                    + "Import net.minecraft.commands.Commands and the argument types (com.mojang.brigadier.arguments.*, net.minecraft.commands.arguments.*); item and block arguments take CommandBuildContext.simple(server.registryAccess(), server.worldData.enabledFeatures()). "
                    + "The name is 1 to 32 lowercase letters, digits and underscores and can't be an existing command. Redirect, fork and custom argument types aren't supported. "
                    + "Executes, suggestions and requires run on the server thread within %s ms per call, declaring within %s ms. For errors meant for the player, throw a CommandSyntaxException or call ctx.source.sendFailure(...). "
                    + "The command runs with full server authority whoever types it, and without .requires every player can use it: add .requires { source -> source.hasPermission(level) } deliberately, 0 every player to 4 owners.")
                    .formatted(ServerConfig.COMMAND_BUDGET_MILLIS.get(), ScriptLimits.DECLARE_BUDGET_MILLIS);
        };
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, "Script name, also its file name: %s.".formatted(ScriptKey.RULE)));
        schema.put("description", new ArgumentDefinition(ArgumentType.STRING, "What the script does, one line of up to %s characters.".formatted(ScriptLimits.MAX_DESCRIPTION_LENGTH)));

        if (type == ScriptType.PERIODIC) {
            schema.put("interval_ticks", new ArgumentDefinition(ArgumentType.INTEGER, "Ticks between runs, %s to %s; 20 ticks = 1 second.".formatted(ScriptLimits.MIN_INTERVAL_TICKS, ScriptLimits.MAX_INTERVAL_TICKS)));
        }

        schema.put("source", new ArgumentDefinition(ArgumentType.STRING, "Groovy source, imports first."));

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
        if (!(requester instanceof PlayerToolRequester)) {
            return ExecutionResult.failure("Only players can create scripts");
        }

        ScriptManager manager = ScriptTools.getManager();
        ScriptKey key = ScriptTools.getKey(arguments);
        String description = arguments.getString("description").replaceAll("\\R", " ").strip();
        Integer intervalTicks = type == ScriptType.PERIODIC ? arguments.getInt("interval_ticks", ScriptLimits.MIN_INTERVAL_TICKS, ScriptLimits.MAX_INTERVAL_TICKS) : null;
        String source = arguments.getString("source");

        if (description.isEmpty() || description.length() > ScriptLimits.MAX_DESCRIPTION_LENGTH) {
            return ExecutionResult.invalidArguments("description must be 1 to %s characters, got %s".formatted(ScriptLimits.MAX_DESCRIPTION_LENGTH, description.length()));
        }

        if (source.isBlank() || source.length() > ScriptLimits.MAX_SOURCE_LENGTH) {
            return ExecutionResult.invalidArguments("source must be 1 to %s characters, got %s".formatted(ScriptLimits.MAX_SOURCE_LENGTH, source.length()));
        }

        if (manager.getScript(key) != null) {
            return ExecutionResult.invalidArguments("A script named '%s' already exists. Use update_script_source to change its source, delete_script it to change anything else, or choose another name".formatted(key));
        }

        try {
            manager.createScript(createScript(key, description, intervalTicks), new ScriptSource(key, source));
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments("%s\nNothing was saved".formatted(e.getMessage()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("name", key.getKey());
        json.addProperty("type", type.getValue());
        json.addProperty("state", manager.getScriptState(key).getValue());

        return ExecutionResult.success(json);
    }

    private Script createScript(ScriptKey key, String description, Integer intervalTicks) {
        return switch (type) {
            case ONE_SHOT -> new OneShotScript(key, description);
            case PERIODIC -> new PeriodicScript(key, description, true, intervalTicks);
            case LISTENER -> new ListenerScript(key, description, true);
            case COMMAND -> new CommandScript(key, description, true);
        };
    }
}
