package com.plummy.vio.voice.transformer;

public class TransformerException extends RuntimeException {

    public TransformerException(String message) {
        super(message);
    }

    public TransformerException(String message, Throwable cause) {
        super(message, cause);
    }
}
