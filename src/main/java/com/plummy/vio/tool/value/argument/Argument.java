package com.plummy.vio.tool.value.argument;

public interface Argument {

    class ParseException extends RuntimeException {

        public ParseException(String message) {
            super(message);
        }
    }
}
