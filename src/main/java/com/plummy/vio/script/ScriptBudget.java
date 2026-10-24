package com.plummy.vio.script;

// The deadline of the script call running on this thread. Compiled scripts check it at the start of every loop
// iteration, method and closure (ConditionalInterrupt), so each run, event or command gets a budget of its own.
public final class ScriptBudget {

    private static final ThreadLocal<long[]> DEADLINE = ThreadLocal.withInitial(() -> new long[]{Long.MAX_VALUE});

    private ScriptBudget() {
    }

    // Called by compiled scripts, so it must stay public.
    public static boolean isExceeded() {
        return System.nanoTime() > DEADLINE.get()[0];
    }

    // Calls can nest, e.g. a script firing an event another script handles: the inner call never outlives the outer
    // one. Returns the deadline to restore with end.
    public static long begin(long millis) {
        long[] deadline = DEADLINE.get();
        long previous = deadline[0];

        deadline[0] = Math.min(previous, System.nanoTime() + millis * 1_000_000);
        return previous;
    }

    public static void end(long previous) {
        DEADLINE.get()[0] = previous;
    }
}
