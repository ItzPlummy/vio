package com.plummy.vio.script;

import com.plummy.vio.storage.Storage;

// One script as it's saved: its header and the source written by the model, without the header.
public class ScriptFile implements Storage.Item<String> {

    private final String name;
    private final ScriptHeader header;
    private final String source;

    public ScriptFile(String name, ScriptHeader header, String source) {
        this.name = name;
        this.header = header;
        this.source = source;
    }

    @Override
    public String getKey() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public ScriptHeader getHeader() {
        return header;
    }

    public String getSource() {
        return source;
    }
}
