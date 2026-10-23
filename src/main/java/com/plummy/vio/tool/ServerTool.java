package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.argument.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public interface ServerTool extends Tool {

    ExecutionResult execute(Context context, Arguments arguments);

    default ToolType getType() {
        return ToolType.SERVER;
    }

    // Resolves arguments against the running server. Chunks are never loaded and positions always come from the model, never from where the player stands.
    class Context {

        private final MinecraftServer server;
        private final Requester requester;

        public Context(MinecraftServer server, Requester requester) {
            this.server = server;
            this.requester = requester;
        }

        public MinecraftServer getServer() {
            return server;
        }

        public AccessLevel getAccessLevel() {
            return requester.getAccessLevel();
        }

        public ServerPlayer getPlayer() {
            ServerPlayer player = server.getPlayerList().getPlayer(requester.getPlayerId());

            if (player == null) {
                throw ResultException.failure("The requesting player is no longer online.");
            }

            return player;
        }

        public ServerPlayer getPlayer(PlayerReference reference) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(reference.getName());

            if (player == null) {
                throw ResultException.invalidArguments("No online player named \"%s\". Use list_players to see who is online.".formatted(reference.getName()));
            }

            return player;
        }

        public Entity getEntity(EntityReference reference) {
            return switch (reference) {
                case PlayerReference player -> getPlayer(player);
                case UuidReference uuid -> getEntity(uuid);
                default -> throw new IllegalArgumentException("Unknown entity reference: " + reference.getClass().getSimpleName());
            };
        }

        private Entity getEntity(UuidReference reference) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(reference.getUuid());

                if (entity != null) {
                    return entity;
                }
            }

            throw ResultException.invalidArguments("No loaded entity with UUID %s. It may have died, despawned or be in an unloaded chunk; use find_entities to look up current UUIDs.".formatted(reference.getUuid()));
        }

        public ServerLevel getLevel(Dimension dimension) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension.getId()));

            if (level == null) {
                List<String> ids = StreamSupport.stream(server.getAllLevels().spliterator(), false).map(other -> other.dimension().location().toString()).toList();

                throw ResultException.invalidArguments("Unknown dimension \"%s\". Dimensions on this server: %s".formatted(dimension.getId(), ids));
            }

            return level;
        }

        // A loaded position inside the build height.
        public void requireLoaded(ServerLevel level, BlockPos pos) {
            if (level.isOutsideBuildHeight(pos)) {
                throw ResultException.invalidArguments("y %d is outside the build height of %s (%d to %d).".formatted(pos.getY(), level.dimension().location(), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1));
            }

            if (!level.isLoaded(pos)) {
                throw ResultException.failure("%d %d %d (%s) is in an unloaded chunk. Chunks are never loaded for tools; a player has to be near it.".formatted(pos.getX(), pos.getY(), pos.getZ(), level.dimension().location()));
            }
        }

        // The whole region inside the build height, with every chunk it touches loaded.
        public void requireLoaded(ServerLevel level, BoundingBox box) {
            if (level.isOutsideBuildHeight(box.minY()) || level.isOutsideBuildHeight(box.maxY())) {
                throw ResultException.invalidArguments("y %d to %d is outside the build height of %s (%d to %d).".formatted(box.minY(), box.maxY(), level.dimension().location(), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1));
            }

            long unloaded = box.intersectingChunks().filter(chunk -> !level.getChunkSource().hasChunk(chunk.x, chunk.z)).count();

            if (unloaded > 0) {
                throw ResultException.failure("The region touches %d unloaded chunk(s) in %s. Chunks are never loaded for tools; a player has to be near them.".formatted(unloaded, level.dimension().location()));
            }
        }

        public Registry<?> getRegistry(Identifier id) {
            Optional<Registry<Object>> registry = server.registryAccess().registry(ResourceKey.createRegistryKey(id.getId()));

            if (registry.isEmpty()) {
                List<String> ids = server.registryAccess().registries().map(entry -> entry.key().location().toString()).sorted().toList();

                throw ResultException.invalidArguments("Unknown registry \"%s\". Valid registries: %s".formatted(id, ids));
            }

            return registry.get();
        }

        public <T> Holder.Reference<T> getEntry(ResourceKey<? extends Registry<T>> registry, Identifier id, String kind) {
            return getEntry(server.registryAccess().registryOrThrow(registry), id, kind);
        }

        // Fails like Unknown block "minecraft:stone_brick" when the id doesn't exist.
        public <T> Holder.Reference<T> getEntry(Registry<T> registry, Identifier id, String kind) {
            if (id.isTag()) {
                throw ResultException.invalidArguments("Expected a %s id, got the tag %s.".formatted(kind, id));
            }

            Optional<Holder.Reference<T>> holder = registry.getHolder(id.getId());

            if (holder.isEmpty()) {
                throw ResultException.invalidArguments("Unknown %s \"%s\".".formatted(kind, id));
            }

            return holder.get();
        }
    }
}
