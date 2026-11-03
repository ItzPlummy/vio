package com.plummy.vio.voice.transformer;

import com.google.gson.JsonObject;
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
import java.nio.charset.StandardCharsets;

public class VoiceStudioTextToSpeechTransformer implements TextToSpeechTransformer {

    private static final int PCM_SAMPLE_RATE = 24_000;

    private final String uri;
    private final String model;
    private final String voice;
    private final String apiKey;
    private final HttpClient httpClient;

    public VoiceStudioTextToSpeechTransformer(String uri, String model, String voice, String apiKey) {
        this.uri = uri;
        this.model = model;
        this.voice = voice;
        this.apiKey = apiKey;
        this.httpClient = Vio.getHttpClient();
    }

    public String getUri() {
        return uri;
    }

    public String getModel() {
        return model;
    }

    public String getVoice() {
        return voice;
    }

    public String getApiKey() {
        return apiKey;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public short[] synthesize(String text, String language) {
        JsonObject body = new JsonObject();

        body.addProperty("model", getModel());
        body.addProperty("input", text);
        body.addProperty("voice", getVoice());
        body.addProperty("response_format", "pcm");

        if (language != null) {
            body.addProperty("language", language);
        }

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s".formatted(getUri(), Endpoints.VOICESTUDIO_SPEECH_PATH)))
                .timeout(VoiceLimits.REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));

        if (!getApiKey().isBlank()) {
            request.header("Authorization", "Bearer %s".formatted(getApiKey()));
        }

        HttpResponse<byte[]> response;

        try {
            response = getHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new TransformerException("Failed to send the VoiceStudio request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformerException("VoiceStudio request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new TransformerException("VoiceStudio returned %s: %s".formatted(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8)));
        }

        return Resampler.resample(toSamples(response.body()), PCM_SAMPLE_RATE, VoiceChat.SAMPLE_RATE);
    }

    private static short[] toSamples(byte[] pcm) {
        short[] samples = new short[pcm.length / 2];
        ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

        return samples;
    }
}
