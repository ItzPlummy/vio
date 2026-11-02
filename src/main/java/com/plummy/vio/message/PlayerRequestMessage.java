package com.plummy.vio.message;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlayerRequestMessage implements Message {

    private final String request;
    private final PlayerLocation playerLocation;
    private final PlayerState playerState;
    private final PlayerInventory playerInventory;
    private final ServerInfo serverInfo;
    private final boolean isVoice;
    private final Instant dispatchTime;

    public PlayerRequestMessage(String request, PlayerLocation playerLocation, PlayerState playerState, PlayerInventory playerInventory, ServerInfo serverInfo, boolean isVoice) {
        this.request = request;
        this.playerLocation = playerLocation;
        this.playerState = playerState;
        this.playerInventory = playerInventory;
        this.serverInfo = serverInfo;
        this.isVoice = isVoice;
        this.dispatchTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public PlayerRequestMessage(String request, PlayerLocation playerLocation, PlayerState playerState, PlayerInventory playerInventory, ServerInfo serverInfo, boolean isVoice, Instant dispatchTime) {
        this.request = request;
        this.playerLocation = playerLocation;
        this.playerState = playerState;
        this.playerInventory = playerInventory;
        this.serverInfo = serverInfo;
        this.isVoice = isVoice;
        this.dispatchTime = dispatchTime;
    }

    public static PlayerRequestMessage of(String request, ServerPlayer player, MinecraftServer server, boolean isVoice) {
        return new PlayerRequestMessage(request, PlayerLocation.of(player), PlayerState.of(player), PlayerInventory.of(player), ServerInfo.of(server), isVoice);
    }

    public static PlayerRequestMessage of(String request, ServerPlayer player, MinecraftServer server, boolean isVoice, Instant dispatchTime) {
        return new PlayerRequestMessage(request, PlayerLocation.of(player), PlayerState.of(player), PlayerInventory.of(player), ServerInfo.of(server), isVoice, dispatchTime);
    }

    @Override
    public Instant getDispatchTime() {
        return dispatchTime;
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitPlayerRequestMessage(this);
    }

    public String getRequest() {
        return request;
    }

    public PlayerLocation getPlayerLocation() {
        return playerLocation;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public boolean isVoice() {
        return isVoice;
    }

    public record PlayerLocation(double x, double y, double z, double yaw, double pitch, String dimension, String biome, List<String> structures) {

        public static PlayerLocation of(ServerPlayer player) {
            Level level = player.level();
            BlockPos pos = player.blockPosition();

            if (!(level instanceof ServerLevel serverLevel)) {
                throw new IllegalStateException("Player level must be a ServerLevel");
            }

            ResourceLocation biome = serverLevel.registryAccess().registryOrThrow(Registries.BIOME).getKey(serverLevel.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value());

            if (biome == null) {
                throw new IllegalStateException("Player biome can't be null");
            }

            List<String> structures = new ArrayList<>();

            Map<Structure, ?> structuresAtPos = serverLevel.structureManager().getAllStructuresAt(pos);

            for (Structure structure : structuresAtPos.keySet()) {
                StructureStart start = serverLevel.structureManager().getStructureAt(pos, structure);

                if (!start.isValid() || !start.getBoundingBox().isInside(pos)) {
                    continue;
                }

                boolean strictlyInsidePiece = start.getPieces().stream().anyMatch(piece -> piece.getBoundingBox().isInside(pos));

                if (strictlyInsidePiece) {
                    ResourceLocation structureId = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);

                    if (structureId != null) {
                        structures.add(structureId.toString());
                    }
                }
            }

            return new PlayerLocation(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.level().dimension().location().toString(), biome.toString(), structures);
        }
    }

    public record PlayerState(double health, double food) {

        public static PlayerState of(ServerPlayer player) {
            return new PlayerState(player.getHealth(), player.getFoodData().getFoodLevel());
        }
    }

    public record PlayerInventory(String mainHandItem, String offHandItem) {

        public static PlayerInventory of(ServerPlayer player) {
            return new PlayerInventory(player.getMainHandItem().getItem().getName(player.getMainHandItem()).getString(), player.getOffhandItem().getItem().getName(player.getOffhandItem()).getString());
        }
    }

    public record ServerInfo(long time, long day) {

        public static ServerInfo of(MinecraftServer server) {
            long dayTime = server.overworld().getDayTime();

            return new ServerInfo(dayTime % 24000, dayTime / 24000 + 1);
        }
    }
}
