package com.plummy.vio.core.tool.argument;

public interface Argument {

    class ParseException extends RuntimeException {

        public ParseException(String message) {
            super(message);
        }
    }
}
