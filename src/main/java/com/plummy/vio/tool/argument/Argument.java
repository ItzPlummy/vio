package com.plummy.vio.tool.argument;

public interface Argument {

    class ParseException extends RuntimeException {

        public ParseException(String message) {
            super(message);
        }
    }
}
