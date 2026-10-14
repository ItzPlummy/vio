package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class TeleportPlayerTool implements Tool {

    @Override
    public String getName() {
        return "teleport_player";
    }

    @Override
    public String getDescription() {
        return "Teleport the player to a specified location.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> arguments = new LinkedHashMap<>();

        arguments.put(
                "name",
                new ArgumentDefinition(
                        ArgumentType.STRING,
                        "Nickname of the player who gets teleported."
                )
        );
        arguments.put(
                "x",
                new ArgumentDefinition(
                        ArgumentType.FLOAT,
                        "X coordinate of the location to teleport to."
                )
        );
        arguments.put(
                "y",
                new ArgumentDefinition(
                        ArgumentType.FLOAT,
                        "Y coordinate of the location to teleport to."
                )
        );
        arguments.put(
                "z",
                new ArgumentDefinition(
                        ArgumentType.FLOAT,
                        "Z coordinate of the location to teleport to."
                )
        );

        return arguments;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        String name = arguments.getString("name");
        float x = arguments.getFloat("x");
        float y = arguments.getFloat("y");
        float z = arguments.getFloat("z");

        ServerPlayer player = context.getServer().getPlayerList().getPlayerByName(name);

        if (player == null) {
            return new ExecutionResult(ExecutionStatus.FAILURE, "Player not found.");
        }

        player.teleportTo(x, y, z);
        return new ExecutionResult(ExecutionStatus.SUCCESS, "Player teleported.");
    }
}
