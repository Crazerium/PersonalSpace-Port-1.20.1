package me.eigenraven.personalspace.registry;

import com.mojang.serialization.Codec;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PersonalSpaceGridChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class PSChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, PersonalSpace.MODID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> PERSONAL_SPACE_GRID =
            CHUNK_GENERATORS.register(
                    "personal_space_grid",
                    () -> PersonalSpaceGridChunkGenerator.CODEC
            );

    private PSChunkGenerators() {
    }
}