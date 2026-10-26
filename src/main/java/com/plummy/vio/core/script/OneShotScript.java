package com.plummy.vio.core.script;

import java.time.Instant;
import java.util.UUID;

public class OneShotScript extends AbstractScript implements Executable {

    public OneShotScript(String name, String description, UUID creator, Instant createdAt, String source) {
        super(name, description, creator, createdAt, source);
    }

    @Override
    public ScriptType getType() {
        return ScriptType.ONE_SHOT;
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitOneShotScript(this);
    }
}
