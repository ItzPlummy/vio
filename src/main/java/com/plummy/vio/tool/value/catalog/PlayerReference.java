package com.plummy.vio.tool.value.catalog;

public class PlayerReference implements EntityReference {

    private final String name;

    public PlayerReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
