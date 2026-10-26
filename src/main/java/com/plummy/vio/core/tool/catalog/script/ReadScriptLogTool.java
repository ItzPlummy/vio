package com.plummy.vio.core.tool.catalog.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.config.ScriptLimits;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.script.Script;
import com.plummy.vio.core.script.ScriptLog;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;

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
        return "The latest entries of a script's log, oldest first: its own output (log.info/warn/error and println) and its failures, including why it was disabled. Kept in memory only, so restarts and replacing the script empty it.%s".formatted(ScriptTools.DATA_NOTE);
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
        Script script = ScriptTools.getScript(ScriptTools.getManager(), arguments);
        int lines = arguments.getInt("lines", 1, ScriptLimits.MAX_LOG_LINES);
        List<ScriptLog.Entry> entries = script.getLog().getEntries();
        List<ScriptLog.Entry> selected = entries.subList(Math.max(0, entries.size() - lines), entries.size());
        JsonArray entriesJson = new JsonArray();

        for (ScriptLog.Entry entry : selected) {
            JsonObject entryJson = new JsonObject();

            entryJson.addProperty("time", entry.time().toString());
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
