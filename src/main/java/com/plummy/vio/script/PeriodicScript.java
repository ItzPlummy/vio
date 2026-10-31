package com.plummy.vio.script;

import com.plummy.vio.script.runtime.RuntimeScript;

public final class PeriodicScript implements Script, Executable, Toggleable {

    private final ScriptKey key;
    private final String description;
    private final boolean isEnabled;
    private final long periodTicks;

    public PeriodicScript(ScriptKey key, String description, boolean isEnabled, long periodTicks) {
        if (periodTicks < 1) {
            throw new ScriptException("Invalid period of %s ticks: it must be at least 1".formatted(periodTicks));
        }

        this.key = key;
        this.description = description;
        this.isEnabled = isEnabled;
        this.periodTicks = periodTicks;
    }

    @Override
    public ScriptKey getKey() {
        return key;
    }

    @Override
    public ScriptType getType() {
        return ScriptType.PERIODIC;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Class<? extends RuntimeScript> getRuntimeBaseClass() {
        return RuntimeScript.class;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public PeriodicScript withEnabled(boolean isEnabled) {
        return new PeriodicScript(key, description, isEnabled, periodTicks);
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitPeriodicScript(this);
    }

    public long getPeriodTicks() {
        return periodTicks;
    }
}
