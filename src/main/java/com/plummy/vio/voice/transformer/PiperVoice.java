package com.plummy.vio.voice.transformer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Piper voices converted for sherpa-onnx, pinned to a Hugging Face revision and checked by SHA-256. Only the
// espeak-ng data the voice's language needs is downloaded, into a directory every voice shares.
public enum PiperVoice {

    ENGLISH(
            "en",
            "en_US-hfc_male-medium",
            "fa0e01a13f2076da2ddaa7fbce30d64f319b4d25",
            "2dba095f50970dddc4f6da704b25a9767f334011b369df2ef496690ab71ea23c",
            "620e1aecf1a68fea3ba5850d137b0138fa2037c9b372dad13b95a2a215d0849a",
            "en_dict", "lang/gmw/en-US"
    ),
    // espeak-ng reads Latin words in Russian text as English, so it needs the English dictionary too.
    RUSSIAN(
            "ru",
            "ru_RU-ruslan-medium",
            "22c226825c24319e30cbf8a3844a240b9f6b1951",
            "43d3e034b04abe67c9ceab09a062617260e71804f55779cc68979a7b3e434064",
            "87c8ef66eae5473ed0cc0366b3964c736ca6c5f676c979522ea31234e47430b9",
            "en_dict", "ru_dict", "lang/zle/ru"
    );

    private static final String REPOSITORY_URI = "https://huggingface.co/csukuangfj/vits-piper-%s/resolve/%s/%s";
    private static final String DATA_DIRECTORY = "espeak-ng-data";
    private static final List<String> COMMON_DATA_FILES = List.of("phontab", "phonindex", "phondata", "intonations");
    // Every voice's repository holds the same espeak-ng data.
    private static final Map<String, String> DATA_SHA256 = Map.of(
            "phontab", "886f3fa402cb0ba73d483aa8ad000af47a6b7cc06293c75a97913fba68a530f6",
            "phonindex", "3ca7b8fa3b42624e4b0f152707e7a39245fce569aa99ea47c055d9e622fcf0c4",
            "phondata", "4e0288957874029a8c3c9f41a8f517ad4bf18127046decbdd4b9d1d6807ce3a3",
            "intonations", "3f8af65fd3eda9759a10f021d61361c120871f463515229c925995c7f90918cc",
            "en_dict", "71bd330ba8a2e3e8076e631508208ef49449d6147c17b7bd2b4b1e1468292e35",
            "ru_dict", "f0f6181bbbf9e53cd1e8f9d26bde8fc62119c4f78181948f961cb29866e5e585",
            "lang/gmw/en-US", "41534c2a22df5dd4f1052ff9e1a33a3ea7bff5a26b5c02bdad5ba8ddb7524704",
            "lang/zle/ru", "9f52d00a279aaeaa45a786b4fd3a98b34e95fedbf823f8cdb9fecc1339751d3a"
    );

    private final String language;
    private final String name;
    private final String revision;
    private final String modelSha256;
    private final String tokensSha256;
    private final List<String> dataFiles;

    PiperVoice(String language, String name, String revision, String modelSha256, String tokensSha256, String... dataFiles) {
        this.language = language;
        this.name = name;
        this.revision = revision;
        this.modelSha256 = modelSha256;
        this.tokensSha256 = tokensSha256;
        this.dataFiles = List.of(dataFiles);
    }

    // Null when there's no voice for the language.
    public static PiperVoice fromLanguage(String language) {
        for (PiperVoice voice : values()) {
            if (voice.getLanguage().equals(language)) {
                return voice;
            }
        }

        return null;
    }

    public static String getDataDirectory() {
        return DATA_DIRECTORY;
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public String getModelFile() {
        return name + ".onnx";
    }

    public String getTokensFile() {
        return "tokens.txt";
    }

    public String getModelSha256() {
        return modelSha256;
    }

    public String getTokensSha256() {
        return tokensSha256;
    }

    // Paths inside the espeak-ng data directory.
    public List<String> getDataFiles() {
        List<String> files = new ArrayList<>(COMMON_DATA_FILES);
        files.addAll(dataFiles);

        return files;
    }

    public String getDataSha256(String file) {
        return DATA_SHA256.get(file);
    }

    public URI getUri(String file) {
        return URI.create(REPOSITORY_URI.formatted(name, revision, file));
    }

    public URI getDataUri(String file) {
        return getUri(DATA_DIRECTORY + "/" + file);
    }

    @Override
    public String toString() {
        return name;
    }
}
