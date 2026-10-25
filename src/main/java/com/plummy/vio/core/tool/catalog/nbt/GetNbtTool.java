package com.plummy.vio.core.tool.catalog.nbt;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

public class GetNbtTool implements ServerTool {

    private final NbtTarget.Kind kind;

    public GetNbtTool(NbtTarget.Kind kind) {
        this.kind = kind;
    }

    @Override
    public String getName() {
        return "get_%s_nbt".formatted(kind.getValue());
    }

    @Override
    public String getDescription() {
        return "Raw NBT of %s, optionally at a vanilla NBT path, returned as SNBT (the same format modify_%s_nbt takes). Large results come back as a structure summary (keys with their types) instead; call again with a path. Several path matches return a list.".formatted(kind.getDescription(), kind.getValue());
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        kind.putArguments(schema);
        schema.put("path", new ArgumentDefinition(ArgumentType.STRING, "Vanilla NBT path, e.g. Inventory[{Slot:0b}].components; empty = the whole tag."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        // Command storage is server state, not something in the world around the player.
        return kind == NbtTarget.Kind.STORAGE ? AccessLevel.FULL : AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        NbtTarget target = kind.resolve(context, arguments);
        String path = arguments.getOptionalString("path");
        CompoundTag root = target.getData();

        Tag value;
        int matches;

        if (path == null) {
            value = root;
            matches = 1;
        } else {
            List<Tag> found = NbtTarget.get(root, path);
            matches = found.size();

            if (matches == 1) {
                value = found.getFirst();
            } else {
                ListTag list = new ListTag();
                list.addAll(found.stream().map(Tag::copy).toList());
                value = list;
            }
        }

        String snbt = value.toString();
        JsonObject json = new JsonObject();

        // World text inside NBT (names, sign and book text) is data, so it stays inside this SNBT string.
        json.addProperty("target", target.getDescription());
        json.addProperty("path", path);
        json.addProperty("matches", matches);

        if (snbt.length() <= 32768) {
            json.addProperty("snbt", snbt);
            json.addProperty("truncated", false);
            return ExecutionResult.success(json);
        }

        json.addProperty("truncated", true);
        json.addProperty("total_chars", snbt.length());

        if (value instanceof CompoundTag compound) {
            JsonObject keys = new JsonObject();

            for (Map.Entry<String, String> entry : NbtTarget.describeKeys(compound).entrySet()) {
                keys.addProperty(entry.getKey(), entry.getValue());
            }

            String example = compound.getAllKeys().stream().sorted().findFirst().orElse("");

            json.add("keys", keys);
            json.addProperty("hint", "Too large. Call again with a path, e.g. '%s'.".formatted(path == null ? example : "%s.%s".formatted(path, example)));
        } else {
            json.addProperty("type", NbtTarget.describeType(value));
            json.addProperty("hint", "Too large. Call again with an indexed path, e.g. '%s[0]'.".formatted(path == null ? "" : path));
        }

        return ExecutionResult.success(json);
    }
}
