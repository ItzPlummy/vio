package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.executor.ExecutionResultException;
import com.plummy.vio.tool.requester.PlayerToolRequester;
import com.plummy.vio.tool.requester.ToolRequester;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.*;
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
import java.util.UUID;
import java.util.stream.StreamSupport;

public interface ServerTool extends Tool {

    ExecutionResult execute(Context context, Arguments arguments);

    class Context {

        private final MinecraftServer server;
        private final ToolRequester requester;

        public Context(MinecraftServer server, ToolRequester requester) {
            this.server = server;
            this.requester = requester;
        }

        public MinecraftServer getServer() {
            return server;
        }

        public ToolRequester getToolRequester() {
            return requester;
        }

        public AccessLevel getAccessLevel() {
            return requester.getAccessLevel();
        }

        public UUID getPlayerId() {
            if (!(requester instanceof PlayerToolRequester player)) {
                throw ExecutionResultException.failure("This tool needs a requesting player");
            }

            return player.getPlayerId();
        }

        public ServerPlayer getPlayer() {
            ServerPlayer player = server.getPlayerList().getPlayer(getPlayerId());

            if (player == null) {
                throw ExecutionResultException.failure("The requesting player is no longer online");
            }

            return player;
        }

        public Entity getEntity(EntityReference reference) {
            return switch (reference) {
                case PlayerReference player -> getPlayer(player);
                case UuidReference uuid -> getEntity(uuid);
                default -> throw new IllegalArgumentException("Unknown entity reference: %s".formatted(reference.getClass().getSimpleName()));
            };
        }

        public ServerPlayer getPlayer(PlayerReference reference) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(reference.getName());

            if (player == null) {
                throw ExecutionResultException.invalidArguments("No online player named '%s'. Use list_players to see who is online".formatted(reference.getName()));
            }

            return player;
        }

        private Entity getEntity(UuidReference reference) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(reference.getUuid());

                if (entity != null) {
                    return entity;
                }
            }

            throw ExecutionResultException.invalidArguments("No loaded entity with UUID %s. It may have died, despawned or be in an unloaded chunk; use find_entities to look up current UUIDs".formatted(reference.getUuid()));
        }

        public ServerLevel getLevel(Dimension dimension) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension.getId()));

            if (level == null) {
                List<String> ids = StreamSupport.stream(server.getAllLevels().spliterator(), false).map(other -> other.dimension().location().toString()).toList();

                throw ExecutionResultException.invalidArguments("Unknown dimension %s. Dimensions on this server: %s".formatted(dimension.getId(), ids));
            }

            return level;
        }

        public void requireLoaded(ServerLevel level, BlockPos pos) {
            if (level.isOutsideBuildHeight(pos)) {
                throw ExecutionResultException.invalidArguments("y %s is outside the build height of %s (%s to %s)".formatted(pos.getY(), level.dimension().location(), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1));
            }

            if (!level.isLoaded(pos)) {
                throw ExecutionResultException.failure("%s %s %s (%s) is in an unloaded chunk. Chunks are never loaded for tools; a player has to be near it".formatted(pos.getX(), pos.getY(), pos.getZ(), level.dimension().location()));
            }
        }

        public void requireLoaded(ServerLevel level, BoundingBox box) {
            if (level.isOutsideBuildHeight(box.minY()) || level.isOutsideBuildHeight(box.maxY())) {
                throw ExecutionResultException.invalidArguments("y %s to %s is outside the build height of %s (%s to %s)".formatted(box.minY(), box.maxY(), level.dimension().location(), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1));
            }

            long unloaded = box.intersectingChunks().filter(chunk -> !level.getChunkSource().hasChunk(chunk.x, chunk.z)).count();

            if (unloaded > 0) {
                throw ExecutionResultException.failure("The region touches %s unloaded chunk(s) in %s. Chunks are never loaded for tools; a player has to be near them".formatted(unloaded, level.dimension().location()));
            }
        }

        public Registry<?> getRegistry(Identifier id) {
            Optional<Registry<Object>> registry = server.registryAccess().registry(ResourceKey.createRegistryKey(id.getId()));

            if (registry.isEmpty()) {
                List<String> ids = server.registryAccess().registries().map(entry -> entry.key().location().toString()).sorted().toList();

                throw ExecutionResultException.invalidArguments("Unknown registry %s. Valid registries: %s".formatted(id, ids));
            }

            return registry.get();
        }

        public <T> Holder.Reference<T> getEntry(ResourceKey<? extends Registry<T>> registry, Identifier id, String kind) {
            return getEntry(server.registryAccess().registryOrThrow(registry), id, kind);
        }

        public <T> Holder.Reference<T> getEntry(Registry<T> registry, Identifier id, String kind) {
            if (id.isTag()) {
                throw ExecutionResultException.invalidArguments("Expected a %s id, got the tag %s".formatted(kind, id));
            }

            Optional<Holder.Reference<T>> holder = registry.getHolder(id.getId());

            if (holder.isEmpty()) {
                throw ExecutionResultException.invalidArguments("Unknown %s %s".formatted(kind, id));
            }

            return holder.get();
        }
    }
}
