package com.plummy.vio.core.tool.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.core.tool.ResultException;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.argument.ArgumentType;
import com.plummy.vio.core.tool.argument.Arguments;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.CommandStorage;

import java.util.*;

// The block entity, entity or command storage an NBT tool addresses, read and written like /data does.
public class NbtTarget {

    private final String description;
    private final DataAccessor accessor;
    private final CommandStorage storage;
    private final ResourceLocation storageId;
    private final boolean player;

    private NbtTarget(String description, DataAccessor accessor, CommandStorage storage, ResourceLocation storageId, boolean player) {
        this.description = description;
        this.accessor = accessor;
        this.storage = storage;
        this.storageId = storageId;
        this.player = player;
    }

    static NbtPathArgument.NbtPath parsePath(String path) {
        try {
            return NbtPathArgument.nbtPath().parse(new StringReader(path));
        } catch (CommandSyntaxException e) {
            throw ResultException.invalidArguments("'path' is not a valid NBT path: " + e.getMessage());
        }
    }

    // The tags at the path; when nothing matches, the error lists the keys at the deepest part of the path that did.
    static List<Tag> get(CompoundTag root, String path) {
        try {
            return parsePath(path).get(root);
        } catch (CommandSyntaxException e) {
            String deepest = "";
            List<Tag> found = List.of(root);

            for (String prefix : getPrefixes(path)) {
                try {
                    found = parsePath(prefix).get(root);
                    deepest = prefix;
                } catch (CommandSyntaxException | ResultException ignored) {
                    break;
                }
            }

            List<String> keys = new ArrayList<>();

            for (Tag tag : found) {
                if (tag instanceof CompoundTag compound) {
                    keys.addAll(compound.getAllKeys());
                } else if (tag instanceof CollectionTag<?> list) {
                    keys.add("[0.." + (list.size() - 1) + "]");
                }
            }

            throw ResultException.invalidArguments("Path \"%s\" matches nothing. %s has: %s".formatted(path, deepest.isEmpty() ? "The root" : "\"" + deepest + "\"", keys.stream().distinct().sorted().toList()));
        }
    }

    // Path prefixes that end on a node boundary, shortest first: a.b[0].c -> a, a.b, a.b[0].
    private static List<String> getPrefixes(String path) {
        List<String> prefixes = new ArrayList<>();
        int depth = 0;
        boolean quoted = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);

            if (c == '"') {
                quoted = !quoted;
            } else if (!quoted && (c == '[' || c == '{')) {
                if (depth == 0 && i > 0) {
                    prefixes.add(path.substring(0, i));
                }

                depth++;
            } else if (!quoted && (c == ']' || c == '}')) {
                depth--;
            } else if (!quoted && depth == 0 && c == '.' && i > 0) {
                prefixes.add(path.substring(0, i));
            }
        }

        return prefixes;
    }

    // Key -> "compound[4]", "list[31]", "string", ... for results too large to show whole.
    static Map<String, String> describeKeys(CompoundTag tag) {
        Map<String, String> keys = new LinkedHashMap<>();

        for (String key : tag.getAllKeys().stream().sorted().toList()) {
            keys.put(key, describeType(tag.get(key)));
        }

        return keys;
    }

    static String describeType(Tag tag) {
        return switch (tag) {
            case CompoundTag compound -> "compound[%d]".formatted(compound.size());
            case CollectionTag<?> list -> "list[%d]".formatted(list.size());
            default -> tag.getType().getName().toLowerCase();
        };
    }

    CompoundTag getData() {
        try {
            return accessor != null ? accessor.getData() : storage.get(storageId);
        } catch (CommandSyntaxException e) {
            throw ResultException.failure("Couldn't read the data of %s: %s".formatted(description, e.getMessage()));
        }
    }

    // Entity.load on a player doesn't resync position, abilities, game mode or effects to the client, which is why vanilla refuses it too.
    void requireWritable() {
        if (player) {
            throw ResultException.invalidArguments("Player data can't be modified through NBT. Use give_player_item, or run_command with /item, /attribute, /effect, /tp, /gamemode or /xp.");
        }
    }

    void setData(CompoundTag tag) throws CommandSyntaxException {
        if (accessor != null) {
            accessor.setData(tag);
        } else {
            storage.set(storageId, tag);
        }
    }

    String getDescription() {
        return description;
    }

    boolean isPlayer() {
        return player;
    }

    // What an NBT tool addresses; each kind has its own tool per operation (get_block_nbt, modify_entity_nbt, ...) and its own arguments.
    public enum Kind {
        BLOCK("block", "the block entity at a position") {
            @Override
            void putArguments(SequencedMap<String, ArgumentDefinition> schema) {
                schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Block position [x, y, z]."));
                schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id."));
            }

            @Override
            NbtTarget resolve(ServerTool.Context context, Arguments arguments) {
                ServerLevel level = context.getLevel(arguments.getDimension("dimension"));
                BlockPos pos = arguments.getBlockPosition("position").toBlockPos();

                context.requireLoaded(level, pos);

                BlockEntity blockEntity = level.getBlockEntity(pos);
                String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();

                if (blockEntity == null) {
                    throw ResultException.invalidArguments("%s at %d %d %d (%s) has no block entity, so it has no NBT.".formatted(block, pos.getX(), pos.getY(), pos.getZ(), level.dimension().location()));
                }

                return new NbtTarget("%s at %d %d %d (%s)".formatted(block, pos.getX(), pos.getY(), pos.getZ(), level.dimension().location()), new BlockDataAccessor(blockEntity, pos), null, null, false);
            }
        },
        ENTITY("entity", "an entity") {
            @Override
            void putArguments(SequencedMap<String, ArgumentDefinition> schema) {
                schema.put("entity", new ArgumentDefinition(ArgumentType.ENTITY_REFERENCE, "Entity UUID or player name."));
            }

            @Override
            NbtTarget resolve(ServerTool.Context context, Arguments arguments) {
                Entity entity = context.getEntity(arguments.getEntityReference("entity"));
                boolean isPlayer = entity instanceof Player;

                String description = isPlayer
                        ? "player " + entity.getName().getString()
                        : "%s %s".formatted(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entity.getStringUUID());

                return new NbtTarget(description, new EntityDataAccessor(entity), null, null, isPlayer);
            }
        },
        STORAGE("storage", "command storage") {
            @Override
            void putArguments(SequencedMap<String, ArgumentDefinition> schema) {
                schema.put("storage_id", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Storage id, e.g. mypack:state."));
            }

            @Override
            NbtTarget resolve(ServerTool.Context context, Arguments arguments) {
                ResourceLocation id = arguments.getIdentifier("storage_id").getId();

                return new NbtTarget("storage " + id, null, context.getServer().getCommandStorage(), id, false);
            }
        };

        private final String value;
        private final String description;

        Kind(String value, String description) {
            this.value = value;
            this.description = description;
        }

        abstract void putArguments(SequencedMap<String, ArgumentDefinition> schema);

        abstract NbtTarget resolve(ServerTool.Context context, Arguments arguments);

        String getValue() {
            return value;
        }

        String getDescription() {
            return description;
        }
    }
}
