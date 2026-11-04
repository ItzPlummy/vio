package com.plummy.vio.core.turn;

import com.plummy.vio.core.user.PlayerUser;

import java.time.Instant;
import java.util.List;

public final class PlayerTurn extends AbstractTurn<PlayerUser> implements Turn<PlayerUser> {

    private final PlayerPosition playerPosition;
    private final PlayerState playerState;
    private final PlayerInventory playerInventory;
    private final ServerState serverState;

    PlayerTurn(
            PlayerUser user,
            InputType inputType,
            String request,
            PlayerPosition playerPosition,
            PlayerState playerState,
            PlayerInventory playerInventory,
            ServerState serverState
    ) {
        super(user, inputType, request);
        this.playerPosition = playerPosition;
        this.playerState = playerState;
        this.playerInventory = playerInventory;
        this.serverState = serverState;
    }

    PlayerTurn(
            PlayerUser user,
            InputType inputType,
            TurnStatus status,
            String request,
            String response,
            List<ToolExecution> toolExecutions,
            Instant turnStartTime,
            Instant turnEndTime,
            PlayerPosition playerPosition,
            PlayerState playerState,
            PlayerInventory playerInventory,
            ServerState serverState
    ) {
        super(user, inputType, status, request, response, toolExecutions, turnStartTime, turnEndTime);
        this.playerPosition = playerPosition;
        this.playerState = playerState;
        this.playerInventory = playerInventory;
        this.serverState = serverState;
    }

    @Override
    public <V> V accept(TurnVisitor<V> visitor) {
        return visitor.visit(this);
    }

    public PlayerPosition getPlayerPosition() {
        return playerPosition;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }

    public ServerState getServerState() {
        return serverState;
    }

    public record PlayerPosition(double x, double y, double z, double yaw, double pitch, String dimension, String biome, List<String> structures) {
    }

    public record PlayerState(double health, double food) {
    }

    public record PlayerInventory(String mainHandItem, String offHandItem) {
    }

    public record ServerState(long time, long day) {
    }
}
