package com.plummy.vio.datapack;

public class DatapackException extends RuntimeException {

    public DatapackException(String message) {
        super(message);
    }

    public DatapackException(String message, Throwable cause) {
        super(message, cause);
    }
}
