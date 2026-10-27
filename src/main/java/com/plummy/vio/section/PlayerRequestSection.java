package com.plummy.vio.section;

public class PlayerRequestSection implements Section {

    private final String request;

    public PlayerRequestSection(String request) {
        this.request = request;
    }

    @Override
    public SectionType getType() {
        return SectionType.PLAYER_REQUEST;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitPlayerRequestSection(this);
    }

    public String getRequest() {
        return request;
    }
}
