package com.plummy.vio.tool;

import com.plummy.vio.tool.argument.Arguments;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicToolExecutor extends AbstractToolExecutor<BasicTool> {

    private static final int WORKER_THREADS = 4;

    private final ExecutorService worker = Executors.newFixedThreadPool(
            WORKER_THREADS,
            runnable -> {
                Thread thread = new Thread(runnable, "vio_basic_tool_executor");
                thread.setDaemon(true);
                return thread;
            }
    );

    public BasicToolExecutor() {
        super(BasicTool.class);
    }

    @Override
    public ExecutionResult execute(Requester requester, ExecutionRequest request) {
        return await(worker.submit(() -> executeNow(requester, request)));
    }

    @Override
    protected ExecutionResult invoke(BasicTool tool, Requester requester, Arguments arguments) {
        return tool.execute(requester, arguments);
    }
}
