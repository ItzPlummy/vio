package com.plummy.vio.core.tool.catalog.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.core.tool.value.argument.ArgumentType;
import com.plummy.vio.core.tool.value.argument.Arguments;
import com.plummy.vio.core.tool.value.catalog.BlockPosition;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.stream.Stream;

public class GetBlockInfoTool implements ServerTool {

    @Override
    public String getName() {
        return "get_block_info";
    }

    @Override
    public String getDescription() {
        return "Full description of one placed block: state, fluid, light, redstone power received, biome, hardness, tools, and a block entity summary (sign text, container slot count).";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("position", new ArgumentDefinition(ArgumentType.BLOCK_POSITION, "Block position [x, y, z]."));
        schema.put("dimension", new ArgumentDefinition(ArgumentType.DIMENSION, "Dimension id, e.g. minecraft:overworld."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        ServerLevel level = context.getLevel(arguments.getDimension("dimension"));

        BlockPos pos = arguments.getBlockPosition("position").toBlockPos();
        context.requireLoaded(level, pos);

        BlockState state = level.getBlockState(pos);
        FluidState fluid = level.getFluidState(pos);

        JsonObject properties = new JsonObject();

        for (Property<?> property : state.getProperties()) {
            properties.addProperty(property.getName(), Util.getPropertyName(property, state.getValue(property)));
        }

        JsonObject light = new JsonObject();
        light.addProperty("block", level.getBrightness(LightLayer.BLOCK, pos));
        light.addProperty("sky", level.getBrightness(LightLayer.SKY, pos));

        BlockEntity blockEntity = level.getBlockEntity(pos);
        JsonObject blockEntityJson = null;

        if (blockEntity != null) {
            blockEntityJson = new JsonObject();
            blockEntityJson.addProperty("type", String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType())));

            if (blockEntity instanceof SignBlockEntity sign) {
                blockEntityJson.add("front_text", Arrays.stream(sign.getFrontText().getMessages(false)).map(Component::getString).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
                blockEntityJson.add("back_text", Arrays.stream(sign.getBackText().getMessages(false)).map(Component::getString).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            } else if (blockEntity instanceof Container container) {
                blockEntityJson.addProperty("slots", container.getContainerSize());
                blockEntityJson.addProperty("contents", "use get_container_contents");
            }
        }

        JsonObject json = new JsonObject();

        json.add("pos", BlockPosition.of(pos).accept(ResultValueJsonSerializer.getInstance()));
        json.addProperty("dimension", level.dimension().location().toString());
        json.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        json.add("state", properties);
        json.addProperty("fluid", fluid.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(fluid.getType()).toString());
        json.add("light", light);
        json.addProperty("redstone_power", level.getBestNeighborSignal(pos));
        json.addProperty("biome", level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("unknown"));
        json.addProperty("hardness", state.getDestroySpeed(level, pos));
        json.add("mineable_with", Stream.of("axe", "hoe", "pickaxe", "shovel")
                .filter(tool -> state.is(TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("mineable/%s".formatted(tool)))))
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.add("block_entity", blockEntityJson);

        return ExecutionResult.success(json);
    }
}
