package com.plummy.vio.user;

import com.plummy.vio.access.AccessLevel;

import java.util.UUID;

public final class PlayerUser implements User<UUID> {

    private final UUID id;
    private final String name;
    private final AccessLevel accessLevel;

    public PlayerUser(UUID id, String name, AccessLevel accessLevel) {
        this.id = id;
        this.name = name;
        this.accessLevel = accessLevel;
    }

    @Override
    public UUID getKey() {
        return getId();
    }

    @Override
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    @Override
    public <T> T accept(UserVisitor<T> visitor) {
        return visitor.visitPlayerUser(this);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
