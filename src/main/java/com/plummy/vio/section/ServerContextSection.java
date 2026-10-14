package com.plummy.vio.section;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class ServerContextSection implements Section {

    private final List<String> dimensions;
    private final long time;

    public ServerContextSection(List<String> dimensions, long time) {
        this.dimensions = List.copyOf(dimensions);
        this.time = time;
    }

    public static ServerContextSection fromServer(MinecraftServer server) {
        List<String> dimensions = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            dimensions.add(level.dimension().location().toString());
        }

        return new ServerContextSection(dimensions, server.overworld().getDayTime());
    }

    @Override
    public SectionType getType() {
        return SectionType.SERVER_CONTEXT;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitServerContextSection(this);
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public long getTime() {
        return time;
    }
}
