package com.plummy.vio.voice.transformer;

public final class Resampler {

    private Resampler() {
    }

    // Linear interpolation; only ever upsamples here, where it's good enough for speech.
    public static short[] resample(short[] samples, int fromRate, int toRate) {
        if (fromRate == toRate || samples.length == 0) {
            return samples;
        }

        short[] resampled = new short[(int) ((long) samples.length * toRate / fromRate)];

        for (int i = 0; i < resampled.length; i++) {
            double position = (double) i * fromRate / toRate;
            int index = (int) position;
            int current = samples[index];
            int next = samples[Math.min(index + 1, samples.length - 1)];

            resampled[i] = (short) Math.round(current + (next - current) * (position - index));
        }

        return resampled;
    }
}
