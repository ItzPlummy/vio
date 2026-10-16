package com.plummy.vio.client.cube;

import com.plummy.vio.client.ClientChatHistory;
import net.minecraft.Util;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

// Animates the cube in real time, so it stays smooth at any frame rate and keeps moving while the game is paused.
public class CubeController {

    private static final CubeController INSTANCE = new CubeController();

    private static final float SPEED_EASING = 2.5f;
    private static final float AXIS_EASING = 1.5f;
    private static final float FLASH_EASING = 6f;
    private static final float SIZE_EASING = 4f;
    private static final float GRID_EASING = 3f;
    // One full cycle of the pixelation, from coarse to fine and back.
    private static final long GRID_PERIOD_MILLIS = 3000;
    private static final long MIN_AXIS_CHANGE_MILLIS = 500;
    private static final long MAX_AXIS_CHANGE_MILLIS = 2000;
    // Every interval, one edge that is free fades to the accent color and back, then rests before it can fade again.
    private static final long EDGE_FADE_INTERVAL_MILLIS = 1000;
    private static final long EDGE_FADE_IN_MILLIS = 1500;
    private static final long EDGE_FADE_OUT_MILLIS = 1500;
    private static final long EDGE_COOLDOWN_MILLIS = 3000;
    private static final long EDGE_CYCLE_MILLIS = EDGE_FADE_IN_MILLIS + EDGE_FADE_OUT_MILLIS + EDGE_COOLDOWN_MILLIS;
    // Long pauses (a lag spike, the window losing focus) should not make the cube jump.
    private static final float MAX_FRAME_SECONDS = 0.1f;

    private final Quaternionf orientation = new Quaternionf();
    private final Vector3f axis = randomAxis();
    private final Vector3f targetAxis = randomAxis();

    private CubeState state = CubeState.IDLE;
    private float speed = CubeState.IDLE.getSpinSpeed();
    private float alert;
    private float size = CubeState.IDLE.getSize();
    private float minGrid = CubeState.IDLE.getMinGrid();
    private float maxGrid = CubeState.IDLE.getMaxGrid();
    private int grid = CubeState.IDLE.getMaxGrid();

    // When each edge last started fading; far in the past means free.
    private final long[] edgeFadeStartMillis = new long[CubeRenderer.EDGE_COUNT];
    private final float[] edgeAccents = new float[CubeRenderer.EDGE_COUNT];

    private long lastNanos = Util.getNanos();
    private long nextAxisChangeMillis;
    private long nextEdgeFadeMillis;

    public static CubeController getInstance() {
        return INSTANCE;
    }

    private CubeController() {
        Arrays.fill(edgeFadeStartMillis, -EDGE_CYCLE_MILLIS);
    }

    // Safe to call several times per frame: the second call just advances by ~0 seconds.
    public void update() {
        long nanos = Util.getNanos();
        float seconds = Math.min((nanos - lastNanos) / 1.0E9f, MAX_FRAME_SECONDS);
        long millis = Util.getMillis();
        lastNanos = nanos;

        state = CubeState.of(ClientChatHistory.getInstance());

        speed = ease(speed, state.getSpinSpeed(), SPEED_EASING, seconds);
        alert = ease(alert, state.getAlert(), FLASH_EASING, seconds);
        size = ease(size, state.getSize(), SIZE_EASING, seconds);

        updateAxis(millis, seconds);
        updateGrid(millis, seconds);
        updateEdges(millis);

        // Rotating around a world axis that slowly drifts gives the tumbling, never-repeating motion.
        orientation.premul(new Quaternionf().rotationAxis(speed * seconds, axis)).normalize();
    }

    public CubeState getState() {
        return state;
    }

    public Quaternionf getOrientation() {
        return orientation;
    }

    public int getGrid() {
        return grid;
    }

    public float getSize() {
        return size;
    }

    // 0-1, how far the edge has faded to the accent color. Indexes follow CubeRenderer's edges.
    public float getEdgeAccent(int edge) {
        return edgeAccents[edge];
    }

    public float getAlert() {
        return alert;
    }

    private void updateAxis(long millis, float seconds) {
        // While Vio works, the cube spins fast around the axis it had when the request went out.
        if (state.isAxisLocked()) {
            return;
        }

        if (millis >= nextAxisChangeMillis) {
            targetAxis.set(randomAxis());
            nextAxisChangeMillis = millis + ThreadLocalRandom.current().nextLong(MIN_AXIS_CHANGE_MILLIS, MAX_AXIS_CHANGE_MILLIS + 1);
        }

        Vector3f next = new Vector3f(axis).lerp(targetAxis, 1 - (float) Math.exp(-AXIS_EASING * seconds));

        // Lerping between nearly opposite axes passes through zero, so keep the old axis for that frame.
        if (next.lengthSquared() > 1.0E-4f) {
            axis.set(next.normalize());
        }
    }

    // The cell count follows a sine wave across the state's range; rounding keeps it stepping in whole cells.
    private void updateGrid(long millis, float seconds) {
        minGrid = ease(minGrid, state.getMinGrid(), GRID_EASING, seconds);
        maxGrid = ease(maxGrid, state.getMaxGrid(), GRID_EASING, seconds);

        double phase = (millis % GRID_PERIOD_MILLIS) / (double) GRID_PERIOD_MILLIS * Math.PI * 2;
        float wave = (float) (Math.sin(phase) + 1) / 2;

        grid = Math.round(minGrid + (maxGrid - minGrid) * wave);
    }

    private void updateEdges(long millis) {
        if (millis >= nextEdgeFadeMillis) {
            nextEdgeFadeMillis = millis + EDGE_FADE_INTERVAL_MILLIS;

            // Only edges that are neither fading nor resting can start.
            int[] free = new int[edgeFadeStartMillis.length];
            int freeCount = 0;

            for (int edge = 0; edge < edgeFadeStartMillis.length; edge++) {
                if (millis - edgeFadeStartMillis[edge] >= EDGE_CYCLE_MILLIS) {
                    free[freeCount++] = edge;
                }
            }

            if (freeCount > 0) {
                edgeFadeStartMillis[free[ThreadLocalRandom.current().nextInt(freeCount)]] = millis;
            }
        }

        for (int edge = 0; edge < edgeAccents.length; edge++) {
            long age = millis - edgeFadeStartMillis[edge];
            float t;

            if (age < EDGE_FADE_IN_MILLIS) {
                t = (float) age / EDGE_FADE_IN_MILLIS;
            } else {
                t = 1 - Math.min(1, (float) (age - EDGE_FADE_IN_MILLIS) / EDGE_FADE_OUT_MILLIS);
            }

            // Smoothstep, so the color eases in and out of each end instead of turning sharply.
            edgeAccents[edge] = t * t * (3 - 2 * t);
        }
    }

    private static float ease(float current, float target, float rate, float seconds) {
        return current + (target - current) * (1 - (float) Math.exp(-rate * seconds));
    }

    private static Vector3f randomAxis() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector3f vector = new Vector3f((float) random.nextGaussian(), (float) random.nextGaussian(), (float) random.nextGaussian());

        return vector.lengthSquared() < 1.0E-6f ? new Vector3f(0, 1, 0) : vector.normalize();
    }
}
