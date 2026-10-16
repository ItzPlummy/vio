package com.plummy.vio.message;

import com.plummy.vio.section.PlayerContextSection;
import com.plummy.vio.section.PlayerRequestSection;
import com.plummy.vio.section.Section;
import com.plummy.vio.section.ServerContextSection;

import java.time.Instant;
import java.util.List;

public class PlayerRequestMessage implements Message {

    private final PlayerRequestSection request;
    private final PlayerContextSection playerContext;
    private final ServerContextSection serverContext;
    private final Instant time;

    public PlayerRequestMessage(PlayerRequestSection request, PlayerContextSection playerContext, ServerContextSection serverContext, Instant time) {
        this.request = request;
        this.playerContext = playerContext;
        this.serverContext = serverContext;
        this.time = time;
    }

    public static PlayerRequestMessage of(String request, PlayerContextSection playerContext, ServerContextSection serverContext, Instant time) {
        return new PlayerRequestMessage(new PlayerRequestSection(request), playerContext, serverContext, time);
    }

    @Override
    public MessageType getType() {
        return MessageType.PLAYER_REQUEST;
    }

    @Override
    public List<Section> getSections() {
        return List.of(
                getPlayerContext(),
                getServerContext(),
                getRequest()
        );
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
}
