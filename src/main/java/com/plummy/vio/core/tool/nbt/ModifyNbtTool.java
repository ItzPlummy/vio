package com.plummy.vio.core.tool.nbt;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ResultException;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ModifyNbtTool implements ServerTool {

    private final NbtTarget.Kind kind;

    public ModifyNbtTool(NbtTarget.Kind kind) {
        this.kind = kind;
    }

    @Override
    public String getName() {
        return "modify_%s_nbt".formatted(kind.getValue());
    }

    @Override
    public String getDescription() {
        return ("Modify the NBT of %s like /data modify. Operations: merge (into the compound at path, or the root when path is \"\"), set (replaces the value at every match of path, so a path matching several tags changes them all; matches is reported), append / prepend (into a list at path). Values are SNBT; items use the 1.20.5+ format {id:\"minecraft:diamond\",count:3,components:{...}}. Use remove_%s_nbt to delete a value. Returns the value at path afterwards.%s")
                .formatted(kind.getDescription(), kind.getValue(), kind == NbtTarget.Kind.ENTITY ? " Player data can't be changed this way; use give_player_item or run_command with /item, /attribute, /effect, /tp, /gamemode, /xp." : "");
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        kind.putArguments(schema);
        schema.put("operation", ArgumentDefinition.enumeration(Operation.class, "How to modify."));
        schema.put("path", new ArgumentDefinition(ArgumentType.STRING, "Vanilla NBT path; \"\" = the root, which only merge accepts."));
        schema.put("value_snbt", new ArgumentDefinition(ArgumentType.STRING, "Value in SNBT; a compound like {key:value} for merge."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        Operation operation = arguments.getEnum("operation", Operation.class);
        String path = arguments.getOptionalString("path");
        NbtTarget target = kind.resolve(context, arguments);

        target.requireWritable();

        if (path == null && operation != Operation.MERGE) {
            return ExecutionResult.invalidArguments("'path' can't be empty for %s; only merge works on the root.".formatted(ArgumentDefinition.getEnumValue(operation)));
        }

        Tag value = parseValue(arguments.getString("value_snbt"));
        CompoundTag root = target.getData().copy();
        int matches;

        try {
            matches = switch (operation) {
                case MERGE -> merge(root, path, value);
                case SET -> NbtTarget.parsePath(path).set(root, value);
                case APPEND -> NbtTarget.parsePath(path).insert(-1, root, List.of(value));
                case PREPEND -> NbtTarget.parsePath(path).insert(0, root, List.of(value));
            };
        } catch (CommandSyntaxException e) {
            return ExecutionResult.invalidArguments("Couldn't apply %s at \"%s\": %s".formatted(ArgumentDefinition.getEnumValue(operation), path, e.getMessage()));
        }

        if (matches == 0) {
            return ExecutionResult.failure("Nothing changed: the path matched nothing or the value was already there.");
        }

        try {
            target.setData(root);
        } catch (CommandSyntaxException e) {
            return ExecutionResult.failure("Couldn't write the data of %s: %s".formatted(target.getDescription(), e.getMessage()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("target", target.getDescription());
        json.addProperty("operation", ArgumentDefinition.getEnumValue(operation));
        json.addProperty("path", path);
        json.addProperty("matches", matches);
        json.addProperty("new_value", describeAfter(target, path));

        return ExecutionResult.success(json);
    }

    private static int merge(CompoundTag root, String path, Tag value) throws CommandSyntaxException {
        if (!(value instanceof CompoundTag compound)) {
            throw ResultException.invalidArguments("merge needs a compound value like {key:value}.");
        }

        if (path == null) {
            CompoundTag before = root.copy();
            root.merge(compound);
            return before.equals(root) ? 0 : 1;
        }

        int changed = 0;

        for (Tag tag : NbtTarget.parsePath(path).getOrCreate(root, CompoundTag::new)) {
            if (!(tag instanceof CompoundTag existing)) {
                throw ResultException.invalidArguments("merge needs \"%s\" to be a compound, it is %s.".formatted(path, NbtTarget.describeType(tag)));
            }

            CompoundTag before = existing.copy();
            existing.merge(compound);

            if (!before.equals(existing)) {
                changed++;
            }
        }

        return changed;
    }

    private static Tag parseValue(String snbt) {
        StringReader reader = new StringReader(snbt);

        try {
            Tag tag = new TagParser(reader).readValue();
            reader.skipWhitespace();

            if (reader.canRead()) {
                throw ResultException.invalidArguments("value_snbt has trailing text at position %d: %s".formatted(reader.getCursor(), snbt.substring(reader.getCursor())));
            }

            return tag;
        } catch (CommandSyntaxException e) {
            throw ResultException.invalidArguments("value_snbt is not valid SNBT: " + e.getMessage());
        }
    }

    // The value at the path after the change, capped like get_*_nbt.
    private String describeAfter(NbtTarget target, String path) {
        CompoundTag root = target.getData();
        String snbt;

        try {
            if (path == null) {
                snbt = root.toString();
            } else {
                List<Tag> found = NbtTarget.parsePath(path).get(root);
                snbt = found.size() == 1 ? found.getFirst().toString() : found.toString();
            }
        } catch (CommandSyntaxException | ResultException e) {
            return null;
        }

        int maxChars = 32768;

        return snbt.length() <= maxChars ? snbt : snbt.substring(0, maxChars) + " ... (truncated; read it with get_%s_nbt and a path)".formatted(kind.getValue());
    }

    private enum Operation {
        MERGE,
        SET,
        APPEND,
        PREPEND
    }
}
