package me.eigenraven.personalspace.dimension;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.commoble.infiniverse.api.InfiniverseAPI;
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
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
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

    public static ResourceKey<Level> personalKeyForPlayer(MinecraftServer server, String playerName) {
        String safeName = sanitizeDimensionName(playerName);

        if (safeName.isBlank()) {
            return randomPersonalKey();
        }

        ResourceKey<Level> baseKey = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, "ps_" + safeName)
        );

        if (server.getLevel(baseKey) == null) {
            return baseKey;
        }

        for (int index = 2; index < 10_000; index++) {
            ResourceKey<Level> candidate = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, "ps_" + safeName + "_" + index)
            );

            if (server.getLevel(candidate) == null) {
                return candidate;
            }
        }

        return randomPersonalKey();
    }

    private static String sanitizeDimensionName(String rawName) {
        if (rawName == null) {
            return "";
        }

        String lowerName = rawName
                .trim()
                .toLowerCase(Locale.ROOT);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lowerName.length(); i++) {
            char c = lowerName.charAt(i);

            if ((c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-'
                    || c == '.') {
                result.append(c);
            } else {
                result.append('_');
            }
        }

        while (result.toString().contains("__")) {
            int index = result.indexOf("__");
            result.replace(index, index + 2, "_");
        }

        String safe = result.toString();

        while (safe.startsWith("_")) {
            safe = safe.substring(1);
        }

        while (safe.endsWith("_")) {
            safe = safe.substring(0, safe.length() - 1);
        }

        if (safe.length() > 48) {
            safe = safe.substring(0, 48);
        }

        return safe;
    }

    public static ResourceKey<Level> key(String idOrPath) {
        ResourceLocation location;

        if (idOrPath.contains(":")) {
            location = ResourceLocation.parse(idOrPath.toLowerCase(Locale.ROOT));
        } else {
            location = ResourceLocation.fromNamespaceAndPath(
                    PersonalSpace.MODID,
                    idOrPath.toLowerCase(Locale.ROOT)
            );
        }

        return ResourceKey.create(Registries.DIMENSION, location);
    }

    public static ServerLevel getOrCreate(MinecraftServer server, ResourceKey<Level> levelKey) {
        ServerLevel level = InfiniverseAPI.get().getOrCreateLevel(
                server,
                levelKey,
                () -> createStem(server, PersonalSpaceData.WorldType.VOID, DEFAULT_GROUND_LEVEL, "minecraft:plains")
        );

        applyStoredSettings(level);

        return level;
    }

    public static ServerLevel createPersonalDimension(
            MinecraftServer server,
            ResourceKey<Level> levelKey,
            PersonalSpaceData data
    ) {
        PersonalSpaceData.WorldType safeType = data.getType() == null
                ? PersonalSpaceData.WorldType.VOID
                : data.getType();

        int safeGroundLevel = clampGroundLevel(server.overworld(), data.getGroundLevel());
        data.setGroundLevel(safeGroundLevel);

        ServerLevel newLevel = InfiniverseAPI.get().getOrCreateLevel(
                server,
                levelKey,
                () -> createStem(server, data)
        );

        PersonalSpaceData.save(newLevel, data);

        applyStoredSettings(newLevel);

        return newLevel;
    }

    private static Holder<Biome> resolveBiome(
            MinecraftServer server,
            String biomeName
    ) {
        ResourceLocation biomeId = ResourceLocation.tryParse(
                biomeName == null || biomeName.isBlank()
                        ? "minecraft:plains"
                        : biomeName
        );

        ResourceKey<Biome> fallbackKey = Biomes.PLAINS;

        if (biomeId == null) {
            return server.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(fallbackKey);
        }

        ResourceKey<Biome> requestedKey = ResourceKey.create(
                Registries.BIOME,
                biomeId
        );

        return server.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(requestedKey)
                .orElseGet(() -> server.registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getHolderOrThrow(fallbackKey));
    }

    private static LevelStem createStem(
            MinecraftServer server,
            PersonalSpaceData.WorldType type,
            int groundLevel,
            String biomeName
    ) {
        PersonalSpaceData.WorldType safeType = type == null
                ? PersonalSpaceData.WorldType.VOID
                : type;

        ServerLevel overworld = server.overworld();

        Holder<DimensionType> dimensionType = overworld.dimensionTypeRegistration();

        Holder<Biome> biome = resolveBiome(server, biomeName);

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

    private static LevelStem createStem(
            MinecraftServer server,
            PersonalSpaceData data
    ) {
        PersonalSpaceData.WorldType safeType = data.getType() == null
                ? PersonalSpaceData.WorldType.VOID
                : data.getType();

        ServerLevel overworld = server.overworld();

        Holder<DimensionType> dimensionType = overworld.dimensionTypeRegistration();
        Holder<Biome> biome = resolveBiome(server, data.getBiomeName());

        if (safeType == PersonalSpaceData.WorldType.FLAT && data.isRepeatingGridEnabled()) {
            int minY = overworld.getMinBuildHeight();
            int height = overworld.getMaxBuildHeight() - minY;

            ChunkGenerator generator = new PersonalSpaceGridChunkGenerator(
                    new FixedBiomeSource(biome),
                    minY,
                    height,
                    data.getGroundLevel(),
                    data.getLayersPreset(),
                    data.getBoundaryChunksX(),
                    data.getBoundaryChunksZ(),
                    data.getGapChunks(),
                    data.getBoundaryBlock(),
                    data.getRoadBlock(),
                    data.getCenterMarkerBlock(),
                    data.isCenterMarkerEnabled(),
                    data.getRepeatingGridOriginX(),
                    data.getRepeatingGridOriginZ()
            );

            return new LevelStem(dimensionType, generator);
        }

        return createStem(
                server,
                safeType,
                data.getGroundLevel(),
                data.getBiomeName()
        );
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
            if (!data.isRepeatingGridEnabled()) {
                applyPresetLayersAroundPortal(
                        level,
                        portalPos,
                        presetLayers
                );

                applyBoundaryRoadsAndMarker(level, portalPos, data);
                applyVegetation(level, portalPos, data);
            }

            clearPortalSpace(level, portalPos);
        }
    }

    private static void alignRepeatingGridToPortal(
            PersonalSpaceData data,
            BlockPos portalPos
    ) {
        int plotBlocksX = Math.max(1, data.getBoundaryChunksX()) * 16;
        int plotBlocksZ = Math.max(1, data.getBoundaryChunksZ()) * 16;

        int originX = portalPos.getX() - plotBlocksX / 2;
        int originZ = portalPos.getZ() - plotBlocksZ / 2;

        data.setRepeatingGridOrigin(originX, originZ);
    }

    private static void applyRepeatingGridChunk(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            PersonalSpaceData data
    ) {
        int groundY = data.getGroundLevel();

        BlockState plotState = getTopLayerState(data);

        BlockState roadState = blockStateFromId(
                data.getRoadBlock(),
                Blocks.COBBLESTONE.defaultBlockState()
        );

        BlockState boundaryState = blockStateFromId(
                data.getBoundaryBlock(),
                Blocks.WHITE_CONCRETE.defaultBlockState()
        );

        BlockState centerState = blockStateFromId(
                data.getCenterMarkerBlock(),
                Blocks.SEA_LANTERN.defaultBlockState()
        );

        int plotBlocksX = Math.max(1, data.getBoundaryChunksX()) * 16;
        int plotBlocksZ = Math.max(1, data.getBoundaryChunksZ()) * 16;
        int gapBlocks = Math.max(1, data.getGapChunks()) * 16;

        int cycleBlocksX = plotBlocksX + gapBlocks;
        int cycleBlocksZ = plotBlocksZ + gapBlocks;

        int originX = data.getRepeatingGridOriginX();
        int originZ = data.getRepeatingGridOriginZ();

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;

                int relX = Math.floorMod(worldX - originX, cycleBlocksX);
                int relZ = Math.floorMod(worldZ - originZ, cycleBlocksZ);

                boolean road =
                        relX >= plotBlocksX ||
                                relZ >= plotBlocksZ;

                boolean boundary =
                        !road &&
                                (
                                        relX == 0 ||
                                                relX == plotBlocksX - 1 ||
                                                relZ == 0 ||
                                                relZ == plotBlocksZ - 1
                                );

                boolean center =
                        !road &&
                                data.isCenterMarkerEnabled() &&
                                relX == plotBlocksX / 2 &&
                                relZ == plotBlocksZ / 2;

                BlockState state;

                if (center) {
                    state = centerState;
                } else if (road) {
                    state = roadState;
                } else if (boundary) {
                    state = boundaryState;
                } else {
                    state = plotState;
                }

                level.setBlock(
                        new BlockPos(worldX, groundY, worldZ),
                        state,
                        2
                );
            }
        }
    }

    private static boolean isRepeatingGridPlotBoundary(
            int localChunkX,
            int localChunkZ,
            int localX,
            int localZ,
            int plotChunksX,
            int plotChunksZ
    ) {
        boolean firstPlotChunkX = localChunkX == 0;
        boolean lastPlotChunkX = localChunkX == plotChunksX - 1;
        boolean firstPlotChunkZ = localChunkZ == 0;
        boolean lastPlotChunkZ = localChunkZ == plotChunksZ - 1;

        return (firstPlotChunkX && localX == 0)
                || (lastPlotChunkX && localX == 15)
                || (firstPlotChunkZ && localZ == 0)
                || (lastPlotChunkZ && localZ == 15);
    }

    private static BlockState getTopLayerState(PersonalSpaceData data) {
        List<PersonalSpaceLayerParser.Layer> layers =
                PersonalSpaceLayerParser.parse(data.getLayersPreset());

        if (!layers.isEmpty()) {
            return layers.get(layers.size() - 1).state();
        }

        return Blocks.GRASS_BLOCK.defaultBlockState();
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

    public static boolean isPersonalSpaceDimension(ResourceLocation id) {
        return id != null
                && id.getNamespace().equals(PersonalSpace.MODID)
                && id.getPath().startsWith("ps_");
    }

    private static void applyStoredSettings(ServerLevel level) {
        if (!isPersonalSpaceDimension(level.dimension().location())) {
            return;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        level.setDayTime(data.getTimeOfDay());

        if (!data.isWeatherEnabled()) {
            level.setWeatherParameters(6000, 0, false, false);
        }
    }
}