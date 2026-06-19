package me.eigenraven.personalspace.dimension;

import commoble.infiniverse.api.InfiniverseAPI;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class PSDimensions {
    private static final int DEFAULT_GROUND_LEVEL = 64;

    private PSDimensions() {
    }

    public static ResourceKey<Level> randomPersonalKey() {
        String id = "ps_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
        return key(id);
    }

    public static ResourceKey<Level> key(String idOrPath) {
        ResourceLocation location;

        if (idOrPath.contains(":")) {
            location = new ResourceLocation(idOrPath.toLowerCase(Locale.ROOT));
        } else {
            location = new ResourceLocation(PersonalSpace.MODID, idOrPath.toLowerCase(Locale.ROOT));
        }

        return ResourceKey.create(Registries.DIMENSION, location);
    }

    public static ServerLevel getOrCreate(MinecraftServer server, ResourceKey<Level> levelKey) {
        ServerLevel level = InfiniverseAPI.get().getOrCreateLevel(
                server,
                levelKey,
                () -> createStem(server, PersonalSpaceData.WorldType.VOID, DEFAULT_GROUND_LEVEL)
        );

        applyStoredSettings(level);

        return level;
    }

    public static ServerLevel createPersonalDimension(
            MinecraftServer server,
            ResourceKey<Level> levelKey,
            PersonalSpaceData.WorldType type,
            int groundLevel
    ) {
        PersonalSpaceData.WorldType safeType = type == null
                ? PersonalSpaceData.WorldType.VOID
                : type;

        int safeGroundLevel = clampGroundLevel(server.overworld(), groundLevel);

        ServerLevel newLevel = InfiniverseAPI.get().getOrCreateLevel(
                server,
                levelKey,
                () -> createStem(server, safeType, safeGroundLevel)
        );

        PersonalSpaceData data = PersonalSpaceData.load(newLevel);
        data.setType(safeType);
        data.setGroundLevel(safeGroundLevel);
        PersonalSpaceData.save(newLevel, data);

        newLevel.setDayTime(data.getTimeOfDay());

        return newLevel;
    }

    private static LevelStem createStem(
            MinecraftServer server,
            PersonalSpaceData.WorldType type,
            int groundLevel
    ) {
        PersonalSpaceData.WorldType safeType = type == null
                ? PersonalSpaceData.WorldType.VOID
                : type;

        ServerLevel overworld = server.overworld();

        Holder<DimensionType> dimensionType = overworld.dimensionTypeRegistration();

        Holder<Biome> biome = server.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.PLAINS);

        List<FlatLayerInfo> layers = new ArrayList<>();

        if (safeType == PersonalSpaceData.WorldType.FLAT) {
            int minY = overworld.getMinBuildHeight();
            int dirtThickness = Math.max(0, groundLevel - minY);

            if (dirtThickness > 0) {
                layers.add(new FlatLayerInfo(dirtThickness, Blocks.DIRT));
            }

            layers.add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));
        }
        Optional<HolderSet<StructureSet>> noStructures = Optional.of(
                HolderSet.direct(List.<Holder<StructureSet>>of())
        );
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                noStructures,
                biome,
                List.<Holder<PlacedFeature>>of()
        ).withBiomeAndLayers(
                layers,
                noStructures,
                biome
        );

        return new LevelStem(dimensionType, new FlatLevelSource(settings));
    }

    public static int clampGroundLevel(ServerLevel level, int y) {
        int min = level.getMinBuildHeight();
        int max = level.getMaxBuildHeight() - 4;

        if (max < min) {
            return y;
        }

        return Math.max(min, Math.min(max, y));
    }

    public static void prepareSpawnArea(
            ServerLevel level,
            PersonalSpaceData.WorldType type,
            int groundLevel,
            BlockPos portalPos
    ) {
        PersonalSpaceData.WorldType safeType = type == null
                ? PersonalSpaceData.WorldType.VOID
                : type;

        PersonalSpaceData data = PersonalSpaceData.load(level);
        List<PersonalSpaceLayerParser.Layer> presetLayers =
                PersonalSpaceLayerParser.parse(data.getLayersPreset());

        int floorY = portalPos.getY() - 1;

        level.getChunkAt(portalPos);

        if (safeType == PersonalSpaceData.WorldType.VOID) {
            BlockState floorState = Blocks.OBSIDIAN.defaultBlockState();

            if (!presetLayers.isEmpty()) {
                floorState = presetLayers.get(presetLayers.size() - 1).state();
            }

            for (int x = portalPos.getX() - 3; x <= portalPos.getX() + 3; x++) {
                for (int z = portalPos.getZ() - 3; z <= portalPos.getZ() + 3; z++) {
                    BlockPos floor = new BlockPos(x, floorY, z);

                    level.setBlock(floor, floorState, 3);

                    for (int dy = 1; dy <= 4; dy++) {
                        level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }

            return;
        }

        if (safeType == PersonalSpaceData.WorldType.FLAT) {
            applyPresetLayersAroundPortal(
                    level,
                    portalPos,
                    presetLayers
            );

            for (int x = portalPos.getX() - 2; x <= portalPos.getX() + 2; x++) {
                for (int z = portalPos.getZ() - 2; z <= portalPos.getZ() + 2; z++) {
                    for (int y = portalPos.getY(); y <= portalPos.getY() + 3; y++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    private static void applyPresetLayersAroundPortal(
            ServerLevel level,
            BlockPos portalPos,
            List<PersonalSpaceLayerParser.Layer> layers
    ) {
        if (layers.isEmpty()) {
            return;
        }

        int minY = level.getMinBuildHeight();
        int floorY = portalPos.getY() - 1;
        int radius = 16;

        for (int x = portalPos.getX() - radius; x <= portalPos.getX() + radius; x++) {
            for (int z = portalPos.getZ() - radius; z <= portalPos.getZ() + radius; z++) {
                int y = minY;

                for (PersonalSpaceLayerParser.Layer layer : layers) {
                    for (int i = 0; i < layer.count(); i++) {
                        if (y > floorY) {
                            break;
                        }

                        level.setBlock(
                                new BlockPos(x, y, z),
                                layer.state(),
                                3
                        );

                        y++;
                    }

                    if (y > floorY) {
                        break;
                    }
                }

                while (y <= floorY) {
                    BlockState topState = layers.get(layers.size() - 1).state();

                    level.setBlock(
                            new BlockPos(x, y, z),
                            topState,
                            3
                    );

                    y++;
                }
            }
        }
    }
    private static void applyStoredSettings(ServerLevel level) {
        if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        level.setDayTime(data.getTimeOfDay());
    }
}