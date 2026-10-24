package com.plummy.vio.tool.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.ServerConfig;
import com.plummy.vio.script.ScriptException;
import com.plummy.vio.script.ScriptLog;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class RunScriptTool implements ServerTool {

    private static final int MAX_OUTPUT_LINES = 50;
    private static final int MAX_RESULT_LENGTH = 2000;

    @Override
    public String getName() {
        return "run_script";
    }

    @Override
    public String getDescription() {
        return "Run a one-shot script, or a periodic script once right now even if it's disabled, and get its return value, what it logged and any error with its line. Runs on the server thread within %d ms. Failures here never disable a script."
                .formatted(ServerConfig.RUN_BUDGET_MILLIS.get());
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
        ScriptManager manager = ScriptTools.getManager();
        ScriptManager.RunResult result;

        try {
            result = manager.run(ScriptTools.getScript(manager, arguments));
        } catch (ScriptException e) {
            return ExecutionResult.invalidArguments(e.getMessage());
        }

        List<ScriptLog.Entry> output = result.output();
        JsonArray outputJson = new JsonArray();

        for (ScriptLog.Entry entry : output.subList(0, Math.min(output.size(), MAX_OUTPUT_LINES))) {
            outputJson.add(entry.level() == ScriptLog.Level.INFO ? entry.message() : "[%s] %s".formatted(entry.level(), entry.message()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("success", result.success());
        json.addProperty("result", result.result() == null ? null : truncate(String.valueOf(result.result())));
        json.add("output", outputJson);
        json.addProperty("error", result.error());
        json.addProperty("duration_ms", result.durationMillis());
        json.addProperty("truncated", output.size() > MAX_OUTPUT_LINES);

        return ExecutionResult.success(json);
    }

    private static String truncate(String text) {
        return text.length() > MAX_RESULT_LENGTH ? text.substring(0, MAX_RESULT_LENGTH) + " ..." : text;
    }
}
