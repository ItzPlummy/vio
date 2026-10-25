package com.plummy.vio.core.tool.catalog.server;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class SetTimeTool implements ServerTool {

    @Override
    public String getName() {
        return "set_time";
    }

    @Override
    public String getDescription() {
        return "Set the time on the server in ticks.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("time", new ArgumentDefinition(ArgumentType.INTEGER, "Time to set in ticks, 0-23999. 0 - sunrise, 6000 - noon, 12000 - sunset, 18000 - midnight."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(ServerTool.Context context, Arguments arguments) {
        int time = arguments.getInt("time", 0, 23999);

        for (ServerLevel level : context.getServer().getAllLevels()) {
            level.setDayTime(time);
        }

        JsonObject json = new JsonObject();

        json.addProperty("time_ticks", time);

        return ExecutionResult.success(json);
    }
}
