package com.plummy.vio.core.script;

abstract class AbstractScript {

    private final String name;
    private final String description;
    private final String source;

    public AbstractScript(String name, String description, String source) {
        this.name = name;
        this.description = description;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }
}
