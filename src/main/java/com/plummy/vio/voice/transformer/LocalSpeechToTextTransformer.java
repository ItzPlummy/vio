package com.plummy.vio.voice.transformer;

import com.k2fsa.sherpa.onnx.*;
import com.plummy.vio.voice.VoiceChat;

import java.nio.file.Path;

// Whisper through sherpa-onnx, on the server's CPU. The model is downloaded on first start, or can be placed by hand.
public class LocalSpeechToTextTransformer implements SpeechToTextTransformer {

    // What Whisper expects; Simple Voice Chat's rate is exactly three times this.
    private static final int MODEL_SAMPLE_RATE = 16_000;
    private static final int DOWNSAMPLE_FACTOR = VoiceChat.SAMPLE_RATE / MODEL_SAMPLE_RATE;

    private final WhisperModel model;
    private final int threads;

    private OfflineRecognizer recognizer;

    public LocalSpeechToTextTransformer(WhisperModel model, int threads) {
        this.model = model;
        this.threads = threads;
    }

    public WhisperModel getModel() {
        return model;
    }

    public int getThreads() {
        return threads;
    }

    @Override
    public void load() {
        SherpaOnnx.loadNatives();

        Path modelDirectory = prepareModel();

        OfflineWhisperModelConfig whisper = OfflineWhisperModelConfig.builder()
                .setEncoder(modelDirectory.resolve(getModel().getEncoderFile()).toString())
                .setDecoder(modelDirectory.resolve(getModel().getDecoderFile()).toString())
                .setLanguage("")
                .setTask("transcribe")
                .build();

        OfflineModelConfig modelConfig = OfflineModelConfig.builder()
                .setWhisper(whisper)
                .setTokens(modelDirectory.resolve(getModel().getTokensFile()).toString())
                .setNumThreads(getThreads())
                // Debug output is on by default and prints the whole config to the console.
                .setDebug(false)
                .build();

        recognizer = new OfflineRecognizer(OfflineRecognizerConfig.builder()
                .setOfflineModelConfig(modelConfig)
                .setDecodingMethod("greedy_search")
                .build());
    }

    @Override
    public Transcript transcribe(short[] samples, String gameLanguage) {
        if (recognizer == null) {
            throw new TransformerException("Speech to text isn't loaded");
        }

        OfflineStream stream = recognizer.createStream();

        try {
            stream.acceptWaveform(downsample(samples), MODEL_SAMPLE_RATE);
            recognizer.decode(stream);

            // The language is fixed when the model loads, so the game's can't be offered per request.
            return new Transcript(recognizer.getResult(stream).getText().strip(), null);
        } finally {
            stream.release();
        }
    }

    @Override
    public void release() {
        if (recognizer != null) {
            recognizer.release();
            recognizer = null;
        }
    }

    private Path prepareModel() {
        Path directory = SherpaOnnx.getDirectory().resolve(getModel().getValue());

        prepareFile(directory, getModel().getEncoderFile(), getModel().getEncoderSha256());
        prepareFile(directory, getModel().getDecoderFile(), getModel().getDecoderSha256());
        prepareFile(directory, getModel().getTokensFile(), getModel().getTokensSha256());

        return directory;
    }

    private void prepareFile(Path directory, String file, String sha256) {
        SherpaOnnx.prepareFile(getModel().getUri(file), directory.resolve(file), sha256);
    }

    // Averages each group of samples, which also filters out what the lower rate can't hold.
    private static float[] downsample(short[] samples) {
        float[] downsampled = new float[samples.length / DOWNSAMPLE_FACTOR];

        for (int i = 0; i < downsampled.length; i++) {
            int sum = 0;

            for (int j = 0; j < DOWNSAMPLE_FACTOR; j++) {
                sum += samples[i * DOWNSAMPLE_FACTOR + j];
            }

            downsampled[i] = sum / (DOWNSAMPLE_FACTOR * 32768f);
        }

        return downsampled;
    }
}
