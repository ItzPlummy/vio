package com.plummy.vio.voice.transformer;

// The language is an ISO 639-1 code, e.g. "ru", or null when the transformer can't tell.
public record Transcript(String text, String language) {
}
