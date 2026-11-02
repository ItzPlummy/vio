package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.config.ScriptLimits;
import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptLogEntry;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ReadScriptLogTool implements ServerTool {

    @Override
    public String getName() {
        return "read_script_log";
    }

    @Override
    public String getDescription() {
        return "The latest entries of a script's log, oldest first: its own output (println) and its failures, including why it doesn't compile or couldn't start. Kept in memory only, so restarts and delete_script empty it; update_script_source keeps it.%s".formatted(ScriptTools.DATA_NOTE);
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("name", new ArgumentDefinition(ArgumentType.STRING, ScriptTools.NAME_DESCRIPTION));
        schema.put("lines", new ArgumentDefinition(ArgumentType.INTEGER, "How many of the latest entries to return, 1 to 200."));

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
        int lines = arguments.getInt("lines", 1, ScriptLimits.MAX_LOG_LINES);
        List<ScriptLogEntry> entries = manager.getScriptLog(script.getKey()).getEntries();
        // The log is newest first, the result oldest first
        List<ScriptLogEntry> selected = entries.subList(0, Math.min(lines, entries.size())).reversed();
        JsonArray entriesJson = new JsonArray();

        for (ScriptLogEntry entry : selected) {
            JsonObject entryJson = new JsonObject();

            entryJson.addProperty("time", entry.dispatchTime().toString());
            entryJson.addProperty("level", entry.level().getValue());
            entryJson.addProperty("message", entry.message());

            entriesJson.add(entryJson);
        }

        JsonObject json = new JsonObject();

        json.add("entries", entriesJson);
        json.addProperty("returned", selected.size());
        json.addProperty("truncated", entries.size() > selected.size());

        return ExecutionResult.success(json);
    }
}
