package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class SetTimeTool implements Tool {

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

        schema.put("time", new ArgumentDefinition(ArgumentType.INTEGER, "Time to set in ticks, 0-23999. 0 - sunrise, 6000 - noon, 12000 - sunset, 18000 - midnight."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        int time = arguments.getInt("time", 0, 23999);

        for (ServerLevel level : context.getServer().getAllLevels()) {
            level.setDayTime(time);
        }

        return ExecutionResult.success("Time set to %d ticks.".formatted(time));
    }
}
