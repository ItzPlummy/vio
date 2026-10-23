package com.plummy.vio.section;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerContextSection implements Section {

    private final UUID id;
    private final String name;
    private final AccessLevel accessLevel;

    private final double x;
    private final double y;
    private final double z;
    private final double yaw;
    private final double pitch;
    private final String dimension;
    private final String biome;
    private final String[] structures;

    private final double health;
    private final double armor;
    private final double food;
    private final double saturation;
    private final int level;

    private final String gameMode;

    private final String mainHandItem;
    private final String offHandItem;

    public PlayerContextSection(
            UUID id,
            String name,
            AccessLevel accessLevel,
            double x,
            double y,
            double z,
            double yaw,
            double pitch,
            String dimension,
            String biome,
            String[] structures,
            double health,
            double armor,
            double food,
            double saturation,
            int level,
            String gameMode,
            String mainHandItem,
            String offHandItem
    ) {
        this.id = id;
        this.name = name;
        this.accessLevel = accessLevel;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
        this.biome = biome;
        this.structures = structures;
        this.health = health;
        this.armor = armor;
        this.food = food;
        this.saturation = saturation;
        this.level = level;
        this.gameMode = gameMode;
        this.mainHandItem = mainHandItem;
        this.offHandItem = offHandItem;
    }

    public static PlayerContextSection fromPlayer(ServerPlayer player, AccessLevel accessLevel) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Player level must be a ServerLevel");
        }

        ResourceLocation biome = serverLevel.registryAccess().registryOrThrow(Registries.BIOME).getKey(serverLevel.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value());

        if (biome == null) {
            throw new IllegalStateException("Player biome cannot be null");
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

        return new PlayerContextSection(
                player.getUUID(),
                player.getName().getString(),
                accessLevel,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.level().dimension().location().toString(),
                biome.toString(),
                structures.toArray(new String[0]),
                player.getHealth(),
                player.getArmorValue(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.experienceLevel,
                player.gameMode.getGameModeForPlayer().getLongDisplayName().getString(),
                player.getMainHandItem().getItem().getName(player.getMainHandItem()).getString(),
                player.getOffhandItem().getItem().getName(player.getOffhandItem()).getString()
        );
    }

    @Override
    public SectionType getType() {
        return SectionType.PLAYER_CONTEXT;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitPlayerContextSection(this);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public String getDimension() {
        return dimension;
    }

    public String getBiome() {
        return biome;
    }

    public String[] getStructures() {
        return structures;
    }

    public double getHealth() {
        return health;
    }

    public double getArmor() {
        return armor;
    }

    public double getFood() {
        return food;
    }

    public double getSaturation() {
        return saturation;
    }

    public int getLevel() {
        return level;
    }

    public String getGameMode() {
        return gameMode;
    }

    public String getMainHandItem() {
        return mainHandItem;
    }

    public String getOffHandItem() {
        return offHandItem;
    }
}
