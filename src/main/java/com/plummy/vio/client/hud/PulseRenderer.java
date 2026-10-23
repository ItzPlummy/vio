package com.plummy.vio.client.hud;

import com.plummy.vio.client.FrameRenderer;
import com.plummy.vio.client.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class PulseRenderer {

    // Radians per second; the ratio is irrational enough that the wave and the noise rarely line up the same way twice.
    private static final double WAVE_SPEED = 1.15;
    private static final double NOISE_SPEED = 13.7;

    // Radians from the center to the edge, so the rings scale with the icon.
    private static final float WAVE_LENGTH = 5.25f;

    // Brightness steps of the wave, so rings come out as hard bands; the noise on top stays smooth.
    private static final int WAVE_LEVELS = 6;

    private static final int NOISE_STEPS = 29;
    // The golden angle, so each step lands in the widest remaining gap and the phases stay evenly spread.
    private static final float NOISE_SPREAD = (float) (Math.PI * (3 - Math.sqrt(5)));
    private static final float NOISE_AMOUNT = 0.5f;

    private PulseRenderer() {
    }

    // Takes the whole frame and fills its inside one frame pixel at a time, so cells line up with the frame's edge.
    public static void render(GuiGraphics graphics, int x, int y, int size, IconController icon) {
        int[][] spans = FrameRenderer.getSpans(size, size, size / 2, FrameRenderer.BORDER);

        double waveTime = icon.getPulseSeconds() * WAVE_SPEED;
        double noiseTime = icon.getFrameSeconds() * NOISE_SPEED;
        int color = Theme.lerp(Theme.ICON_PULSE, Theme.ICON_RECORDING_PULSE, icon.getTint());
        float center = size / 2f;
        float radius = center - FrameRenderer.BORDER;

        for (int row = 0; row < size; row++) {
            if (spans[row] == null) {
                continue;
            }

            for (int column = spans[row][0]; column < spans[row][1]; column++) {
                float distance = (float) Math.hypot(column + 0.5f - center, row + 0.5f - center) / radius;
                int hash = Math.floorMod(Mth.murmurHash3Mixer(row * size + column), NOISE_STEPS);

                // Minus the distance, so the wave travels outward as time grows.
                double wave = (Math.sin(waveTime - distance * WAVE_LENGTH) + 1) / 2;
                wave = Math.round(wave * (WAVE_LEVELS - 1)) / (WAVE_LEVELS - 1.0) * 2 - 1;

                double intensity = wave + Math.sin(noiseTime + hash * NOISE_SPREAD) * NOISE_AMOUNT;
                int alpha = Math.round((float) (intensity + 1 + NOISE_AMOUNT) / (2 * (1 + NOISE_AMOUNT)) * 255);

                if (alpha <= 0) {
                    continue;
                }

                graphics.fill(x + column, y + row, x + column + 1, y + row + 1, alpha << 24 | color & 0xFFFFFF);
            }
        }
    }
}
