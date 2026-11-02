package com.plummy.vio.voice.transformer;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.config.Endpoints;
import com.plummy.vio.config.VoiceLimits;
import com.plummy.vio.voice.VoiceChat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ElevenLabsTextToSpeechTransformer implements TextToSpeechTransformer {

    private static final String PCM_FORMAT_PREFIX = "pcm_";

    private final String voiceId;
    private final String model;
    private final String outputFormat;
    private final String apiKey;
    private final HttpClient httpClient;

    public ElevenLabsTextToSpeechTransformer(String voiceId, String model, String outputFormat, String apiKey) {
        this.voiceId = voiceId;
        this.model = model;
        this.outputFormat = outputFormat;
        this.apiKey = apiKey;
        this.httpClient = Vio.getHttpClient();
    }

    public String getVoiceId() {
        return voiceId;
    }

    public String getModel() {
        return model;
    }

    public String getOutputFormat() {
        return outputFormat;
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

        body.addProperty("text", text);
        body.addProperty("model_id", getModel());

        // Also reads numbers in that language; models that can't take it ignore it.
        if (language != null) {
            body.addProperty("language_code", language);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?output_format=%s".formatted(Endpoints.ELEVENLABS_TEXT_TO_SPEECH_URI, URLEncoder.encode(getVoiceId(), StandardCharsets.UTF_8), getOutputFormat())))
                .timeout(VoiceLimits.REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("xi-api-key", getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<byte[]> response;

        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new TransformerException("Failed to send the ElevenLabs request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformerException("ElevenLabs request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new TransformerException("ElevenLabs returned %s: %s".formatted(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8)));
        }

        return Resampler.resample(toSamples(response.body()), getSampleRate(), VoiceChat.SAMPLE_RATE);
    }

    // Formats are named after their sample rate, e.g. pcm_24000.
    private int getSampleRate() {
        return Integer.parseInt(getOutputFormat().substring(PCM_FORMAT_PREFIX.length()));
    }

    // ElevenLabs PCM is raw 16-bit little-endian mono.
    private static short[] toSamples(byte[] pcm) {
        short[] samples = new short[pcm.length / 2];
        ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

        return samples;
    }

    @Override
    public void release() {
    }
}
