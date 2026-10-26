package com.plummy.vio.voice.transformer;

import com.plummy.vio.config.Endpoints;
import java.net.URI;

// Multilingual Whisper models converted for sherpa-onnx, pinned to a Hugging Face revision and checked by SHA-256.
public enum WhisperModel {

    TINY(
            "whisper-tiny",
            "tiny",
            "65176e2deb88badc814a94058666cadccc29b61c",
            "d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434",
            "d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925"
    ),
    BASE(
            "whisper-base",
            "base",
            "bb53ee204431c90d314c1cc08d28d23e5b7927cc",
            "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11",
            "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d"
    ),
    SMALL(
            "whisper-small",
            "small",
            "8f3c18b358db4d1f2fc1eae49d75cd20989e4309",
            "4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9",
            "acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee"
    );

    private final String value;
    private final String size;
    private final String revision;
    private final String encoderSha256;
    private final String decoderSha256;

    WhisperModel(String value, String size, String revision, String encoderSha256, String decoderSha256) {
        this.value = value;
        this.size = size;
        this.revision = revision;
        this.encoderSha256 = encoderSha256;
        this.decoderSha256 = decoderSha256;
    }

    public static WhisperModel fromValue(String value) {
        for (WhisperModel model : values()) {
            if (model.getValue().equals(value)) {
                return model;
            }
        }

        throw new IllegalArgumentException("Invalid Whisper model: %s".formatted(value));
    }

    public String getValue() {
        return value;
    }

    public String getEncoderFile() {
        return "%s-encoder.int8.onnx".formatted(size);
    }

    public String getDecoderFile() {
        return "%s-decoder.int8.onnx".formatted(size);
    }

    public String getTokensFile() {
        return "%s-tokens.txt".formatted(size);
    }

    public String getEncoderSha256() {
        return encoderSha256;
    }

    public String getDecoderSha256() {
        return decoderSha256;
    }

    public String getTokensSha256() {
        return Endpoints.WHISPER_TOKENS_SHA256;
    }

    public URI getUri(String file) {
        return URI.create(Endpoints.WHISPER_REPOSITORY_URI.formatted(size, revision, file));
    }

    @Override
    public String toString() {
        return value;
    }
}
