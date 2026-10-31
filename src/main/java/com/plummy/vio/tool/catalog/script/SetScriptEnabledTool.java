package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptException;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.script.Toggleable;
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
        return "Enable or disable a periodic, listener or command script; saved, so it lasts across restarts. One-shot scripts can't be toggled, and an errored script has to be fixed with update_script_source; read_script_log has why it failed.";
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
        Script script = ScriptTools.getScript(manager, ScriptTools.getKey(arguments));
        boolean enabled = arguments.getBoolean("enabled");
        boolean changed = !(script instanceof Toggleable toggleable) || toggleable.isEnabled() != enabled;

        try {
            if (changed && enabled) {
                manager.enable(script.getKey());
            } else if (changed) {
                manager.disable(script.getKey());
            }
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        JsonObject json = new JsonObject();

        json.addProperty("name", script.getKey().getKey());
        json.addProperty("state", manager.getScriptState(script.getKey()).getValue());
        json.addProperty("changed", changed);

        return ExecutionResult.success(json);
    }
}
