package com.plummy.vio.provider;

public class AgentProviderException extends RuntimeException {

    public AgentProviderException(String message) {
        super(message);
    }

    public AgentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
