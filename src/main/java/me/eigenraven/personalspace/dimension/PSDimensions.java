package me.eigenraven.personalspace.dimension;

import commoble.infiniverse.api.InfiniverseAPI;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.world.PersonalSpaceChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;
import java.util.UUID;

public final class PSDimensions {
    private PSDimensions() {}

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
                () -> createInitialStem(server) // используем стандартный генератор
        );
    }

    // Используем стандартный генератор (копируем из обычного мира)
    private static LevelStem createInitialStem(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        Holder<DimensionType> dimensionType = overworld.dimensionTypeRegistration();
        // Берём генератор из обычного мира и копируем (без изменений)
        ChunkGenerator generator = overworld.getChunkSource().getGenerator();
        return new LevelStem(dimensionType, generator);
    }

    // Создаём персональный мир с настройками (тип и высота)
    public static ServerLevel createPersonalDimension(
            MinecraftServer server,
            ResourceKey<Level> levelKey,
            PersonalSpaceData.WorldType type,
            int height
    ) {
        // Создаём мир через стандартный генератор
        ServerLevel newLevel = InfiniverseAPI.get().getOrCreateLevel(
                server,
                levelKey,
                () -> createInitialStem(server)
        );

        // Сохраняем настройки в JSON
        PersonalSpaceData data = new PersonalSpaceData();
        data.setType(type);
        data.setGroundLevel(height);
        PersonalSpaceData.save(newLevel, data);

        return newLevel;
    }

    // Обработчик события загрузки чанка — ЗДЕСЬ МЫ ЗАМЕНЯЕМ ГЕНЕРАЦИЮ
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
                // Вызываем нашу статическую генерацию
                PersonalSpaceChunkGenerator.generateChunk(level, event.getChunk());
            }
        }
    }
}