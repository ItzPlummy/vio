package com.plummy.vio.section;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerContextSection implements Section {

    private final String[] dimensions;

    private final long time;
    private final long day;

    public ServerContextSection(
            String[] dimensions,
            long time,
            long day
    ) {
        this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
        this.time = time;
        this.day = day;
    }

    public static ServerContextSection fromServer(MinecraftServer server) {
        List<String> dimensions = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            dimensions.add(level.dimension().location().toString());
        }

        return new ServerContextSection(
                dimensions.toArray(new String[0]),
                server.overworld().getDayTime() % 24000,
                server.overworld().getDayTime() / 24000
        );
    }

    @Override
    public SectionType getType() {
        return SectionType.SERVER_CONTEXT;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitServerContextSection(this);
    }

    public String[] getDimensions() {
        return dimensions;
    }

    public long getTime() {
        return time;
    }

    public long getDay() {
        return day;
    }
}
