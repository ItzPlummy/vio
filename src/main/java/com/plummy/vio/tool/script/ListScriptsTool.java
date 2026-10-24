package com.plummy.vio.tool.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import com.plummy.vio.tool.argument.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ListScriptsTool implements ServerTool {

    private static final int MAX_LIMIT = 100;

    @Override
    public String getName() {
        return "list_scripts";
    }

    @Override
    public String getDescription() {
        return "Scripts saved on this server, sorted by name: type, state (enabled, disabled or errored, with the error), description, interval or permission level, who created them and who last enabled or disabled them. Scripts are shared: every FULL player can use and change all of them." + ScriptTools.DATA_NOTE;
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Page size, 1 to 100; 50 is a good default."));
        schema.put("offset", new ArgumentDefinition(ArgumentType.INTEGER, "Scripts to skip; 0 for the first page."));

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
        Page page = arguments.getPage(MAX_LIMIT);
        List<Script> scripts = ScriptTools.getManager().getAll();
        JsonArray scriptsJson = new JsonArray();

        for (Script script : page.apply(scripts)) {
            scriptsJson.add(ScriptTools.describe(context.getServer(), script));
        }

        JsonObject json = new JsonObject();

        json.add("scripts", scriptsJson);
        page.write(json, scripts.size());

        return ExecutionResult.success(json);
    }
}
