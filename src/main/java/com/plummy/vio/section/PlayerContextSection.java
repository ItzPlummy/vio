package com.plummy.vio.section;

import com.plummy.vio.access.AccessLevel;
import net.minecraft.server.level.ServerPlayer;

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

    public PlayerContextSection(UUID id, String name, AccessLevel accessLevel, double x, double y, double z, double yaw, double pitch) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.accessLevel = accessLevel;
    }

    public static PlayerContextSection fromPlayer(ServerPlayer player, AccessLevel accessLevel) {
        return new PlayerContextSection(player.getUUID(), player.getName().getString(), accessLevel, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
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

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }
}
