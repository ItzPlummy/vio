package com.plummy.vio.core.script;

public class PeriodicScript extends AbstractToggleableScript implements Executable {

    // Runtime only: after a restart the first run is one interval away again, with no catching up.
    private int ticksUntilRun;

    public PeriodicScript(ScriptFile file, CompiledScript compiled, String error) {
        super(file, compiled, error);
        resetSchedule();
    }

    public int getIntervalTicks() {
        return getHeader().getIntervalTicks();
    }

    void resetSchedule() {
        ticksUntilRun = getIntervalTicks();
    }

    // Counts down one tick; true when a run is due.
    boolean tick() {
        if (--ticksUntilRun > 0) {
            return false;
        }

        resetSchedule();
        return true;
    }
}
