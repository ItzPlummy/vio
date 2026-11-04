package com.plummy.vio.core.tool;

import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import net.minecraft.server.MinecraftServer;

public non-sealed interface ServerTool extends Tool {

    ExecutionResult execute(User user, Arguments arguments, Context context);

    record Context(MinecraftServer server) {
    }
}
