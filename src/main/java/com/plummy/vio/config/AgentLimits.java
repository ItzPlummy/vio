package com.plummy.vio.config;

public final class AgentLimits {

    public static final int MAX_ROUNDS = 15;
    public static final int WORKER_THREADS = 4;
    public static final long PLAYER_LOOKUP_TIMEOUT_SECONDS = 5;
    public static final int MAX_CONVERSATION_MESSAGES = 100;
    public static final int MAX_REQUEST_LENGTH = 1000;
    public static final int MAX_HISTORY_ENTRIES = 200;
    public static final int MAX_HISTORY_LENGTH = 200_000;

    private AgentLimits() {
    }
}
