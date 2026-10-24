package com.plummy.vio.message;

import com.plummy.vio.section.PlayerContextSection;
import com.plummy.vio.section.PlayerRequestSection;
import com.plummy.vio.section.ScriptContextSection;
import com.plummy.vio.section.Section;
import com.plummy.vio.section.ServerContextSection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PlayerRequestMessage implements Message {

    private final PlayerRequestSection request;
    private final PlayerContextSection playerContext;
    private final ServerContextSection serverContext;
    // Null unless the player could use scripts when sending it.
    private final ScriptContextSection scriptContext;
    private final Instant time;
    private final boolean voice;

    public PlayerRequestMessage(PlayerRequestSection request, PlayerContextSection playerContext, ServerContextSection serverContext, ScriptContextSection scriptContext, Instant time, boolean voice) {
        this.request = request;
        this.playerContext = playerContext;
        this.serverContext = serverContext;
        this.scriptContext = scriptContext;
        this.time = time;
        this.voice = voice;
    }

    public static PlayerRequestMessage of(String request, PlayerContextSection playerContext, ServerContextSection serverContext, ScriptContextSection scriptContext, Instant time, boolean voice) {
        return new PlayerRequestMessage(new PlayerRequestSection(request), playerContext, serverContext, scriptContext, time, voice);
    }

    @Override
    public MessageType getType() {
        return MessageType.PLAYER_REQUEST;
    }

    @Override
    public List<Section> getSections() {
        List<Section> sections = new ArrayList<>();

        sections.add(getPlayerContext());
        sections.add(getServerContext());

        if (getScriptContext() != null) {
            sections.add(getScriptContext());
        }

        sections.add(getRequest());

        return sections;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitPlayerRequestMessage(this);
    }

    @Override
    public Instant getTime() {
        return time;
    }

    public PlayerRequestSection getRequest() {
        return request;
    }

    public PlayerContextSection getPlayerContext() {
        return playerContext;
    }

    public ServerContextSection getServerContext() {
        return serverContext;
    }

    public ScriptContextSection getScriptContext() {
        return scriptContext;
    }

    // Spoken through voice chat and transcribed, rather than typed.
    public boolean isVoice() {
        return voice;
    }
}
