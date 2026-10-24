package com.plummy.vio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractJsonStorage<K, T extends Storage.Item<K>> implements Storage<K, T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    protected void writeJson(Path path, JsonObject json) {
        try {
            AtomicFiles.writeString(path, GSON.toJson(json));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save " + path, e);
        }
    }
}
