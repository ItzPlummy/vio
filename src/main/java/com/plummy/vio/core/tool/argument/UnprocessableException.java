package com.plummy.vio.core.tool.argument;

import com.plummy.vio.core.tool.executor.ExecutionException;
import com.plummy.vio.core.tool.executor.ExecutionResult;

public class UnprocessableException extends ExecutionException {

    public UnprocessableException(String message) {
        super(message, ExecutionResult.unprocessable(message));
    }

    public UnprocessableException(String message, Throwable cause) {
        super(message, cause, ExecutionResult.unprocessable(message));
    }
}
