package com.plummy.vio.voice.transformer;

import com.plummy.vio.Vio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class FileDownloader {

    private static final int BUFFER_SIZE = 64 * 1024;

    private FileDownloader() {
    }

    // Downloads next to the target and moves the file in place only once its SHA-256 matches, so a target that exists is complete.
    public static void download(URI uri, Path target, String sha256) {
        Path partial = target.resolveSibling(target.getFileName() + ".part");

        Vio.getLogger().info("Downloading {} to {}", uri, target);

        try {
            Files.createDirectories(target.getParent());

            HttpResponse<InputStream> response = Vio.getHttpClient().send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = new DigestInputStream(response.body(), digest); OutputStream output = Files.newOutputStream(partial)) {
                if (response.statusCode() != 200) {
                    throw new TransformerException("Download of " + uri + " returned " + response.statusCode());
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;

                // Reading the body isn't interruptible, so a server stop is checked between chunks.
                while ((read = input.read(buffer)) >= 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }

                    output.write(buffer, 0, read);
                }
            }

            String actual = HexFormat.of().formatHex(digest.digest());

            if (!actual.equals(sha256)) {
                throw new TransformerException("Download of " + uri + " has SHA-256 " + actual + ", expected " + sha256);
            }

            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new TransformerException("Download of " + uri + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformerException("Download of " + uri + " interrupted", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        } finally {
            deleteQuietly(partial);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to delete {}", path, e);
        }
    }
}
