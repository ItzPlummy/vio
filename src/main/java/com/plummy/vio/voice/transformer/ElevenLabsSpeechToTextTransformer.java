package com.plummy.vio.voice.transformer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.plummy.vio.Vio;
import com.plummy.vio.config.Endpoints;
import com.plummy.vio.config.VoiceLimits;
import com.plummy.vio.voice.VoiceChat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Scribe, in the cloud: the whole capture in one request, with the language detected. Short phrases are often
// detected as some other language, so when Scribe isn't sure, the capture is transcribed again as the game's language.
public class ElevenLabsSpeechToTextTransformer implements SpeechToTextTransformer {

    // Raw 16-bit little-endian mono PCM at this rate is the one format sent without a container.
    private static final String FILE_FORMAT = "pcm_s16le_16";
    private static final int FILE_SAMPLE_RATE = 16_000;
    private static final int DOWNSAMPLE_FACTOR = VoiceChat.SAMPLE_RATE / FILE_SAMPLE_RATE;

    // Scribe answers with ISO 639-3 codes, e.g. "rus", and everything else here uses ISO 639-1.
    private static final Map<String, String> ISO_639_1_CODES = getIso6391Codes();

    private final String model;
    private final double languageConfidence;
    private final String apiKey;
    private final HttpClient httpClient;

    public ElevenLabsSpeechToTextTransformer(String model, double languageConfidence, String apiKey) {
        this.model = model;
        this.languageConfidence = languageConfidence;
        this.apiKey = apiKey;
        this.httpClient = Vio.getHttpClient();
    }

    public String getModel() {
        return model;
    }

    public double getLanguageConfidence() {
        return languageConfidence;
    }

    public String getApiKey() {
        return apiKey;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void load() {
    }

    @Override
    public Transcript transcribe(short[] samples, String gameLanguage) {
        byte[] pcm = toPcm(samples);
        JsonObject detected = request(pcm, null);
        String language = getIso6391Code(detected);

        if (gameLanguage == null || gameLanguage.equals(language) || getLanguageProbability(detected) >= getLanguageConfidence()) {
            return new Transcript(getText(detected), language);
        }

        // The language is only a hint: speech in another language still comes back as itself, and then the first guess stands.
        String text = getText(request(pcm, gameLanguage));

        return new Transcript(text, text.equals(getText(detected)) ? language : gameLanguage);
    }

    @Override
    public void release() {
    }

    // With a language, Scribe reports that one back as certain, so only a request without it says what was detected.
    private JsonObject request(byte[] pcm, String language) {
        String boundary = UUID.randomUUID().toString();
        Multipart body = new Multipart(boundary);

        body.addField("model_id", getModel());
        body.addField("file_format", FILE_FORMAT);
        // Otherwise sounds like laughter come back in the text, e.g. "(laughs)".
        body.addField("tag_audio_events", "false");

        if (language != null) {
            body.addField("language_code", language);
        }

        body.addFile("file", "speech.pcm", pcm);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Endpoints.ELEVENLABS_SPEECH_TO_TEXT_URI))
                .timeout(VoiceLimits.REQUEST_TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=%s".formatted(boundary))
                .header("xi-api-key", getApiKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.build()))
                .build();

        HttpResponse<String> response;

        try {
            response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TransformerException("Failed to send the ElevenLabs request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformerException("ElevenLabs request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new TransformerException("ElevenLabs returned %s: %s".formatted(response.statusCode(), response.body()));
        }

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            json.get("text").getAsString();

            return json;
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException | UnsupportedOperationException e) {
            throw new TransformerException("Malformed ElevenLabs response: %s".formatted(response.body()), e);
        }
    }

    private static String getText(JsonObject json) {
        return json.get("text").getAsString().strip();
    }

    private static String getIso6391Code(JsonObject json) {
        return json.has("language_code") && !json.get("language_code").isJsonNull()
                ? ISO_639_1_CODES.get(json.get("language_code").getAsString())
                : null;
    }

    private static double getLanguageProbability(JsonObject json) {
        return json.has("language_probability") && !json.get("language_probability").isJsonNull()
                ? json.get("language_probability").getAsDouble()
                : 0;
    }

    // Both codes map to the ISO 639-1 one, as Scribe isn't consistent about which it sends.
    private static Map<String, String> getIso6391Codes() {
        Map<String, String> codes = new HashMap<>();

        for (String code : Locale.getISOLanguages()) {
            codes.put(code, code);
            codes.put(Locale.of(code).getISO3Language(), code);
        }

        return codes;
    }

    // Averages each group of samples down to the file's rate, which also filters out what that rate can't hold.
    private static byte[] toPcm(short[] samples) {
        ByteBuffer pcm = ByteBuffer.allocate(samples.length / DOWNSAMPLE_FACTOR * 2).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samples.length / DOWNSAMPLE_FACTOR; i++) {
            int sum = 0;

            for (int j = 0; j < DOWNSAMPLE_FACTOR; j++) {
                sum += samples[i * DOWNSAMPLE_FACTOR + j];
            }

            pcm.putShort((short) (sum / DOWNSAMPLE_FACTOR));
        }

        return pcm.array();
    }

    // multipart/form-data, which the HTTP client has no builder for.
    private static class Multipart {

        private final String boundary;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        private Multipart(String boundary) {
            this.boundary = boundary;
        }

        private void addField(String name, String value) {
            write("--%s\r\n".formatted(boundary));
            write("Content-Disposition: form-data; name=\"%s\"\r\n\r\n".formatted(name));
            write("%s\r\n".formatted(value));
        }

        private void addFile(String name, String fileName, byte[] content) {
            write("--%s\r\n".formatted(boundary));
            write("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n".formatted(name, fileName));
            write("Content-Type: application/octet-stream\r\n\r\n");
            output.writeBytes(content);
            write("\r\n");
        }

        private byte[] build() {
            write("--%s--\r\n".formatted(boundary));
            return output.toByteArray();
        }

        private void write(String text) {
            output.writeBytes(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
