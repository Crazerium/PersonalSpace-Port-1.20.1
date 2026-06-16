package me.eigenraven.personalspace.dimension;

import commoble.infiniverse.api.InfiniverseAPI;
import com.mojang.serialization.DynamicOps;
import me.eigenraven.personalspace.personalspace.PersonalSpace;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Locale;
import java.util.UUID;

public final class PSDimensions {
    private PSDimensions() {
    }

    public static ResourceKey<Level> randomPersonalKey() {
        String path = "ps_" + UUID.randomUUID().toString().replace("-", "");
        return key(path);
    }

    public static ResourceKey<Level> key(String idOrPath) {
        String normalized = idOrPath.toLowerCase(Locale.ROOT);

        ResourceLocation id = normalized.contains(":")
                ? new ResourceLocation(normalized)
                : new ResourceLocation(PersonalSpace.MODID, normalized);

        return ResourceKey.create(Registries.DIMENSION, id);
    }

    public static ServerLevel getOrCreate(MinecraftServer server, ResourceKey<Level> levelKey) {
        return InfiniverseAPI.get().getOrCreateLevel(
                server,
                levelKey,
                () -> createInitialStem(server)
        );
    }

    private static LevelStem createInitialStem(MinecraftServer server) {
        ServerLevel overworld = server.overworld();

        Holder<DimensionType> dimensionType = overworld.dimensionTypeRegistration();
        ChunkGenerator chunkGenerator = copyChunkGenerator(server, overworld.getChunkSource().getGenerator());

        return new LevelStem(dimensionType, chunkGenerator);
    }

    private static ChunkGenerator copyChunkGenerator(MinecraftServer server, ChunkGenerator oldGenerator) {
        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());

        Tag generatorTag = ChunkGenerator.CODEC.encodeStart(ops, oldGenerator)
                .getOrThrow(false, msg -> new RuntimeException("Failed to decode chunk generator: " + msg));

        return ChunkGenerator.CODEC.parse(ops, generatorTag)
                .getOrThrow(false, msg -> new RuntimeException("Failed to decode chunk generator: " + msg));
    }
}