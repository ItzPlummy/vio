package com.plummy.vio.core.script;

import com.plummy.vio.core.script.dsl.CompiledScript;
import net.minecraft.server.MinecraftServer;

public interface Executable extends Script {

    default Object execute(MinecraftServer server) {
        CompiledScript instance = newInstance();

        instance.init(server, getLog());
        return instance.run();
    }
}
