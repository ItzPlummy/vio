package com.plummy.vio.tool.catalog.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Page;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;

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
        return "Scripts saved on this server, sorted by name: type, state (enabled, disabled or errored, with the error), description, interval or permission level, who created them and who last enabled or disabled them. Scripts are shared: every FULL player can use and change all of them.%s".formatted(ScriptTools.DATA_NOTE);
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 50, "scripts"));

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
        Page page = arguments.getPage("page", MAX_LIMIT);
        List<Script> scripts = ScriptTools.getManager().getAll();
        JsonArray scriptsJson = new JsonArray();

        for (Script script : page.apply(scripts)) {
            scriptsJson.add(ScriptTools.describe(context.getServer(), script));
        }

        JsonObject json = new JsonObject();

        json.add("scripts", scriptsJson);
        json.add("page", page.withTotal(scripts.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }
}
