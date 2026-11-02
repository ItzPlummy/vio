package com.plummy.vio.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class Instructions {

    private static final String PATH = "/vio/instructions.txt";

    private final String text;

    private Instructions(String text) {
        this.text = text;
    }

    public static Instructions load() {
        try (InputStream stream = Instructions.class.getResourceAsStream(PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Agent instructions are missing");
            }

            return new Instructions(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read agent instructions", e);
        }
    }

    public String getText() {
        return text;
    }
}
