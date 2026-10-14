package com.plummy.vio.tool;

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
        return "Sets the time of day in all dimensions, like the /time set command.";
    }

    @Override
    public SequencedMap<String, ToolArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ToolArgumentDefinition> arguments = new LinkedHashMap<>();

        arguments.put(
                "time",
                new ToolArgumentDefinition(
                        ToolArgumentType.INTEGER,
                        "Time of day in ticks, 0-23999. 0 = sunrise, 6000 = noon, 13000 = night, 18000 = midnight."
                )
        );

        return arguments;
    }

    @Override
    public ToolExecutionResult execute(ToolContext context, ToolArguments arguments) {
        int time = arguments.getInt("time", 0, 23999);

        for (ServerLevel level : context.getServer().getAllLevels()) {
            level.setDayTime(time);
        }

        return ToolExecutionResult.success("Time set to " + time + " in all dimensions.");
    }
}
