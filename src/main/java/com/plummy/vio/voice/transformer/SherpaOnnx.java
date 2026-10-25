package com.plummy.vio.voice.transformer;

import com.plummy.vio.Vio;
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

// What local speech to text and text to speech share: sherpa-onnx's native libraries and the directory models go to.
// They aren't bundled (they'd make every client's jar much bigger); they're downloaded on first use, or can be placed by hand.
public final class SherpaOnnx {

    // Must match sherpa_onnx_version in gradle.properties: the native libraries have to fit the bundled Java API.
    private static final String VERSION = "1.13.8";
    private static final String NATIVE_URI = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v%1$s/sherpa-onnx-native-lib-%2$s-%1$s.jar";
    private static final String NATIVE_PATH_PROPERTY = "sherpa_onnx.native.path";
    private static final String[] NATIVE_LIBRARIES = {"onnxruntime", "sherpa-onnx-jni"};

    private static boolean nativesLoaded;

    private SherpaOnnx() {
    }

    public static Path getDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Vio.MODID).resolve("speech");
    }

    // Before any sherpa-onnx class is used: it loads its libraries from this property the first time one is.
    public static synchronized void loadNatives() {
        if (nativesLoaded) {
            return;
        }

        System.setProperty(NATIVE_PATH_PROPERTY, prepareNatives().toString());
        nativesLoaded = true;
    }

    // Downloads next to the target only when it's missing, so a file placed by hand is kept.
    public static void prepareFile(URI uri, Path path, String sha256) {
        if (!Files.exists(path)) {
            FileDownloader.download(uri, path, sha256);
        }
    }

    private static Path prepareNatives() {
        NativePlatform platform = NativePlatform.detect();
        Path directory = getDirectory().resolve("native").resolve(VERSION).resolve(platform.getClassifier());

        if (hasNatives(directory)) {
            return directory;
        }

        Path jar = directory.resolve("natives.jar");
        FileDownloader.download(URI.create(NATIVE_URI.formatted(VERSION, platform.getClassifier())), jar, platform.getSha256());

        try {
            extractNatives(jar, directory, platform);
            Files.delete(jar);
        } catch (IOException e) {
            throw new TransformerException("Failed to extract sherpa-onnx native libraries to %s".formatted(directory), e);
        }

        if (!hasNatives(directory)) {
            throw new TransformerException("sherpa-onnx native libraries are missing from %s".formatted(directory));
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
        String prefix = "sherpa-onnx/native/%s/".formatted(platform.getClassifier());

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

            throw new TransformerException("Local speech doesn't support %s on %s".formatted(os, arch));
        }

        public String getClassifier() {
            return classifier;
        }

        public String getSha256() {
            return sha256;
        }
    }
}
