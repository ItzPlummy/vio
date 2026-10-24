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
        String structures = section.getStructures().length == 0 ? "none" : String.join(", ", section.getStructures());

        return "<player_context>\nPlayer UUID: %s\nNickname: %s\nPosition X: %.3f\nPosition Y: %.3f\nPosition Z: %.3f\nYaw: %.1f\nPitch: %.1f\nDimension: %s\nBiome: %s\nInside structures: %s\nHealth: %.1f\nArmor: %.0f\nFood: %.0f\nSaturation: %.1f\nExperience level: %d\nGame mode: %s\nMain hand item: %s\nOff hand item: %s\nAccess level: %s\n</player_context>"
                .formatted(section.getId(), section.getName(), section.getX(), section.getY(), section.getZ(), section.getYaw(), section.getPitch(), section.getDimension(), section.getBiome(), structures, section.getHealth(), section.getArmor(), section.getFood(), section.getSaturation(), section.getLevel(), section.getGameMode(), section.getMainHandItem(), section.getOffHandItem(), section.getAccessLevel());
    }

    @Override
    public String visitServerContextSection(ServerContextSection section) {
        return "<server_context>\nDimensions: %s\nTime of day in ticks (0-23999): %d\nDay: %d\n</server_context>"
                .formatted(String.join(", ", section.getDimensions()), section.getTime(), section.getDay());
    }

    @Override
    public String visitScriptContextSection(ScriptContextSection section) {
        StringBuilder scripts = new StringBuilder();

        for (ScriptContextSection.Entry script : section.getScripts()) {
            scripts.append("\n- %s (%s, %s): %s".formatted(script.name(), script.type(), script.state(), script.description()));
        }

        return "<script_context>\nSaved scripts: %s%s\n</script_context>"
                .formatted(section.getScripts().isEmpty() ? "none" : section.getScripts().size(), scripts);
    }

    public String getMessageContent(Message message) {
        return message.getSections().stream()
                .map(section -> section.accept(this))
                .collect(Collectors.joining("\n\n"));
    }
}
