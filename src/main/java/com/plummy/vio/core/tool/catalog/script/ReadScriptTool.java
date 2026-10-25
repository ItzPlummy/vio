package com.plummy.vio.core.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.script.Script;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ReadScriptTool implements ServerTool {

    @Override
    public String getName() {
        return "read_script";
    }

    @Override
    public String getDescription() {
        return "One script's details, as in list_scripts, plus its full source. Error line numbers refer to lines of this source.%s".formatted(ScriptTools.DATA_NOTE);
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, ScriptTools.NAME_DESCRIPTION));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public boolean isAvailable() {
        return ScriptTools.isAvailable();
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Script script = ScriptTools.getScript(ScriptTools.getManager(), arguments);
        JsonObject json = ScriptTools.describe(context.getServer(), script);

        json.addProperty("source", script.getSource());

        return ExecutionResult.success(json);
    }
}
