package com.plummy.vio.tool.script;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.ScriptException;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class DeleteScriptTool implements ServerTool {

    @Override
    public String getName() {
        return "delete_script";
    }

    @Override
    public String getDescription() {
        return "Delete a script for good: it stops and its file is removed. There's no undo, so read_script first if its source may be needed again. Also removes a script file too broken to load, which list_scripts doesn't show.";
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
        String name = ScriptTools.getName(arguments);

        try {
            ScriptTools.getManager().delete(name);
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        JsonObject json = new JsonObject();

        json.addProperty("name", name);
        json.addProperty("deleted", true);

        return ExecutionResult.success(json);
    }
}
