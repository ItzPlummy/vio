package com.plummy.vio.message;

import java.util.ArrayList;
import java.util.List;

public class MessageContentSerializer implements MessageVisitor<String> {

    private static final MessageContentSerializer INSTANCE = new MessageContentSerializer();

    public static MessageContentSerializer getInstance() {
        return INSTANCE;
    }

    private MessageContentSerializer() {
    }

    @Override
    public String visitPlayerRequestMessage(PlayerRequestMessage message) {
        List<String> blocks = new ArrayList<>();

        blocks.add("<player_request>\n%s\n</player_request>".formatted(message.getRequest()));

        blocks.add(serializePlayerContext(message.getPlayerLocation(), message.getPlayerState(), message.getPlayerInventory()));
        blocks.add(serializeServerContext(message.getServerInfo()));

        return String.join("\n\n", blocks);
    }

    @Override
    public String visitToolRequestMessage(ToolRequestMessage message) {
        return message.getActivityDescription();
    }

    @Override
    public String visitToolResponseMessage(ToolResponseMessage message) {
        return "";
    }

    @Override
    public String visitAgentResponseMessage(AgentResponseMessage message) {
        return message.getResponse();
    }

    private static String serializePlayerContext(PlayerRequestMessage.PlayerLocation location, PlayerRequestMessage.PlayerState state, PlayerRequestMessage.PlayerInventory inventory) {
        return "<player_context>\nPosition X: %.3f\nPosition Y: %.3f\nPosition Z: %.3f\nYaw: %.1f\nPitch: %.1f\nDimension: %s\nBiome: %s\nInside structures: %s\nHealth: %.1f\nFood: %.0f\nMain hand item: %s\nOff hand item: %s\n</player_context>".formatted(
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch(),
                location.dimension(),
                location.biome(),
                location.structures().isEmpty() ? "none" : String.join(", ", location.structures()),
                state.health(),
                state.food(),
                inventory.mainHandItem(),
                inventory.offHandItem()
        );
    }

    private static String serializeServerContext(PlayerRequestMessage.ServerInfo info) {
        return "<server_context>\nTime of day in ticks (0-23999): %s\nDay: %s\n</server_context>".formatted(
                info.time(),
                info.day()
        );
    }
}
