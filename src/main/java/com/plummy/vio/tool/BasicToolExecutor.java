package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.registry.ToolRegistry;

import java.util.List;
import java.util.concurrent.ExecutionException;
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

    @Override
    public List<BasicTool> getAvailableTools(AccessLevel accessLevel) {
        return ToolRegistry.getAll(BasicTool.class).stream().toList();
    }

    @Override
    public Tool.ExecutionResult execute(AccessLevel accessLevel, Tool.ExecutionRequest request) {
        try {
            return worker.submit(
                    () -> {
                        BasicTool tool = ToolRegistry.get(request.getName(), BasicTool.class);

                        if (tool == null) {
                            return Tool.ExecutionResult.invalidArguments("Unknown tool: %s.".formatted(request.getName()));
                        }

                        if (!accessLevel.isAtLeast(tool.getAccessLevel())) {
                            return Tool.ExecutionResult.denied("Tool '%s' requires access level '%s', the player has '%s'.".formatted(tool.getName(), tool.getAccessLevel(), accessLevel));
                        }

                        try {
                            return tool.execute(request.getArguments());
                        } catch (Tool.ArgumentException e) {
                            return Tool.ExecutionResult.invalidArguments(e.getMessage());
                        } catch (RuntimeException e) {
                            return Tool.ExecutionResult.failure("Internal error while executing the tool.");
                        }
                    }
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Tool.ExecutionResult.failure("Execution interrupted.");
        } catch (ExecutionException e) {
            return Tool.ExecutionResult.failure("Internal error while executing the tools.");
        }
    }
}
