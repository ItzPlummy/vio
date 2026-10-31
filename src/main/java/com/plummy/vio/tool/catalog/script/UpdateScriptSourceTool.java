package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.ScriptLimits;
import com.plummy.vio.script.ScriptException;
import com.plummy.vio.script.ScriptKey;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.script.ScriptSource;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class UpdateScriptSourceTool implements ServerTool {

    @Override
    public String getName() {
        return "update_script_source";
    }

    @Override
    public String getDescription() {
        return "Replace an existing script's source, under the same rules as the create_*_script tool of its type. Only the source changes: the type, description, interval, enabled state and log stay. "
                + "An enabled script switches to the new source right away. If compiling or starting fails, nothing is saved and the previous version keeps running; an errored script starts working once its new source compiles. "
                + "To change anything other than the source, delete_script it and create it again.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, ScriptTools.NAME_DESCRIPTION));
        schema.put("source", new ArgumentDefinition(ArgumentType.STRING, "The new Groovy source, imports first."));

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
        ScriptKey key = ScriptTools.getScript(manager, ScriptTools.getKey(arguments)).getKey();
        String source = arguments.getString("source");

        if (source.isBlank() || source.length() > ScriptLimits.MAX_SOURCE_LENGTH) {
            return ExecutionResult.invalidArguments("source must be 1 to %s characters, got %s".formatted(ScriptLimits.MAX_SOURCE_LENGTH, source.length()));
        }

        try {
            manager.updateScriptSource(new ScriptSource(key, source));
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments("%s\nNothing was saved, and the previous version is unchanged".formatted(e.getMessage()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("name", key.getKey());
        json.addProperty("state", manager.getScriptState(key).getValue());

        return ExecutionResult.success(json);
    }
}
