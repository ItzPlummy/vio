package com.plummy.vio.voice.transformer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Multipart {

    private final String boundary = UUID.randomUUID().toString();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public String getContentType() {
        return "multipart/form-data; boundary=%s".formatted(boundary);
    }

    public void addField(String name, String value) {
        write("--%s\r\n".formatted(boundary));
        write("Content-Disposition: form-data; name=\"%s\"\r\n\r\n".formatted(name));
        write("%s\r\n".formatted(value));
    }

    public void addFile(String name, String fileName, byte[] content) {
        write("--%s\r\n".formatted(boundary));
        write("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n".formatted(name, fileName));
        write("Content-Type: application/octet-stream\r\n\r\n");
        output.writeBytes(content);
        write("\r\n");
    }

    public byte[] build() {
        write("--%s--\r\n".formatted(boundary));
        return output.toByteArray();
    }

    private void write(String text) {
        output.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
}
