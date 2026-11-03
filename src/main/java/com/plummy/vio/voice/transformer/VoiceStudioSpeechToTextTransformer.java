package com.plummy.vio.voice.transformer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.plummy.vio.Vio;
import com.plummy.vio.config.Endpoints;
import com.plummy.vio.config.VoiceLimits;
import com.plummy.vio.voice.VoiceChat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;

public class VoiceStudioSpeechToTextTransformer implements SpeechToTextTransformer {

    private static final String PROMPT = "Vio";
    private static final int WAV_HEADER_SIZE = 44;

    private final String uri;
    private final String apiKey;
    private final HttpClient httpClient;

    public VoiceStudioSpeechToTextTransformer(String uri, String apiKey) {
        this.uri = uri;
        this.apiKey = apiKey;
        this.httpClient = Vio.getHttpClient();
    }

    public String getUri() {
        return uri;
    }

    public String getApiKey() {
        return apiKey;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public Transcript transcribe(short[] samples, String gameLanguage) {
        Multipart body = new Multipart();

        body.addField("response_format", "verbose_json");
        body.addField("prompt", PROMPT);
        body.addFile("file", "speech.wav", toWav(samples));

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s".formatted(getUri(), Endpoints.VOICESTUDIO_TRANSCRIPTIONS_PATH)))
                .timeout(VoiceLimits.REQUEST_TIMEOUT)
                .header("Content-Type", body.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.build()));

        if (!getApiKey().isBlank()) {
            request.header("Authorization", "Bearer %s".formatted(getApiKey()));
        }

        HttpResponse<String> response;

        try {
            response = getHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TransformerException("Failed to send the VoiceStudio request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformerException("VoiceStudio request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new TransformerException("VoiceStudio returned %s: %s".formatted(response.statusCode(), response.body()));
        }

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            return new Transcript(json.get("text").getAsString().strip(), getLanguage(json));
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException | UnsupportedOperationException e) {
            throw new TransformerException("Malformed VoiceStudio response: %s".formatted(response.body()), e);
        }
    }

    private static String getLanguage(JsonObject json) {
        if (!json.has("language") || json.get("language").isJsonNull()) {
            return null;
        }

        String language = json.get("language").getAsString();

        return Arrays.asList(Locale.getISOLanguages()).contains(language) ? language : null;
    }

    private static byte[] toWav(short[] samples) {
        int dataSize = samples.length * 2;
        ByteBuffer wav = ByteBuffer.allocate(WAV_HEADER_SIZE + dataSize).order(ByteOrder.LITTLE_ENDIAN);

        wav.put("RIFF".getBytes()).putInt(WAV_HEADER_SIZE - 8 + dataSize).put("WAVE".getBytes());
        wav.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1);
        wav.putInt(VoiceChat.SAMPLE_RATE).putInt(VoiceChat.SAMPLE_RATE * 2).putShort((short) 2).putShort((short) 16);
        wav.put("data".getBytes()).putInt(dataSize);

        for (short sample : samples) {
            wav.putShort(sample);
        }

        return wav.array();
    }
}
