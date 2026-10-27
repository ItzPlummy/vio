package com.plummy.vio.message;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.script.ScriptState;
import com.plummy.vio.script.ScriptType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerRequestMessage extends AbstractMessage {

    private final String request;
    private final PlayerInfo playerInfo;
    private final PlayerLocation playerLocation;
    private final PlayerState playerState;
    private final PlayerInventory playerInventory;
    private final ServerInfo serverInfo;
    private final ScriptInfo scriptInfo;
    private final boolean isVoice;

    public PlayerRequestMessage(String request, PlayerInfo playerInfo, PlayerLocation playerLocation, PlayerState playerState, PlayerInventory playerInventory, ServerInfo serverInfo, ScriptInfo scriptInfo, boolean isVoice) {
        this.request = request;
        this.playerInfo = playerInfo;
        this.playerLocation = playerLocation;
        this.playerState = playerState;
        this.playerInventory = playerInventory;
        this.serverInfo = serverInfo;
        this.scriptInfo = scriptInfo;
        this.isVoice = isVoice;
    }

    public PlayerRequestMessage(String request, PlayerInfo playerInfo, PlayerLocation playerLocation, PlayerState playerState, PlayerInventory playerInventory, ServerInfo serverInfo, ScriptInfo scriptInfo, boolean isVoice, Instant dispatchTime) {
        super(dispatchTime);

        this.request = request;
        this.playerInfo = playerInfo;
        this.playerLocation = playerLocation;
        this.playerState = playerState;
        this.playerInventory = playerInventory;
        this.serverInfo = serverInfo;
        this.scriptInfo = scriptInfo;
        this.isVoice = isVoice;
    }

    public static PlayerRequestMessage of(String request, ServerPlayer player, MinecraftServer server, AccessLevel accessLevel, ScriptInfo scriptInfo, boolean isVoice) {
        return new PlayerRequestMessage(request, PlayerInfo.of(player, accessLevel), PlayerLocation.of(player), PlayerState.of(player), PlayerInventory.of(player), ServerInfo.of(server), scriptInfo, isVoice);
    }

    public static PlayerRequestMessage of(String request, ServerPlayer player, MinecraftServer server, AccessLevel accessLevel, ScriptInfo scriptInfo, boolean isVoice, Instant dispatchTime) {
        return new PlayerRequestMessage(request, PlayerInfo.of(player, accessLevel), PlayerLocation.of(player), PlayerState.of(player), PlayerInventory.of(player), ServerInfo.of(server), scriptInfo, isVoice, dispatchTime);
    }

    @Override
    public <T> T accept(MessageVisitor<T> visitor) {
        return visitor.visitPlayerRequestMessage(this);
    }

    public String getRequest() {
        return request;
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
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

    public ScriptInfo getScriptInfo() {
        return scriptInfo;
    }

    public boolean isVoice() {
        return isVoice;
    }

    public record PlayerInfo(UUID id, String name, AccessLevel accessLevel) {

        public static PlayerInfo of(ServerPlayer player, AccessLevel accessLevel) {
            return new PlayerInfo(player.getUUID(), player.getName().getString(), accessLevel);
        }
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

    public record PlayerState(double health, double armor, double food, double saturation, int level, String gameMode) {

        public static PlayerState of(ServerPlayer player) {
            return new PlayerState(player.getHealth(), player.getArmorValue(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(), player.experienceLevel, player.gameMode.getGameModeForPlayer().getLongDisplayName().getString());
        }
    }

    public record PlayerInventory(String mainHandItem, String offHandItem) {

        public static PlayerInventory of(ServerPlayer player) {
            return new PlayerInventory(player.getMainHandItem().getItem().getName(player.getMainHandItem()).getString(), player.getOffhandItem().getItem().getName(player.getOffhandItem()).getString());
        }
    }

    public record ServerInfo(List<String> dimensions, long time, long day) {

        public static ServerInfo of(MinecraftServer server) {
            List<String> dimensions = new ArrayList<>();

            for (ServerLevel level : server.getAllLevels()) {
                dimensions.add(level.dimension().location().toString());
            }

            long dayTime = server.overworld().getDayTime();

            return new ServerInfo(dimensions, dayTime % 24000, dayTime / 24000 + 1);
        }
    }

    public record ScriptInfo(List<Entry> scripts) {

        public static ScriptInfo of(ScriptManager manager) {
            List<Entry> scripts = new ArrayList<>();

            for (Script script : manager.getAll()) {
                scripts.add(new Entry(script.getName(), script.getType(), script.getState(), script.getDescription()));
            }

            return new ScriptInfo(scripts);
        }

        public record Entry(String name, ScriptType type, ScriptState state, String description) {
        }
    }
}
