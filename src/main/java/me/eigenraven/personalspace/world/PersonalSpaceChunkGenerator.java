package me.eigenraven.personalspace.world;

import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

public class PersonalSpaceChunkGenerator {


    public static void generateChunk(ServerLevel level, ChunkAccess chunk) {
        PersonalSpaceData data = PersonalSpaceData.load(level);
        PersonalSpaceData.WorldType type = data.getType();
        int groundLevel = data.getGroundLevel();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;


        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }
            }
        }


        if (chunkX == 0 && chunkZ == 0) {
            if (type == PersonalSpaceData.WorldType.FLAT) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        chunk.setBlockState(new BlockPos(x, groundLevel, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                    }
                }
            } else if (type == PersonalSpaceData.WorldType.VOID) {
                for (int x = 5; x <= 10; x++) {
                    for (int z = 5; z <= 10; z++) {
                        chunk.setBlockState(new BlockPos(x, groundLevel, z), Blocks.OBSIDIAN.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}