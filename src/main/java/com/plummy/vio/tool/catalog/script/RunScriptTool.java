package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.ScriptLimits;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.script.*;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class RunScriptTool implements ServerTool {

    @Override
    public String getName() {
        return "run_script";
    }

    @Override
    public String getDescription() {
        return "Run a one-shot script, or a periodic script once right now even if it's disabled, and get its return value, what it printed and any error. Runs on the server thread within %s ms."
                .formatted(ServerConfig.RUN_BUDGET_MILLIS.get());
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
        ScriptOutcome result;

        try {
            result = manager.execute(script.getKey());
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        // The output is newest first, the result oldest first
        List<ScriptLogEntry> output = result.output().reversed();
        JsonArray outputJson = new JsonArray();

        for (ScriptLogEntry entry : output.subList(0, Math.min(output.size(), ScriptLimits.MAX_OUTPUT_LINES))) {
            outputJson.add(entry.level() == ScriptLogLevel.INFO ? entry.message() : "[%s] %s".formatted(entry.level(), entry.message()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("success", result.isSuccessful());
        json.addProperty("result", result.result() == null ? null : truncate(String.valueOf(result.result())));
        json.add("output", outputJson);
        json.addProperty("error", result.error());
        json.addProperty("duration_ms", result.durationMillis());
        json.addProperty("truncated", output.size() > ScriptLimits.MAX_OUTPUT_LINES);

        return ExecutionResult.success(json);
    }

    private static String truncate(String text) {
        return text.length() > ScriptLimits.MAX_RESULT_LENGTH ? "%s ...".formatted(text.substring(0, ScriptLimits.MAX_RESULT_LENGTH)) : text;
    }
}
