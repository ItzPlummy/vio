package com.plummy.vio.core.message;

import com.plummy.vio.core.section.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PlayerRequestMessage extends AbstractMessage {

    private final PlayerRequestSection request;
    private final PlayerContextSection playerContext;
    private final ServerContextSection serverContext;
    private final ScriptContextSection scriptContext;
    private final boolean isVoice;

    public PlayerRequestMessage(PlayerRequestSection request, PlayerContextSection playerContext, ServerContextSection serverContext, ScriptContextSection scriptContext, Instant time, boolean isVoice) {
        super(time);
        this.request = request;
        this.playerContext = playerContext;
        this.serverContext = serverContext;
        this.scriptContext = scriptContext;
        this.isVoice = isVoice;
    }

    public static PlayerRequestMessage of(String request, PlayerContextSection playerContext, ServerContextSection serverContext, ScriptContextSection scriptContext, Instant time, boolean isVoice) {
        return new PlayerRequestMessage(
                new PlayerRequestSection(request),
                playerContext,
                serverContext,
                scriptContext,
                time,
                isVoice
        );
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

    public boolean isVoice() {
        return isVoice;
    }
}
