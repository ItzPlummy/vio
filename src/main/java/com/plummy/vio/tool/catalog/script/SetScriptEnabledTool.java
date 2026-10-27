package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptException;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class SetScriptEnabledTool implements ServerTool {

    @Override
    public String getName() {
        return "set_script_enabled";
    }

    @Override
    public String getDescription() {
        return "Enable or disable a periodic, listener or command script; saved, so it lasts across restarts. Enabling also restarts a script that was disabled after failing repeatedly (state errored), so check read_script_log and fix the cause first. One-shot scripts can't be toggled, and a script that doesn't compile has to be fixed by replacing it.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, ScriptTools.NAME_DESCRIPTION));
        schema.put("enabled", new ArgumentDefinition(ArgumentType.BOOLEAN, "true to enable, false to disable."));

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
        Script script = ScriptTools.getScript(manager, arguments);
        boolean changed;

        try {
            changed = manager.setEnabled(script, arguments.getBoolean("enabled"), context.getPlayerId());
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        JsonObject json = new JsonObject();

        json.addProperty("name", script.getName());
        json.addProperty("state", script.getState().getValue());
        json.addProperty("changed", changed);

        return ExecutionResult.success(json);
    }
}
