package com.plummy.vio.core.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Writes through a temporary file moved into place, so a crash mid-write never leaves a half-written file.
public final class AtomicFiles {

    private AtomicFiles() {
    }

    public static void writeString(Path path, String text) throws IOException {
        Path directory = path.getParent();

        Files.createDirectories(directory);

        Path temporary = Files.createTempFile(directory, path.getFileName().toString(), ".tmp");
        Files.writeString(temporary, text, StandardCharsets.UTF_8);

        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
