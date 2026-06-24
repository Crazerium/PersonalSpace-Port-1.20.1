package me.eigenraven.personalspace.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.eigenraven.personalspace.registry.PSChunkGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PersonalSpaceGridChunkGenerator extends ChunkGenerator {
    public static final MapCodec<PersonalSpaceGridChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.source),
                    Codec.INT.fieldOf("min_y").forGetter(generator -> generator.minY),
                    Codec.INT.fieldOf("height").forGetter(generator -> generator.height),
                    Codec.INT.fieldOf("ground_y").forGetter(generator -> generator.groundY),

                    Codec.STRING.fieldOf("layers_preset").forGetter(generator -> generator.layersPreset),

                    Codec.INT.fieldOf("plot_chunks_x").forGetter(generator -> generator.plotChunksX),
                    Codec.INT.fieldOf("plot_chunks_z").forGetter(generator -> generator.plotChunksZ),
                    Codec.INT.fieldOf("gap_chunks").forGetter(generator -> generator.gapChunks),

                    Codec.STRING.fieldOf("boundary_block").forGetter(generator -> generator.boundaryBlock),
                    Codec.STRING.fieldOf("road_block").forGetter(generator -> generator.roadBlock),
                    Codec.STRING.fieldOf("center_marker_block").forGetter(generator -> generator.centerMarkerBlock),

                    Codec.BOOL.fieldOf("center_marker_enabled").forGetter(generator -> generator.centerMarkerEnabled),

                    Codec.INT.fieldOf("origin_x").forGetter(generator -> generator.originX),
                    Codec.INT.fieldOf("origin_z").forGetter(generator -> generator.originZ)
            ).apply(instance, PersonalSpaceGridChunkGenerator::new));

    private final BiomeSource source;

    private final int minY;
    private final int height;
    private final int groundY;

    private final String layersPreset;

    private final int plotChunksX;
    private final int plotChunksZ;
    private final int gapChunks;

    private final String boundaryBlock;
    private final String roadBlock;
    private final String centerMarkerBlock;

    private final boolean centerMarkerEnabled;

    private final int originX;
    private final int originZ;

    private final List<PersonalSpaceLayerParser.Layer> cachedLayers;
    private final int totalLayerHeight;
    private final int layerStartY;

    private final BlockState plotTopState;
    private final BlockState roadState;
    private final BlockState boundaryState;
    private final BlockState centerState;

    public PersonalSpaceGridChunkGenerator(
            BiomeSource source,
            int minY,
            int height,
            int groundY,
            String layersPreset,
            int plotChunksX,
            int plotChunksZ,
            int gapChunks,
            String boundaryBlock,
            String roadBlock,
            String centerMarkerBlock,
            boolean centerMarkerEnabled,
            int originX,
            int originZ
    ) {
        super(source);

        this.source = source;

        this.minY = minY;
        this.height = height;
        this.groundY = Mth.clamp(groundY, minY, minY + height - 1);

        this.layersPreset = layersPreset == null
                ? "minecraft:bedrock,1;minecraft:dirt,3;minecraft:grass_block,1"
                : layersPreset;

        this.plotChunksX = Math.max(1, plotChunksX);
        this.plotChunksZ = Math.max(1, plotChunksZ);
        this.gapChunks = Math.max(1, gapChunks);

        this.boundaryBlock = boundaryBlock;
        this.roadBlock = roadBlock;
        this.centerMarkerBlock = centerMarkerBlock;

        this.centerMarkerEnabled = centerMarkerEnabled;

        this.originX = originX;
        this.originZ = originZ;

        this.cachedLayers = buildSafeLayers(this.layersPreset);

        int layerHeight = 0;

        for (PersonalSpaceLayerParser.Layer layer : cachedLayers) {
            layerHeight += layer.count();
        }

        this.totalLayerHeight = Math.max(1, Math.min(layerHeight, height));
        this.layerStartY = this.groundY - this.totalLayerHeight + 1;

        this.plotTopState = cachedLayers.isEmpty()
                ? Blocks.GRASS_BLOCK.defaultBlockState()
                : cachedLayers.get(cachedLayers.size() - 1).state();

        this.roadState = blockStateFromId(
                this.roadBlock,
                Blocks.COBBLESTONE.defaultBlockState()
        );

        this.boundaryState = blockStateFromId(
                this.boundaryBlock,
                Blocks.WHITE_CONCRETE.defaultBlockState()
        );

        this.centerState = blockStateFromId(
                this.centerMarkerBlock,
                Blocks.SEA_LANTERN.defaultBlockState()
        );
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return PSChunkGenerators.PERSONAL_SPACE_GRID.get();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        ChunkPos chunkPos = chunk.getPos();

        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minBlockX + localX;
                int worldZ = minBlockZ + localZ;

                for (int y = layerStartY; y <= groundY; y++) {
                    BlockState state = getStateAt(worldX, y, worldZ);

                    if (!state.isAir()) {
                        mutablePos.set(worldX, y, worldZ);
                        chunk.setBlockState(mutablePos, state, false);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor level,
            RandomState randomState
    ) {
        return groundY + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor level,
            RandomState randomState
    ) {
        BlockState[] states = new BlockState[height];

        for (int i = 0; i < states.length; i++) {
            int y = minY + i;

            states[i] = getStateAt(x, y, z);
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void buildSurface(
            WorldGenRegion region,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk
    ) {
    }

    @Override
    public void applyCarvers(
            WorldGenRegion region,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving carving
    ) {
    }

    @Override
    public void applyBiomeDecoration(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structureManager
    ) {
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager
    ) {
    }

    @Override
    public void createReferences(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return height;
    }

    @Override
    public int getSeaLevel() {
        return -64;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public void addDebugScreenInfo(
            List<String> lines,
            RandomState randomState,
            BlockPos pos
    ) {
        lines.add("PersonalSpace grid generator");
    }

    private BlockState getStateAt(int worldX, int y, int worldZ) {
        if (y < layerStartY || y > groundY) {
            return Blocks.AIR.defaultBlockState();
        }

        if (y == groundY) {
            return getTopStateAt(worldX, worldZ);
        }

        return getLayerStateAtY(y);
    }

    private BlockState getLayerStateAtY(int y) {
        if (cachedLayers.isEmpty()) {
            return Blocks.DIRT.defaultBlockState();
        }

        int relativeY = y - layerStartY;
        int cursor = 0;

        for (PersonalSpaceLayerParser.Layer layer : cachedLayers) {
            cursor += layer.count();

            if (relativeY < cursor) {
                return layer.state();
            }
        }

        return cachedLayers.get(cachedLayers.size() - 1).state();
    }

    private BlockState getTopStateAt(int worldX, int worldZ) {
        int plotBlocksX = plotChunksX * 16;
        int plotBlocksZ = plotChunksZ * 16;
        int gapBlocks = gapChunks * 16;

        int cycleBlocksX = plotBlocksX + gapBlocks;
        int cycleBlocksZ = plotBlocksZ + gapBlocks;

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
                        centerMarkerEnabled &&
                        relX == plotBlocksX / 2 &&
                        relZ == plotBlocksZ / 2;

        if (center) {
            return centerState;
        }

        if (road) {
            return roadState;
        }

        if (boundary) {
            return boundaryState;
        }

        return plotTopState;
    }

    private static List<PersonalSpaceLayerParser.Layer> buildSafeLayers(String preset) {
        List<PersonalSpaceLayerParser.Layer> layers =
                PersonalSpaceLayerParser.parse(preset);

        if (!layers.isEmpty()) {
            return layers;
        }

        List<PersonalSpaceLayerParser.Layer> fallback = new ArrayList<>();
        fallback.add(new PersonalSpaceLayerParser.Layer(Blocks.BEDROCK.defaultBlockState(), 1));
        fallback.add(new PersonalSpaceLayerParser.Layer(Blocks.DIRT.defaultBlockState(), 3));
        fallback.add(new PersonalSpaceLayerParser.Layer(Blocks.GRASS_BLOCK.defaultBlockState(), 1));
        return fallback;
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
}