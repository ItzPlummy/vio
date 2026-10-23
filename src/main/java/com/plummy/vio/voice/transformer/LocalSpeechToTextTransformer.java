package com.plummy.vio.voice.transformer;

import com.k2fsa.sherpa.onnx.*;
import com.plummy.vio.Vio;
import com.plummy.vio.voice.VoiceChat;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Whisper through sherpa-onnx, on the server's CPU. Its native libraries and the model aren't bundled
// (they'd make every client's jar much bigger); they're downloaded on first start, or can be placed by hand.
public class LocalSpeechToTextTransformer implements SpeechToTextTransformer {

    // Must match sherpa_onnx_version in gradle.properties: the native libraries have to fit the bundled Java API.
    private static final String SHERPA_ONNX_VERSION = "1.13.8";
    private static final String NATIVE_URI = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v%1$s/sherpa-onnx-native-lib-%2$s-%1$s.jar";
    private static final String NATIVE_PATH_PROPERTY = "sherpa_onnx.native.path";
    private static final String[] NATIVE_LIBRARIES = {"onnxruntime", "sherpa-onnx-jni"};

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

    public static Path getDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Vio.MODID).resolve("speech");
    }

    public WhisperModel getModel() {
        return model;
    }

    public int getThreads() {
        return threads;
    }

    @Override
    public void load() {
        Path nativeDirectory = prepareNatives();
        Path modelDirectory = prepareModel();

        // sherpa-onnx loads its libraries from here the first time one of its classes is used.
        System.setProperty(NATIVE_PATH_PROPERTY, nativeDirectory.toString());

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
            throw new TransformerException("Speech to text is not loaded");
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
        Path directory = getDirectory().resolve(getModel().getValue());

        prepareFile(directory, getModel().getEncoderFile(), getModel().getEncoderSha256());
        prepareFile(directory, getModel().getDecoderFile(), getModel().getDecoderSha256());
        prepareFile(directory, getModel().getTokensFile(), getModel().getTokensSha256());

        return directory;
    }

    private void prepareFile(Path directory, String file, String sha256) {
        Path path = directory.resolve(file);

        if (!Files.exists(path)) {
            FileDownloader.download(getModel().getUri(file), path, sha256);
        }
    }

    private static Path prepareNatives() {
        NativePlatform platform = NativePlatform.detect();
        Path directory = getDirectory().resolve("native").resolve(SHERPA_ONNX_VERSION).resolve(platform.getClassifier());

        if (hasNatives(directory)) {
            return directory;
        }

        Path jar = directory.resolve("natives.jar");
        FileDownloader.download(URI.create(NATIVE_URI.formatted(SHERPA_ONNX_VERSION, platform.getClassifier())), jar, platform.getSha256());

        try {
            extractNatives(jar, directory, platform);
            Files.delete(jar);
        } catch (IOException e) {
            throw new TransformerException("Failed to extract sherpa-onnx native libraries to " + directory, e);
        }

        if (!hasNatives(directory)) {
            throw new TransformerException("sherpa-onnx native libraries are missing from " + directory);
        }

        return directory;
    }

    private static boolean hasNatives(Path directory) {
        for (String library : NATIVE_LIBRARIES) {
            if (!Files.exists(directory.resolve(System.mapLibraryName(library)))) {
                return false;
            }
        }

        return true;
    }

    // The jar holds the libraries under sherpa-onnx/native/<classifier>/; only their file names are used, never the entry paths.
    private static void extractNatives(Path jar, Path directory, NativePlatform platform) throws IOException {
        String prefix = "sherpa-onnx/native/" + platform.getClassifier() + "/";

        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                    continue;
                }

                try (InputStream input = zip.getInputStream(entry)) {
                    Files.copy(input, directory.resolve(Path.of(entry.getName()).getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
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

    private enum NativePlatform {

        WINDOWS_X64("win-x64", "33fbdbd5410e9ba9bdda94aa164ec8f7825bb49246420d8ce9bdd88219d97039"),
        WINDOWS_ARM64("win-arm64", "986660bef51f0ca4f5635b763c172c64bb056eabebcd319f42185d7b23337fb8"),
        LINUX_X64("linux-x64", "30c93b59381113f9c20aedbbf9fc1ad399158f6bc03dddc0f8934a6e28e069ba"),
        LINUX_AARCH64("linux-aarch64", "5123d2e48ae1a7ce82ba89ce153906c651bf418a63db2f7dff82265e6d49c104"),
        MACOS_X64("osx-x64", "9190c28951d85efdbd376bae6b6dff12993311ad605945289e8459bf9c886a96"),
        MACOS_AARCH64("osx-aarch64", "42e272180c8836127f024f3335d7afcdfb30fe0164b78330d5d449034e34ce34");

        private final String classifier;
        private final String sha256;

        NativePlatform(String classifier, String sha256) {
            this.classifier = classifier;
            this.sha256 = sha256;
        }

        public static NativePlatform detect() {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
            boolean arm = arch.equals("aarch64") || arch.equals("arm64");
            boolean x64 = arch.equals("amd64") || arch.equals("x86_64");

            if (os.contains("win") && (x64 || arm)) {
                return x64 ? WINDOWS_X64 : WINDOWS_ARM64;
            }

            if (os.contains("linux") && (x64 || arm)) {
                return x64 ? LINUX_X64 : LINUX_AARCH64;
            }

            if ((os.contains("mac") || os.contains("darwin")) && (x64 || arm)) {
                return x64 ? MACOS_X64 : MACOS_AARCH64;
            }

            throw new TransformerException("Local speech to text doesn't support " + os + " on " + arch);
        }

        public String getClassifier() {
            return classifier;
        }

        public String getSha256() {
            return sha256;
        }
    }
}
