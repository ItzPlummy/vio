package com.plummy.vio.core.tool.executor;

import com.plummy.vio.core.tool.BasicTool;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.Tool;
import com.plummy.vio.core.tool.argument.Arguments;
import com.plummy.vio.core.user.User;

public final class ToolExecutorPolicy implements ToolExecutor<Tool> {

    private final ToolExecutor<BasicTool> basicToolExecutor;
    private final ToolExecutor<ServerTool> serverToolExecutor;

    public ToolExecutorPolicy(ToolExecutor<BasicTool> basicToolExecutor, ToolExecutor<ServerTool> serverToolExecutor) {
        this.basicToolExecutor = basicToolExecutor;
        this.serverToolExecutor = serverToolExecutor;
    }

    @Override
    public ExecutionResult execute(Tool tool, User user, Arguments arguments) {
        return switch (tool) {
            case BasicTool basicTool -> basicToolExecutor.execute(basicTool, user, arguments);
            case ServerTool serverTool -> serverToolExecutor.execute(serverTool, user, arguments);
        };
    }
}
