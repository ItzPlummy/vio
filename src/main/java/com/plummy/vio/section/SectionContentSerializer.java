package com.plummy.vio.section;

import com.plummy.vio.message.Message;

import java.util.stream.Collectors;

public class SectionContentSerializer implements SectionVisitor<String> {

    private static final SectionContentSerializer INSTANCE = new SectionContentSerializer();

    public static SectionContentSerializer getInstance() {
        return INSTANCE;
    }

    private SectionContentSerializer() {
    }

    @Override
    public String visitTextSection(TextSection section) {
        return section.getText();
    }

    @Override
    public String visitActivityDescriptionSection(ActivityDescriptionSection section) {
        return section.getActivityDescription();
    }

    @Override
    public String visitPlayerRequestSection(PlayerRequestSection section) {
        return "<player_request>\n%s\n</player_request>"
                .formatted(section.getRequest());
    }

    @Override
    public String visitPlayerContextSection(PlayerContextSection section) {
        return "<player_context>\nPlayer UUID: %s\nNickname: %s\nPosition X: %.3f\nPosition Y: %.3f\nPosition Z: %.3f\nYaw: %.1f\nPitch: %.1f\nAccess level: %s\n</player_context>"
                .formatted(section.getId(), section.getName(), section.getX(), section.getY(), section.getZ(), section.getYaw(), section.getPitch(), section.getAccessLevel());
    }

    @Override
    public String visitServerContextSection(ServerContextSection section) {
        return "<server_context>\nDimensions: %s\nTime of day in ticks (0-23999): %d\nDay: %d\n</server_context>"
                .formatted(section.getDimensions(), section.getTime() % 24000, section.getTime() / 24000);
    }

    public String getMessageContent(Message message) {
        return message.getSections().stream()
                .map(section -> section.accept(this))
                .collect(Collectors.joining("\n\n"));
    }
}
