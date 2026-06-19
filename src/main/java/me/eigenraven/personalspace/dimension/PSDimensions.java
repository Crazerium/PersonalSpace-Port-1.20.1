package me.eigenraven.personalspace.dimension;

import commoble.infiniverse.api.InfiniverseAPI;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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

            applyBoundaryRoadsAndMarker(level, portalPos, data);

            return;
        }

        if (safeType == PersonalSpaceData.WorldType.FLAT) {
            applyPresetLayersAroundPortal(
                    level,
                    portalPos,
                    presetLayers
            );

            applyBoundaryRoadsAndMarker(level, portalPos, data);
            applyVegetation(level, portalPos, data);
            clearPortalSpace(level, portalPos);
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
    private static void applyBoundaryRoadsAndMarker(
            ServerLevel level,
            BlockPos portalPos,
            PersonalSpaceData data
    ) {
        int boundaryChunksX = data.getBoundaryChunksX();
        int boundaryChunksZ = data.getBoundaryChunksZ();
        int gapChunks = data.getGapChunks();

        if (boundaryChunksX <= 0 && boundaryChunksZ <= 0 && gapChunks <= 0 && !data.isCenterMarkerEnabled()) {
            return;
        }

        int centerX = portalPos.getX();
        int centerZ = portalPos.getZ();
        int floorY = portalPos.getY() - 1;

        int radiusX = Math.max(1, boundaryChunksX) * 16;
        int radiusZ = Math.max(1, boundaryChunksZ) * 16;

        int minX = centerX - radiusX;
        int maxX = centerX + radiusX;
        int minZ = centerZ - radiusZ;
        int maxZ = centerZ + radiusZ;

        int roadHalfWidth = Math.max(0, gapChunks * 8);

        BlockState boundaryState = blockStateFromId(
                data.getBoundaryBlock(),
                Blocks.YELLOW_CONCRETE.defaultBlockState()
        );

        BlockState roadState = blockStateFromId(
                data.getRoadBlock(),
                Blocks.BLACK_CONCRETE.defaultBlockState()
        );

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isBoundary =
                        x == minX || x == maxX ||
                                z == minZ || z == maxZ;

                boolean isRoad =
                        roadHalfWidth > 0 &&
                                (Math.abs(x - centerX) <= roadHalfWidth ||
                                        Math.abs(z - centerZ) <= roadHalfWidth);

                if (!isBoundary && !isRoad) {
                    continue;
                }

                BlockState state = isBoundary ? boundaryState : roadState;

                level.setBlock(
                        new BlockPos(x, floorY, z),
                        state,
                        3
                );
            }
        }

        if (data.isCenterMarkerEnabled()) {
            BlockState markerState = blockStateFromId(
                    data.getCenterMarkerBlock(),
                    Blocks.BEACON.defaultBlockState()
            );

            if (!markerState.isAir()) {
                level.setBlock(
                        new BlockPos(centerX, floorY, centerZ),
                        markerState,
                        3
                );
            }
        }
    }
    private static void applyVegetation(
            ServerLevel level,
            BlockPos portalPos,
            PersonalSpaceData data
    ) {
        if (!data.isTreesEnabled() && !data.isFoliageEnabled()) {
            return;
        }

        RandomSource random = RandomSource.create(level.getSeed() ^ portalPos.asLong());

        int centerX = portalPos.getX();
        int centerZ = portalPos.getZ();
        int floorY = portalPos.getY() - 1;

        int radius = 14;

        if (data.isFoliageEnabled()) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    if (isProtectedCreationArea(x, z, portalPos, data)) {
                        continue;
                    }

                    if (random.nextInt(5) != 0) {
                        continue;
                    }

                    BlockPos groundPos = new BlockPos(x, floorY, z);
                    BlockPos plantPos = groundPos.above();

                    if (level.getBlockState(groundPos).isAir()) {
                        continue;
                    }

                    if (!level.getBlockState(plantPos).isAir()) {
                        continue;
                    }

                    int variant = random.nextInt(4);

                    if (variant == 0) {
                        level.setBlock(plantPos, Blocks.DANDELION.defaultBlockState(), 3);
                    } else if (variant == 1) {
                        level.setBlock(plantPos, Blocks.POPPY.defaultBlockState(), 3);
                    } else {
                        level.setBlock(plantPos, Blocks.FERN.defaultBlockState(), 3);
                    }
                }
            }
        }

        if (data.isTreesEnabled()) {
            int attempts = 8;

            for (int i = 0; i < attempts; i++) {
                int x = centerX + random.nextInt(radius * 2 + 1) - radius;
                int z = centerZ + random.nextInt(radius * 2 + 1) - radius;

                if (isProtectedCreationArea(x, z, portalPos, data)) {
                    continue;
                }

                BlockPos basePos = new BlockPos(x, floorY + 1, z);

                if (!level.getBlockState(basePos.below()).isAir()
                        && level.getBlockState(basePos).isAir()) {
                    placeSimpleOakTree(level, basePos);
                }
            }
        }
    }

    private static boolean isProtectedCreationArea(
            int x,
            int z,
            BlockPos portalPos,
            PersonalSpaceData data
    ) {
        int centerX = portalPos.getX();
        int centerZ = portalPos.getZ();

        if (Math.abs(x - centerX) <= 4 && Math.abs(z - centerZ) <= 4) {
            return true;
        }

        int boundaryChunksX = data.getBoundaryChunksX();
        int boundaryChunksZ = data.getBoundaryChunksZ();
        int gapChunks = data.getGapChunks();

        int radiusX = Math.max(1, boundaryChunksX) * 16;
        int radiusZ = Math.max(1, boundaryChunksZ) * 16;

        int minX = centerX - radiusX;
        int maxX = centerX + radiusX;
        int minZ = centerZ - radiusZ;
        int maxZ = centerZ + radiusZ;

        if (x == minX || x == maxX || z == minZ || z == maxZ) {
            return true;
        }

        int roadHalfWidth = Math.max(0, gapChunks * 8);

        return roadHalfWidth > 0 &&
                (Math.abs(x - centerX) <= roadHalfWidth ||
                        Math.abs(z - centerZ) <= roadHalfWidth);
    }

    private static void placeSimpleOakTree(ServerLevel level, BlockPos basePos) {
        for (int y = 0; y <= 6; y++) {
            if (!level.getBlockState(basePos.above(y)).isAir()) {
                return;
            }
        }

        for (int y = 0; y < 5; y++) {
            level.setBlock(
                    basePos.above(y),
                    Blocks.OAK_LOG.defaultBlockState(),
                    3
            );
        }

        BlockPos leavesCenter = basePos.above(4);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int distance = Math.abs(dx) + Math.abs(dz) + Math.abs(dy);

                    if (distance > 4) {
                        continue;
                    }

                    BlockPos leafPos = leavesCenter.offset(dx, dy, dz);

                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(
                                leafPos,
                                Blocks.OAK_LEAVES.defaultBlockState(),
                                3
                        );
                    }
                }
            }
        }

        BlockPos top = leavesCenter.above();

        if (level.getBlockState(top).isAir()) {
            level.setBlock(
                    top,
                    Blocks.OAK_LEAVES.defaultBlockState(),
                    3
            );
        }
    }

    private static void clearPortalSpace(ServerLevel level, BlockPos portalPos) {
        for (int x = portalPos.getX() - 2; x <= portalPos.getX() + 2; x++) {
            for (int z = portalPos.getZ() - 2; z <= portalPos.getZ() + 2; z++) {
                for (int y = portalPos.getY(); y <= portalPos.getY() + 4; y++) {
                    level.setBlock(
                            new BlockPos(x, y, z),
                            Blocks.AIR.defaultBlockState(),
                            3
                    );
                }
            }
        }
    }

    private static BlockState blockStateFromId(String blockId, BlockState fallback) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);

        if (id == null) {
            return fallback;
        }

        return BuiltInRegistries.BLOCK
                .getOptional(id)
                .map(block -> block.defaultBlockState())
                .orElse(fallback);
    }
    private static void applyStoredSettings(ServerLevel level) {
        if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        level.setDayTime(data.getTimeOfDay());
    }
}