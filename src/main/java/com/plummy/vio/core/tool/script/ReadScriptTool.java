package com.plummy.vio.core.tool.script;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.script.Script;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ReadScriptTool implements ServerTool {

    @Override
    public String getName() {
        return "read_script";
    }

    @Override
    public String getDescription() {
        return "One script's details, as in list_scripts, plus its full source. Error line numbers refer to lines of this source." + ScriptTools.DATA_NOTE;
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

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
