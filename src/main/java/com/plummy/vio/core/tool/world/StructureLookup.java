package com.plummy.vio.core.tool.world;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Structures containing a position, read only from loaded chunks. StructureManager would load the chunks holding structure starts, which tools never do.
final class StructureLookup {

    private StructureLookup() {
    }

    // The position's chunk must be loaded; starts in unloaded chunks are counted in unloadedStarts instead.
    static Result getStructuresAt(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        List<Found> found = new ArrayList<>();
        int unloadedStarts = 0;

        if (chunk == null) {
            return new Result(found, 0);
        }

        for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
            Structure structure = entry.getKey();

            for (long reference : entry.getValue()) {
                ChunkPos startChunk = new ChunkPos(reference);
                LevelChunk startAccess = level.getChunkSource().getChunkNow(startChunk.x, startChunk.z);

                if (startAccess == null) {
                    unloadedStarts++;
                    continue;
                }

                StructureStart start = startAccess.getStartForStructure(structure);

                if (start == null || !start.isValid() || !start.getBoundingBox().isInside(pos)) {
                    continue;
                }

                boolean insidePiece = false;

                for (StructurePiece piece : start.getPieces()) {
                    if (piece.getBoundingBox().isInside(pos)) {
                        insidePiece = true;
                        break;
                    }
                }

                found.add(new Found(level.registryAccess().registryOrThrow(Registries.STRUCTURE).getResourceKey(structure).map(key -> key.location().toString()).orElse("unknown"), structure, start, insidePiece));
            }
        }

        found.sort(Comparator.comparing(Found::id));

        return new Result(found, unloadedStarts);
    }

    record Found(String id, Structure structure, StructureStart start, boolean insidePiece) {
    }

    record Result(List<Found> structures, int unloadedStarts) {
    }
}
