package me.eigenraven.personalspace.registry;

import com.mojang.serialization.MapCodec;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PersonalSpaceGridChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PSChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, PersonalSpace.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> PERSONAL_SPACE_GRID =
            CHUNK_GENERATORS.register(
                    "personal_space_grid",
                    () -> PersonalSpaceGridChunkGenerator.CODEC
            );

    private PSChunkGenerators() {
    }
}