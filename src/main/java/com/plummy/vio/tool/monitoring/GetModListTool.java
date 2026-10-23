package com.plummy.vio.tool.monitoring;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ExecutionResult;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.argument.ArgumentDefinition;
import com.plummy.vio.tool.argument.ArgumentType;
import com.plummy.vio.tool.argument.Arguments;
import com.plummy.vio.tool.argument.Page;
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

        schema.put("limit", new ArgumentDefinition(ArgumentType.INTEGER, "Page size, 1 to 500; 100 is a good default."));
        schema.put("offset", new ArgumentDefinition(ArgumentType.INTEGER, "Entries to skip; 0 for the first page."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Page page = arguments.getPage(MAX_LIMIT);
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

        json.addProperty("loader", "neoforge " + NeoForgeVersion.getVersion());
        json.addProperty("minecraft", SharedConstants.getCurrentVersion().getName());
        json.add("mods", modsJson);
        page.write(json, mods.size());

        return ExecutionResult.success(json);
    }
}
