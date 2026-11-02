package com.plummy.vio.profile;

import java.util.ArrayList;
import java.util.List;

public class ProfileContentSerializer {

    private static final ProfileContentSerializer INSTANCE = new ProfileContentSerializer();

    public static ProfileContentSerializer getInstance() {
        return INSTANCE;
    }

    private ProfileContentSerializer() {
    }

    public String serialize(Profile profile) {
        List<String> blocks = new ArrayList<>();

        blocks.add(serializePlayerProfile(profile.getPlayerInfo()));
        blocks.add(serializeServerProfile(profile.getServerInfo()));

        if (profile.getScriptInfo() != null) {
            blocks.add(serializeScriptContext(profile.getScriptInfo()));
        }

        return String.join("\n\n", blocks);
    }

    private static String serializePlayerProfile(Profile.PlayerInfo info) {
        return "<player_profile>\nPlayer UUID: %s\nNickname: %s\nAccess level: %s\nExperience level: %s\nGame mode: %s\n</player_profile>".formatted(
                info.id(),
                info.name(),
                info.accessLevel(),
                info.level(),
                info.gameMode()
        );
    }

    private static String serializeServerProfile(Profile.ServerInfo info) {
        return "<server_profile>\nDimensions: %s\n</server_profile>".formatted(String.join(", ", info.dimensions()));
    }

    private static String serializeScriptContext(Profile.ScriptInfo info) {
        StringBuilder scripts = new StringBuilder();

        for (Profile.ScriptInfo.Entry script : info.scripts()) {
            scripts.append("\n- %s (%s, %s): %s".formatted(script.name(), script.type(), script.state(), script.description()));
        }

        return "<script_context>\nSaved scripts: %s%s\n</script_context>".formatted(info.scripts().size(), scripts);
    }
}
