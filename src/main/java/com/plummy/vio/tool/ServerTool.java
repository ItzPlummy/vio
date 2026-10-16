package com.plummy.vio.tool;

import net.minecraft.server.MinecraftServer;

public interface ServerTool extends Tool {

    ExecutionResult execute(Context context, Arguments arguments);

    default ToolType getType() {
        return ToolType.SERVER;
    }

    class Context {

        private final MinecraftServer server;

        public Context(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }
}
