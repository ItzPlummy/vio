package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.argument.Arguments;

public record ExecutionRequest(String name, Arguments arguments) {
}
