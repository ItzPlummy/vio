package com.plummy.vio.tool.value.catalog;

import java.util.UUID;

public class UuidReference implements EntityReference {

    private final UUID uuid;

    public UuidReference(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
