package com.plummy.vio.script;

import java.time.Instant;
import java.util.UUID;

public class PeriodicScript extends AbstractToggleableScript implements Executable {

    private int ticks;
    private final int intervalTicks;

    public PeriodicScript(String name, String description, UUID creator, Instant createdAt, int intervalTicks, boolean enabled, UUID toggledBy, Instant toggledAt, String source) {
        super(name, description, creator, createdAt, enabled, toggledBy, toggledAt, source);
        this.intervalTicks = intervalTicks;
        reset();
    }

    @Override
    public ScriptType getType() {
        return ScriptType.PERIODIC;
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitPeriodicScript(this);
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    void reset() {
        ticks = intervalTicks;
    }

    boolean tick() {
        if (--ticks > 0) {
            return false;
        }

        reset();
        return true;
    }
}
