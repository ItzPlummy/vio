package com.plummy.vio.core.tool.catalog.server;

import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.Arguments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GetServerStatusTool implements ServerTool {

    private static final double NANOS_PER_MILLI = 1_000_000.0;

    @Override
    public String getName() {
        return "get_server_status";
    }

    @Override
    public String getDescription() {
        return "Overall server health in one call: TPS, tick times (average, 95th percentile, per dimension), tick rate and freeze state, memory, CPU load, uptime, players online, loaded chunks and entities per dimension.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        MinecraftServer server = context.getServer();
        float tickRate = server.tickRateManager().tickrate();
        double msptAverage = server.getAverageTickTimeNanos() / NANOS_PER_MILLI;
        long[] samples = server.getTickTimesNanos().clone();

        Arrays.sort(samples);

        double msptP95 = samples.length == 0 ? 0.0 : samples[Math.min(samples.length - 1, (int) Math.ceil(samples.length * 0.95) - 1)] / NANOS_PER_MILLI;
        double tps = msptAverage <= 0.0 ? tickRate : Math.min(tickRate, 1000.0 / msptAverage);

        JsonObject msptByDimension = new JsonObject();
        JsonObject loadedChunks = new JsonObject();
        JsonObject entities = new JsonObject();

        for (ServerLevel level : server.getAllLevels()) {
            String dimension = level.dimension().location().toString();
            long[] times = server.getTickTime(level.dimension());

            msptByDimension.addProperty(dimension, times == null ? 0.0 : Math.round(Arrays.stream(times).average().orElse(0.0) / NANOS_PER_MILLI * 10) / 10.0);
            loadedChunks.addProperty(dimension, level.getChunkSource().getLoadedChunksCount());

            int count = 0;
            for (Entity ignored : level.getAllEntities()) {
                count++;
            }

            entities.addProperty(dimension, count);
        }

        Runtime runtime = Runtime.getRuntime();
        JsonObject memory = new JsonObject();
        memory.addProperty("used", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.addProperty("max", runtime.maxMemory() / (1024 * 1024));

        JsonObject cpu = new JsonObject();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            cpu.addProperty("process", Math.round(Math.max(0.0, sunOs.getProcessCpuLoad()) * 100));
            cpu.addProperty("system", Math.round(Math.max(0.0, sunOs.getCpuLoad()) * 100));
        } else {
            cpu.add("process", null);
            cpu.add("system", null);
        }

        JsonObject json = new JsonObject();

        json.addProperty("tps", Math.round(tps * 10) / 10.0);
        json.addProperty("mspt_avg", Math.round(msptAverage * 10) / 10.0);
        json.addProperty("mspt_p95", Math.round(msptP95 * 10) / 10.0);
        json.add("mspt_by_dimension", msptByDimension);
        json.addProperty("tick_rate", tickRate);
        json.addProperty("frozen", server.tickRateManager().isFrozen());
        json.add("memory_mb", memory);
        json.add("cpu_percent", cpu);
        json.addProperty("uptime_seconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        json.addProperty("players_online", server.getPlayerCount());
        json.add("loaded_chunks", loadedChunks);
        json.add("entities", entities);

        return ExecutionResult.success(json);
    }
}
