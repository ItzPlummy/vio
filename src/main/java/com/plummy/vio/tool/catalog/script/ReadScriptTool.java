package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.script.ScriptSource;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ReadScriptTool implements ServerTool {

    @Override
    public String getName() {
        return "read_script";
    }

    @Override
    public String getDescription() {
        return "One script's details, as in list_scripts, plus its full source, null if its source file is missing. Compile error line numbers refer to lines of this source.%s".formatted(ScriptTools.DATA_NOTE);
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
        ScriptManager manager = ScriptTools.getManager();
        Script script = ScriptTools.getScript(manager, ScriptTools.getKey(arguments));
        ScriptSource source = manager.getScriptSource(script.getKey());
        JsonObject json = ScriptTools.describe(manager, script);

        json.addProperty("source", source == null ? null : source.getSource());

        return ExecutionResult.success(json);
    }
}
