package com.plummy.vio.tool.argument;

// An online player by name; resolved by ServerTool.Context.getPlayer.
public class PlayerReference implements EntityReference {

    private final String name;

    public PlayerReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
