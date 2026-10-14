package com.plummy.vio.config;

import com.plummy.vio.Vio;
import com.plummy.vio.message.SystemMessage;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class SystemMessageProvider {

    private static final String DEFAULT_LANGUAGE = "en_us";

    private static final List<String> BUNDLED_LANGUAGES = List.of(
            "en_us",
            "ru_ru"
    );

    private final Map<String, SystemMessage> prompts;

    private SystemMessageProvider(Map<String, SystemMessage> prompts) {
        this.prompts = prompts;
    }

    public static Path getDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Vio.MODID).resolve("prompts");
    }

    public static SystemMessageProvider load() {
        Path directory = getDirectory();
        Map<String, SystemMessage> prompts = new HashMap<>();

        for (String language : BUNDLED_LANGUAGES) {
            createDefaultIfMissing(directory, language);
        }

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".txt")).forEach(path -> loadPrompt(path, prompts));
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to list {}, using the bundled system prompts", directory, e);
        }

        prompts.computeIfAbsent(DEFAULT_LANGUAGE, language -> SystemMessage.of(readBundled(language)));

        Vio.getLogger().info("Loaded system prompts for {}", prompts.keySet());
        return new SystemMessageProvider(Map.copyOf(prompts));
    }

    public SystemMessage getPrompt(String languageCode) {
        String code = normalize(languageCode);
        SystemMessage prompt = prompts.get(code);

        if (prompt != null) {
            return prompt;
        }

        String language = code.split("_", 2)[0] + "_";

        return prompts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(language))
                .min(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElseGet(() -> prompts.get(DEFAULT_LANGUAGE));
    }

    private static String normalize(String languageCode) {
        return languageCode == null ? "" : languageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static void createDefaultIfMissing(Path directory, String language) {
        Path path = directory.resolve(language + ".txt");

        if (Files.exists(path)) {
            return;
        }

        try {
            Files.createDirectories(directory);
            Files.writeString(path, readBundled(language), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to create {}", path, e);
        }
    }

    private static void loadPrompt(Path path, Map<String, SystemMessage> prompts) {
        String fileName = path.getFileName().toString();
        String language = normalize(fileName.substring(0, fileName.length() - 4));

        try {
            prompts.put(language, SystemMessage.of(Files.readString(path, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to read {}, skipping it", path, e);
        }
    }

    private static String readBundled(String language) {
        String resource = "/vio/prompts/" + language + ".txt";

        try (InputStream stream = SystemMessageProvider.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Bundled system prompt " + resource + " is missing");
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read bundled system prompt " + resource, e);
        }
    }
}
