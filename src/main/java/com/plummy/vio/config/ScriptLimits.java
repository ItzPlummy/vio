package com.plummy.vio.config;

public final class ScriptLimits {

    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int MAX_INTERVAL_TICKS = 72000;
    public static final int DECLARE_BUDGET_MILLIS = 100;
    public static final int LOG_CAPACITY = 200;
    public static final int MAX_LOG_MESSAGE_LENGTH = 1000;
    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final int MAX_SOURCE_LENGTH = 65536;
    public static final int MAX_OUTPUT_LINES = 50;
    public static final int MAX_RESULT_LENGTH = 2000;
    public static final int MAX_LOG_LINES = 200;

    private ScriptLimits() {
    }
}
