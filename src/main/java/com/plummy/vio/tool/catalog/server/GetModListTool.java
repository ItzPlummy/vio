package com.plummy.vio.tool.catalog.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.Page;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.SharedConstants;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class GetModListTool implements ServerTool {

    private static final int MAX_LIMIT = 500;

    @Override
    public String getName() {
        return "get_mod_list";
    }

    @Override
    public String getDescription() {
        return "Installed mods with their versions, sorted by id, plus the loader and Minecraft versions.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 100, "mods"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Page page = arguments.getPage("page", MAX_LIMIT);
        List<IModInfo> mods = ModList.get().getMods().stream().sorted(Comparator.comparing(IModInfo::getModId)).toList();

        JsonArray modsJson = new JsonArray();

        for (IModInfo mod : page.apply(mods)) {
            JsonObject json = new JsonObject();

            json.addProperty("id", mod.getModId());
            json.addProperty("name", mod.getDisplayName());
            json.addProperty("version", mod.getVersion().toString());

            modsJson.add(json);
        }

        JsonObject json = new JsonObject();

        json.addProperty("loader", "neoforge %s".formatted(NeoForgeVersion.getVersion()));
        json.addProperty("minecraft", SharedConstants.getCurrentVersion().getName());
        json.add("mods", modsJson);
        json.add("page", page.withTotal(mods.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }
}
