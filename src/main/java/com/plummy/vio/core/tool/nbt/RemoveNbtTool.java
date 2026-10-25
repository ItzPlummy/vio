package com.plummy.vio.core.tool.nbt;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class RemoveNbtTool implements ServerTool {

    private final NbtTarget.Kind kind;

    public RemoveNbtTool(NbtTarget.Kind kind) {
        this.kind = kind;
    }

    @Override
    public String getName() {
        return "remove_%s_nbt".formatted(kind.getValue());
    }

    @Override
    public String getDescription() {
        return "Remove the value at an NBT path from %s, like /data remove. A path matching several tags removes them all; matches is reported.%s"
                .formatted(kind.getDescription(), kind == NbtTarget.Kind.ENTITY ? " Player data can't be changed this way." : "");
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        kind.putArguments(schema);
        schema.put("path", new ArgumentDefinition(ArgumentType.STRING, "Vanilla NBT path of the value to remove, e.g. CustomName."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        String path = arguments.getString("path");
        NbtTarget target = kind.resolve(context, arguments);

        target.requireWritable();

        CompoundTag root = target.getData().copy();
        int matches = NbtTarget.parsePath(path).remove(root);

        if (matches == 0) {
            return ExecutionResult.failure("Nothing changed: the path matched nothing.");
        }

        try {
            target.setData(root);
        } catch (CommandSyntaxException e) {
            return ExecutionResult.failure("Couldn't write the data of %s: %s".formatted(target.getDescription(), e.getMessage()));
        }

        JsonObject json = new JsonObject();

        json.addProperty("target", target.getDescription());
        json.addProperty("path", path);
        json.addProperty("matches", matches);

        return ExecutionResult.success(json);
    }
}
