package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.SequencedMap;
import java.util.UUID;

public interface ToolExecutor {

    List<Tool> getAvailableTools(AccessLevel accessLevel);

    SequencedMap<String, Tool.ExecutionResult> execute(MinecraftServer server, UUID playerId, SequencedMap<String, Tool.ExecutionRequest> requests);
}
