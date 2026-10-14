package com.plummy.vio.tool;

public class ToolArgumentDefinition {

    private final ToolArgumentType type;
    private final String description;

    public ToolArgumentDefinition(ToolArgumentType type, String description) {
        this.type = type;
        this.description = description;
    }

    public ToolArgumentType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
