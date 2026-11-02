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

        blocks.add(serializePlayerContext(message.getPlayerInfo(), message.getPlayerLocation(), message.getPlayerState(), message.getPlayerInventory()));
        blocks.add(serializeServerContext(message.getServerInfo()));
        if (message.getScriptInfo() != null) {
            blocks.add(serializeScriptContext(message.getScriptInfo()));
        }

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

    private static String serializePlayerContext(PlayerRequestMessage.PlayerInfo info, PlayerRequestMessage.PlayerLocation location, PlayerRequestMessage.PlayerState state, PlayerRequestMessage.PlayerInventory inventory) {
        return "<player_context>\nPlayer UUID: %s\nNickname: %s\nAccess level: %s\nPosition X: %.3f\nPosition Y: %.3f\nPosition Z: %.3f\nYaw: %.1f\nPitch: %.1f\nDimension: %s\nBiome: %s\nInside structures: %s\nHealth: %.1f\nArmor: %.0f\nFood: %.0f\nSaturation: %.1f\nExperience level: %s\nGame mode: %s\nMain hand item: %s\nOff hand item: %s\n</player_context>".formatted(
                info.id(),
                info.name(),
                info.accessLevel(),
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch(),
                location.dimension(),
                location.biome(),
                location.structures().isEmpty() ? "none" : String.join(", ", location.structures()),
                state.health(),
                state.armor(),
                state.food(),
                state.saturation(),
                state.level(),
                state.gameMode(),
                inventory.mainHandItem(),
                inventory.offHandItem()
        );
    }

    private static String serializeServerContext(PlayerRequestMessage.ServerInfo info) {
        return "<server_context>\nDimensions: %s\nTime of day in ticks (0-23999): %s\nDay: %s\n</server_context>".formatted(
                String.join(", ", info.dimensions()),
                info.time(),
                info.day()
        );
    }

    private static String serializeScriptContext(PlayerRequestMessage.ScriptInfo info) {
        StringBuilder scripts = new StringBuilder();

        for (PlayerRequestMessage.ScriptInfo.Entry script : info.scripts()) {
            scripts.append("\n- %s (%s, %s): %s".formatted(script.name(), script.type(), script.state(), script.description()));
        }

        return "<script_context>\nSaved scripts: %s%s\n</script_context>".formatted(info.scripts().size(), scripts);
    }
}
