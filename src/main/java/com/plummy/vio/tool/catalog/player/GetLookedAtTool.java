package com.plummy.vio.tool.catalog.player;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import com.plummy.vio.tool.value.catalog.BlockPosition;
import com.plummy.vio.tool.value.catalog.Position;
import com.plummy.vio.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class GetLookedAtTool implements ServerTool {

    private static final int MAX_DISTANCE = 256;

    @Override
    public String getName() {
        return "get_looked_at";
    }

    @Override
    public String getDescription() {
        return "What the requesting player is looking at: the block (with the face hit and place_pos, the block adjacent to that face) or the entity under their crosshair. Use it to resolve 'this', 'here' and 'that'. Uses the server-side view, which can trail the client by a tick.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("max_distance_blocks", new ArgumentDefinition(ArgumentType.INTEGER, "How far to look, 1 to 256; 64 is a good default."));
        schema.put("include_fluids", new ArgumentDefinition(ArgumentType.BOOLEAN, "Stop at water and lava surfaces instead of looking through them."));
        schema.put("include_entities", new ArgumentDefinition(ArgumentType.BOOLEAN, "Whether entities can be hit."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        int maxDistance = arguments.getInt("max_distance_blocks", 1, MAX_DISTANCE);
        boolean includeFluids = arguments.getBoolean("include_fluids");
        boolean includeEntities = arguments.getBoolean("include_entities");

        ServerPlayer player = context.getPlayer();
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(maxDistance));

        ClipContext clipContext = new ClipContext(eye, end, ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = clip(level, clipContext);
        Vec3 rayEnd = blockHit == null ? end : blockHit.getLocation();

        if (includeEntities) {
            AABB searchBox = player.getBoundingBox().expandTowards(rayEnd.subtract(eye)).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, rayEnd, searchBox, entity -> !entity.isSpectator() && entity.isPickable(), eye.distanceToSqr(rayEnd));

            if (entityHit != null) {
                Entity entity = entityHit.getEntity();
                JsonObject json = new JsonObject();

                json.addProperty("hit", "entity");
                json.addProperty("uuid", entity.getStringUUID());
                json.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                json.addProperty("name", entity instanceof Player || entity.hasCustomName() ? entity.getName().getString() : null);
                json.add("pos", Position.of(entity.position()).accept(ResultValueJsonSerializer.getInstance()));
                json.addProperty("distance_blocks", Math.round(eye.distanceTo(entityHit.getLocation()) * 10) / 10.0);

                return ExecutionResult.success(json);
            }
        }

        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) {
            JsonObject json = new JsonObject();

            json.addProperty("hit", "none");
            json.addProperty("max_distance_blocks", maxDistance);

            return ExecutionResult.success(json);
        }

        BlockPos pos = blockHit.getBlockPos();
        JsonObject json = new JsonObject();

        json.addProperty("hit", "block");
        json.add("pos", BlockPosition.of(pos).accept(ResultValueJsonSerializer.getInstance()));
        json.addProperty("face", blockHit.getDirection().getSerializedName());
        json.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        json.addProperty("distance_blocks", Math.round(eye.distanceTo(blockHit.getLocation()) * 10) / 10.0);
        json.add("place_pos", BlockPosition.of(pos.relative(blockHit.getDirection())).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }

    private static BlockHitResult clip(ServerLevel level, ClipContext context) {
        boolean[] unloaded = {false};

        BlockHitResult hit = BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (clipContext, pos) -> {
            if (level.isOutsideBuildHeight(pos)) {
                return null;
            }

            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
                unloaded[0] = true;
                return BlockHitResult.miss(context.getTo(), Direction.UP, pos);
            }

            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);
            VoxelShape blockShape = clipContext.getBlockShape(state, level, pos);
            BlockHitResult blockHit = level.clipWithInteractionOverride(clipContext.getFrom(), clipContext.getTo(), pos, blockShape, state);
            VoxelShape fluidShape = clipContext.getFluidShape(fluid, level, pos);
            BlockHitResult fluidHit = fluidShape.clip(clipContext.getFrom(), clipContext.getTo(), pos);
            double blockDistance = blockHit == null ? Double.MAX_VALUE : clipContext.getFrom().distanceToSqr(blockHit.getLocation());
            double fluidDistance = fluidHit == null ? Double.MAX_VALUE : clipContext.getFrom().distanceToSqr(fluidHit.getLocation());

            return blockDistance <= fluidDistance ? blockHit : fluidHit;
        }, clipContext -> BlockHitResult.miss(clipContext.getTo(), Direction.UP, BlockPos.containing(clipContext.getTo())));

        return unloaded[0] ? null : hit;
    }
}
