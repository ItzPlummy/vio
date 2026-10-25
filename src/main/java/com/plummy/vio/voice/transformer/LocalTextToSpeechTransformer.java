package com.plummy.vio.voice.transformer;

import com.k2fsa.sherpa.onnx.*;
import com.plummy.vio.Vio;
import com.plummy.vio.voice.VoiceChat;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

// Piper through sherpa-onnx, on the server's CPU. A voice is downloaded and loaded the first time a reply in its
// language is spoken, then kept loaded.
public class LocalTextToSpeechTransformer implements TextToSpeechTransformer {

    private static final Pattern CYRILLIC = Pattern.compile("\\p{IsCyrillic}");

    private final int threads;
    private final Map<PiperVoice, OfflineTts> voices = new EnumMap<>(PiperVoice.class);

    public LocalTextToSpeechTransformer(int threads) {
        this.threads = threads;
    }

    public int getThreads() {
        return threads;
    }

    @Override
    public short[] synthesize(String text, String language) {
        GeneratedAudio audio = getVoice(selectVoice(text, language)).generate(text, 0, 1.0f);

        return Resampler.resample(toSamples(audio.getSamples()), audio.getSampleRate(), VoiceChat.SAMPLE_RATE);
    }

    @Override
    public void release() {
        voices.values().forEach(OfflineTts::release);
        voices.clear();
    }

    // The language's voice, or one guessed from the text when the language is unknown or has none.
    private static PiperVoice selectVoice(String text, String language) {
        PiperVoice voice = PiperVoice.fromLanguage(language);

        if (voice != null) {
            return voice;
        }

        return CYRILLIC.matcher(text).find() ? PiperVoice.RUSSIAN : PiperVoice.ENGLISH;
    }

    private OfflineTts getVoice(PiperVoice voice) {
        OfflineTts tts = voices.get(voice);

        if (tts == null) {
            tts = loadVoice(voice);
            voices.put(voice, tts);
        }

        return tts;
    }

    private OfflineTts loadVoice(PiperVoice voice) {
        long start = System.nanoTime();

        SherpaOnnx.loadNatives();

        Path voiceDirectory = SherpaOnnx.getDirectory().resolve(voice.getName());
        Path dataDirectory = SherpaOnnx.getDirectory().resolve(PiperVoice.getDataDirectory());

        SherpaOnnx.prepareFile(voice.getUri(voice.getModelFile()), voiceDirectory.resolve(voice.getModelFile()), voice.getModelSha256());
        SherpaOnnx.prepareFile(voice.getUri(voice.getTokensFile()), voiceDirectory.resolve(voice.getTokensFile()), voice.getTokensSha256());

        // espeak-ng starts once per process and only sees the languages present then, so the first voice loaded
        // brings every voice's data; it's small next to a model.
        for (PiperVoice dataVoice : PiperVoice.values()) {
            for (String file : dataVoice.getDataFiles()) {
                SherpaOnnx.prepareFile(dataVoice.getDataUri(file), dataDirectory.resolve(file), dataVoice.getDataSha256(file));
            }
        }

        OfflineTtsVitsModelConfig vits = OfflineTtsVitsModelConfig.builder()
                .setModel(voiceDirectory.resolve(voice.getModelFile()).toString())
                .setTokens(voiceDirectory.resolve(voice.getTokensFile()).toString())
                .setDataDir(dataDirectory.toString())
                .build();

        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setVits(vits)
                .setNumThreads(getThreads())
                .setDebug(false)
                .build();

        OfflineTts tts = new OfflineTts(OfflineTtsConfig.builder()
                .setModel(modelConfig)
                .build());

        Vio.getLogger().info("Text to speech voice %s loaded in %s ms".formatted(voice, (System.nanoTime() - start) / 1_000_000));

        return tts;
    }

    // Piper's samples are floats in [-1, 1], though they can overshoot a little.
    private static short[] toSamples(float[] audio) {
        short[] samples = new short[audio.length];

        for (int i = 0; i < audio.length; i++) {
            samples[i] = (short) Math.round(Math.clamp(audio[i], -1f, 1f) * Short.MAX_VALUE);
        }

        return samples;
    }
}
