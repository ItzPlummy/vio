package com.plummy.vio.script;

import com.plummy.vio.script.dsl.VioScript;
import net.minecraft.server.MinecraftServer;

// A script whose body is an action that can be run on demand.
public interface Executable {

    CompiledScript getCompiled();

    ScriptLog getLog();

    // Runs the body once on a fresh instance, so nothing carries over between runs. No budget or error handling:
    // ScriptManager adds those.
    default Object execute(MinecraftServer server) {
        VioScript instance = getCompiled().newInstance();

        instance.init(server, getLog());
        return instance.run();
    }
}
