package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.block.PortalBlock;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.registry.PSBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateDimensionPacket {
    private final PersonalSpaceData.WorldType type;
    private final int height;
    private final BlockPos sourcePortalPos;
    private final ResourceLocation sourceLevelId;

    private final long timeOfDay;
    private final int skyRed;
    private final int skyGreen;
    private final int skyBlue;

    private final float starBrightness;
    private final String biomeName;

    private final boolean treesEnabled;
    private final boolean foliageEnabled;
    private final boolean weatherEnabled;
    private final boolean cloudsEnabled;

    private final String layersPreset;
    private final int boundaryChunksX;
    private final int boundaryChunksZ;
    private final int gapChunks;

    private final String boundaryBlock;
    private final String roadBlock;
    private final String centerMarkerBlock;

    private final boolean centerMarkerEnabled;
    private final boolean repeatingGridEnabled;

    public CreateDimensionPacket(
            PersonalSpaceData.WorldType type,
            int height,
            BlockPos sourcePortalPos,
            ResourceLocation sourceLevelId,
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue,
            float starBrightness,
            String biomeName,
            boolean treesEnabled,
            boolean foliageEnabled,
            boolean weatherEnabled,
            boolean cloudsEnabled,
            String layersPreset,
            int boundaryChunksX,
            int boundaryChunksZ,
            int gapChunks,
            String boundaryBlock,
            String roadBlock,
            String centerMarkerBlock,
            boolean centerMarkerEnabled,
            boolean repeatingGridEnabled
    ) {
        this.type = type;
        this.height = height;
        this.sourcePortalPos = sourcePortalPos;
        this.sourceLevelId = sourceLevelId;

        this.timeOfDay = timeOfDay;
        this.skyRed = skyRed;
        this.skyGreen = skyGreen;
        this.skyBlue = skyBlue;

        this.starBrightness = starBrightness;
        this.biomeName = biomeName;

        this.treesEnabled = treesEnabled;
        this.foliageEnabled = foliageEnabled;
        this.weatherEnabled = weatherEnabled;
        this.cloudsEnabled = cloudsEnabled;

        this.layersPreset = layersPreset;
        this.boundaryChunksX = boundaryChunksX;
        this.boundaryChunksZ = boundaryChunksZ;
        this.gapChunks = gapChunks;

        this.boundaryBlock = boundaryBlock;
        this.roadBlock = roadBlock;
        this.centerMarkerBlock = centerMarkerBlock;

        this.centerMarkerEnabled = centerMarkerEnabled;
        this.repeatingGridEnabled = repeatingGridEnabled;
    }

    public static void encode(CreateDimensionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeInt(msg.height);
        buf.writeBlockPos(msg.sourcePortalPos);

        buf.writeBoolean(msg.sourceLevelId != null);

        if (msg.sourceLevelId != null) {
            buf.writeResourceLocation(msg.sourceLevelId);
        }

        buf.writeLong(msg.timeOfDay);
        buf.writeInt(msg.skyRed);
        buf.writeInt(msg.skyGreen);
        buf.writeInt(msg.skyBlue);

        buf.writeFloat(msg.starBrightness);
        buf.writeUtf(msg.biomeName);

        buf.writeBoolean(msg.treesEnabled);
        buf.writeBoolean(msg.foliageEnabled);
        buf.writeBoolean(msg.weatherEnabled);
        buf.writeBoolean(msg.cloudsEnabled);

        buf.writeUtf(msg.layersPreset);

        buf.writeInt(msg.boundaryChunksX);
        buf.writeInt(msg.boundaryChunksZ);
        buf.writeInt(msg.gapChunks);

        buf.writeUtf(msg.boundaryBlock);
        buf.writeUtf(msg.roadBlock);
        buf.writeUtf(msg.centerMarkerBlock);

        buf.writeBoolean(msg.centerMarkerEnabled);
        buf.writeBoolean(msg.repeatingGridEnabled);
    }

    public static CreateDimensionPacket decode(FriendlyByteBuf buf) {
        PersonalSpaceData.WorldType type = buf.readEnum(PersonalSpaceData.WorldType.class);
        int height = buf.readInt();
        BlockPos sourcePortalPos = buf.readBlockPos();

        ResourceLocation sourceLevelId = null;

        if (buf.readBoolean()) {
            sourceLevelId = buf.readResourceLocation();
        }

        return new CreateDimensionPacket(
                type,
                height,
                sourcePortalPos,
                sourceLevelId,

                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),

                buf.readFloat(),
                buf.readUtf(),

                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),

                buf.readUtf(),

                buf.readInt(),
                buf.readInt(),
                buf.readInt(),

                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),

                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(
            CreateDimensionPacket msg,
            Supplier<NetworkEvent.Context> ctx
    ) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            handleOnServer(msg, player);
        });

        context.setPacketHandled(true);
    }

    private static void handleOnServer(CreateDimensionPacket msg, ServerPlayer player) {
        MinecraftServer server = player.server;

        if (server == null) {
            return;
        }

        PersonalSpaceData.WorldType safeType = msg.type == null
                ? PersonalSpaceData.WorldType.VOID
                : msg.type;

        int safeHeight = net.minecraft.util.Mth.clamp(msg.height, 1, 240);

        long safeTimeOfDay = Math.max(0L, Math.min(24000L, msg.timeOfDay));

        int safeSkyRed = net.minecraft.util.Mth.clamp(msg.skyRed, 0, 255);
        int safeSkyGreen = net.minecraft.util.Mth.clamp(msg.skyGreen, 0, 255);
        int safeSkyBlue = net.minecraft.util.Mth.clamp(msg.skyBlue, 0, 255);

        float safeStarBrightness = net.minecraft.util.Mth.clamp(
                msg.starBrightness,
                0.0F,
                1.0F
        );

        String safeBiomeName = sanitizeBiomeId(
                server,
                msg.biomeName,
                "minecraft:plains"
        );

        String safeLayersPreset = sanitizeLayersPreset(
                msg.layersPreset,
                "minecraft:bedrock,1;minecraft:dirt,3;minecraft:grass_block,1"
        );

        int safeBoundaryChunksX = net.minecraft.util.Mth.clamp(
                msg.boundaryChunksX,
                0,
                16
        );

        int safeBoundaryChunksZ = net.minecraft.util.Mth.clamp(
                msg.boundaryChunksZ,
                0,
                16
        );

        int safeGapChunks = net.minecraft.util.Mth.clamp(
                msg.gapChunks,
                0,
                16
        );

        String safeBoundaryBlock = sanitizeBlockId(
                msg.boundaryBlock,
                "minecraft:white_concrete"
        );

        String safeRoadBlock = sanitizeBlockId(
                msg.roadBlock,
                "minecraft:cobbled_deepslate"
        );

        String safeCenterMarkerBlock = sanitizeBlockId(
                msg.centerMarkerBlock,
                "minecraft:white_concrete"
        );

        ServerLevel sourceLevel = getSourceLevel(msg, player);

        if (sourceLevel == null) {
            player.sendSystemMessage(Component.literal("Source dimension was not found."));
            return;
        }

        BlockEntity sourceBlockEntity = sourceLevel.getBlockEntity(msg.sourcePortalPos);

        if (!(sourceBlockEntity instanceof PortalBlockEntity sourcePortal)) {
            player.sendSystemMessage(Component.literal("Personal Space portal was not found."));
            return;
        }

        if (sourcePortal.isActive() && sourcePortal.getTargetLevel() != null) {
            sourcePortal.teleport(player);
            return;
        }

        ResourceKey<Level> newLevelKey = PSDimensions.personalKeyForPlayer(
                server,
                player.getGameProfile().getName()
        );

        PersonalSpaceData data = new PersonalSpaceData();

        data.setType(safeType);
        data.setGroundLevel(safeHeight);

        data.setReturnLevel(sourceLevel.dimension().location().toString());
        data.setReturnPos(msg.sourcePortalPos);

        data.setTimeOfDay(safeTimeOfDay);
        data.setSkyColor(safeSkyRed, safeSkyGreen, safeSkyBlue);

        data.setStarBrightness(safeStarBrightness);
        data.setBiomeName(safeBiomeName);

        data.setTreesEnabled(msg.treesEnabled);
        data.setFoliageEnabled(msg.foliageEnabled);
        data.setWeatherEnabled(msg.weatherEnabled);
        data.setCloudsEnabled(msg.cloudsEnabled);

        data.setLayersPreset(safeLayersPreset);

        data.setBoundaryChunksX(safeBoundaryChunksX);
        data.setBoundaryChunksZ(safeBoundaryChunksZ);
        data.setGapChunks(safeGapChunks);

        data.setBoundaryBlock(safeBoundaryBlock);
        data.setRoadBlock(safeRoadBlock);
        data.setCenterMarkerBlock(safeCenterMarkerBlock);

        data.setCenterMarkerEnabled(msg.centerMarkerEnabled);
        data.setRepeatingGridEnabled(msg.repeatingGridEnabled);

        BlockPos innerPortalPos = new BlockPos(
                7,
                data.getGroundLevel() + 1,
                7
        );

        if (data.isRepeatingGridEnabled()) {
            int plotBlocksX = Math.max(1, data.getBoundaryChunksX()) * 16;
            int plotBlocksZ = Math.max(1, data.getBoundaryChunksZ()) * 16;

            data.setRepeatingGridOrigin(
                    innerPortalPos.getX() - plotBlocksX / 2,
                    innerPortalPos.getZ() - plotBlocksZ / 2
            );
        }

        ServerLevel newLevel = PSDimensions.createPersonalDimension(
                server,
                newLevelKey,
                data
        );

        newLevel.setDayTime(data.getTimeOfDay());

        if (!data.isWeatherEnabled()) {
            newLevel.setWeatherParameters(
                    6000,
                    0,
                    false,
                    false
            );
        }

        PSDimensions.prepareSpawnArea(
                newLevel,
                data.getType(),
                data.getGroundLevel(),
                innerPortalPos
        );

        BlockState portalState = PSBlocks.PERSONAL_PORTAL.get()
                .defaultBlockState()
                .setValue(PortalBlock.RETURN_PORTAL, true);

        newLevel.setBlock(innerPortalPos, portalState, 3);

        PortalBlockEntity innerPortal = getOrCreatePortalBlockEntity(
                newLevel,
                innerPortalPos,
                portalState
        );

        ResourceLocation returnLocation = ResourceLocation.tryParse(data.getReturnLevel());

        if (returnLocation == null) {
            player.sendSystemMessage(Component.literal(
                    "Invalid return dimension: " + data.getReturnLevel()
            ));
            return;
        }

        ResourceKey<Level> returnKey = ResourceKey.create(
                Registries.DIMENSION,
                returnLocation
        );

        innerPortal.setReturnPortal(true);
        innerPortal.setTarget(returnKey, data.getReturnPos());

        sourcePortal.setReturnPortal(false);
        sourcePortal.setTarget(newLevelKey, innerPortalPos);

        player.teleportTo(
                newLevel,
                innerPortalPos.getX() + 0.5D,
                innerPortalPos.getY() + 1.0D,
                innerPortalPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        PersonalSpaceSettingsSync.syncTo(player, newLevel);
    }

    private static String sanitizeBiomeId(
            MinecraftServer server,
            String rawId,
            String fallbackId
    ) {
        ResourceLocation id = ResourceLocation.tryParse(cleanId(rawId));

        if (id == null) {
            return fallbackId;
        }

        if (!server.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .containsKey(id)) {
            return fallbackId;
        }

        return id.toString();
    }

    private static String sanitizeBlockId(
            String rawId,
            String fallbackId
    ) {
        ResourceLocation id = ResourceLocation.tryParse(cleanId(rawId));

        if (id == null) {
            return fallbackId;
        }

        if (!isAllowedPersonalSpaceBlock(id)) {
            return fallbackId;
        }

        return id.toString();
    }

    public static boolean isAllowedPersonalSpaceBlock(ResourceLocation id) {
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return false;
        }

        String namespace = id.getNamespace();
        String path = id.getPath();
        if (isForbiddenMaterialBlockName(path)) {
            return false;
        }
        boolean decorativeModBlock =
                namespace.equals("chisel")
                        || namespace.equals("chisel_reborn")
                        || namespace.equals("chiselreborn")
                        || namespace.equals("xtones")
                        || namespace.equals("xtones_reworked")
                        || namespace.equals("xtonesreworked")
                        || namespace.equals("xtones_reforged")
                        || namespace.equals("xtonesreforged");

        if (!namespace.equals("minecraft") && !decorativeModBlock) {
            return false;
        }

        if (isForbiddenBlockName(path)) {
            return false;
        }

        var block = BuiltInRegistries.BLOCK.get(id);
        var state = block.defaultBlockState();

        if (state.isAir()) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false;
        }
        if (namespace.equals("minecraft")) {
            return isAllowedVanillaBuildingBlock(path);
        }
        return decorativeModBlock;
    }


    private static boolean isForbiddenBlockName(String path) {
        return path.contains("log")
                || path.contains("wood")
                || path.contains("stem")
                || path.contains("hyphae")
                || path.contains("planks")
                || path.contains("leaves")
                || path.contains("sapling")
                || path.contains("root")
                || path.contains("mushroom")
                || path.contains("fungus")
                || path.contains("wart")
                || path.contains("azalea")
                || path.contains("bamboo")
                || path.contains("cactus")
                || path.contains("sugar_cane")
                || path.contains("kelp")
                || path.contains("coral")
                || path.contains("flower")
                || path.contains("tulip")
                || path.contains("rose")
                || path.contains("orchid")
                || path.contains("allium")
                || path.contains("daisy")
                || path.contains("torchflower")
                || path.contains("pitcher")
                || path.contains("crop")
                || path.contains("wheat")
                || path.contains("carrot")
                || path.contains("potato")
                || path.contains("beetroot")
                || path.contains("melon")
                || path.contains("pumpkin")
                || path.contains("ore")
                || path.contains("raw_")
                || path.contains("diamond")
                || path.contains("emerald")
                || path.contains("gold")
                || path.contains("iron")
                || path.contains("copper")
                || path.contains("coal")
                || path.contains("lapis")
                || path.contains("redstone")
                || path.contains("quartz")
                || path.contains("netherite")
                || path.contains("ancient_debris")
                || path.contains("amethyst")
                || path.contains("beacon")
                || path.contains("barrier")
                || path.contains("bedrock")
                || path.contains("command")
                || path.contains("structure")
                || path.contains("jigsaw")
                || path.contains("spawner")
                || path.contains("chest")
                || path.contains("barrel")
                || path.contains("shulker")
                || path.contains("furnace")
                || path.contains("blast_furnace")
                || path.contains("smoker")
                || path.contains("hopper")
                || path.contains("dispenser")
                || path.contains("dropper")
                || path.contains("piston")
                || path.contains("observer")
                || path.contains("comparator")
                || path.contains("repeater")
                || path.contains("daylight_detector")
                || path.contains("enchanting")
                || path.contains("anvil")
                || path.contains("grindstone")
                || path.contains("smithing")
                || path.contains("brewing")
                || path.contains("cauldron")
                || path.contains("bell")
                || path.contains("lectern")
                || path.contains("portal")
                || path.contains("end_gateway")
                || path.contains("end_portal")
                || path.contains("dragon_egg")
                || path.contains("water")
                || path.contains("lava")
                || path.contains("ice")
                || path.contains("snow")
                || path.contains("powder_snow")
                || path.contains("fire")
                || path.contains("candle")
                || path.contains("torch")
                || path.contains("lantern")
                || path.contains("sea_lantern")
                || path.contains("glowstone")
                || path.contains("soul")
                || path.contains("skull")
                || path.contains("end_stone")
                || path.contains("endstone")
                || path.contains("wool")
                || path.contains("netherrack")
                || path.contains("nether_brick")
                || path.contains("netherbrick")
                || path.contains("nether_bricks")
                || path.contains("head");
    }

    private static boolean isForbiddenMaterialBlockName(String path) {
        return path.contains("quartz")
                || path.contains("iron")
                || path.contains("gold")
                || path.contains("copper")
                || path.contains("diamond")
                || path.contains("emerald")
                || path.contains("lapis")
                || path.contains("redstone")
                || path.contains("coal")
                || path.contains("netherite")
                || path.contains("ancient_debris")
                || path.contains("amethyst")
                || path.contains("raw_")
                || path.contains("ore")
                || path.contains("metal")
                || path.contains("steel")
                || path.contains("bronze")
                || path.contains("tin")
                || path.contains("lead")
                || path.contains("silver")
                || path.contains("nickel")
                || path.contains("uranium")
                || path.contains("osmium")
                || path.contains("aluminum")
                || path.contains("aluminium");
    }


    private static boolean isAllowedVanillaBuildingBlock(String path) {
        return path.equals("stone")
                || path.equals("smooth_stone")
                || path.equals("cobblestone")
                || path.equals("mossy_cobblestone")
                || path.equals("stone_bricks")
                || path.equals("mossy_stone_bricks")
                || path.equals("cracked_stone_bricks")
                || path.equals("chiseled_stone_bricks")

                || path.equals("granite")
                || path.equals("polished_granite")
                || path.equals("diorite")
                || path.equals("polished_diorite")
                || path.equals("andesite")
                || path.equals("polished_andesite")

                || path.equals("deepslate")
                || path.equals("cobbled_deepslate")
                || path.equals("polished_deepslate")
                || path.equals("deepslate_bricks")
                || path.equals("cracked_deepslate_bricks")
                || path.equals("deepslate_tiles")
                || path.equals("cracked_deepslate_tiles")
                || path.equals("chiseled_deepslate")

                || path.equals("tuff")
                || path.equals("calcite")
                || path.equals("dripstone_block")

                || path.equals("basalt")
                || path.equals("smooth_basalt")
                || path.equals("polished_basalt")
                || path.equals("blackstone")
                || path.equals("polished_blackstone")
                || path.equals("polished_blackstone_bricks")
                || path.equals("cracked_polished_blackstone_bricks")
                || path.equals("chiseled_polished_blackstone")
                || path.equals("purpur_block")
                || path.equals("purpur_pillar")
                || path.equals("sandstone")
                || path.equals("smooth_sandstone")
                || path.equals("cut_sandstone")
                || path.equals("chiseled_sandstone")
                || path.equals("red_sandstone")
                || path.equals("smooth_red_sandstone")
                || path.equals("cut_red_sandstone")
                || path.equals("chiseled_red_sandstone")

                || path.equals("bricks")
                || path.equals("mud_bricks")
                || path.equals("packed_mud")

                || path.equals("dirt")
                || path.equals("coarse_dirt")
                || path.equals("rooted_dirt")
                || path.equals("grass_block")
                || path.equals("podzol")
                || path.equals("mycelium")
                || path.equals("mud")
                || path.equals("clay")
                || path.equals("gravel")
                || path.equals("sand")
                || path.equals("red_sand")

                || path.equals("glass")
                || path.equals("tinted_glass")
                || path.endsWith("_stained_glass")

                || path.equals("terracotta")
                || path.endsWith("_terracotta")
                || path.endsWith("_glazed_terracotta")

                || path.equals("white_concrete")
                || path.equals("orange_concrete")
                || path.equals("magenta_concrete")
                || path.equals("light_blue_concrete")
                || path.equals("yellow_concrete")
                || path.equals("lime_concrete")
                || path.equals("pink_concrete")
                || path.equals("gray_concrete")
                || path.equals("light_gray_concrete")
                || path.equals("cyan_concrete")
                || path.equals("purple_concrete")
                || path.equals("blue_concrete")
                || path.equals("brown_concrete")
                || path.equals("green_concrete")
                || path.equals("red_concrete")
                || path.equals("black_concrete")

                || path.equals("white_concrete_powder")
                || path.equals("orange_concrete_powder")
                || path.equals("magenta_concrete_powder")
                || path.equals("light_blue_concrete_powder")
                || path.equals("yellow_concrete_powder")
                || path.equals("lime_concrete_powder")
                || path.equals("pink_concrete_powder")
                || path.equals("gray_concrete_powder")
                || path.equals("light_gray_concrete_powder")
                || path.equals("cyan_concrete_powder")
                || path.equals("purple_concrete_powder")
                || path.equals("blue_concrete_powder")
                || path.equals("brown_concrete_powder")
                || path.equals("green_concrete_powder")
                || path.equals("red_concrete_powder")
                || path.equals("black_concrete_powder");
    }

    private static String sanitizeLayersPreset(
            String rawPreset,
            String fallbackPreset
    ) {
        if (rawPreset == null || rawPreset.isBlank()) {
            return fallbackPreset;
        }

        StringBuilder sanitized = new StringBuilder();
        String[] layers = rawPreset.split(";");

        for (String layer : layers) {
            String[] parts = layer.split(",");

            if (parts.length != 2) {
                return fallbackPreset;
            }

            String blockId = sanitizeBlockId(parts[0], null);

            if (blockId == null) {
                return fallbackPreset;
            }

            int count;

            try {
                count = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                return fallbackPreset;
            }

            count = net.minecraft.util.Mth.clamp(count, 1, 256);

            if (!sanitized.isEmpty()) {
                sanitized.append(";");
            }

            sanitized.append(blockId).append(",").append(count);
        }

        if (sanitized.isEmpty()) {
            return fallbackPreset;
        }

        return sanitized.toString();
    }

    private static String cleanId(String rawId) {
        if (rawId == null) {
            return "";
        }

        return rawId.trim().toLowerCase();
    }

    private static ServerLevel getSourceLevel(
            CreateDimensionPacket msg,
            ServerPlayer player
    ) {
        MinecraftServer server = player.server;

        if (server == null) {
            return null;
        }

        ResourceLocation sourceId = msg.sourceLevelId;

        if (sourceId == null) {
            sourceId = player.level().dimension().location();
        }

        ResourceKey<Level> sourceKey = ResourceKey.create(
                Registries.DIMENSION,
                sourceId
        );

        ServerLevel sourceLevel = server.getLevel(sourceKey);

        if (sourceLevel != null) {
            return sourceLevel;
        }

        if (player.level() instanceof ServerLevel currentLevel) {
            return currentLevel;
        }

        return null;
    }

    private static PortalBlockEntity getOrCreatePortalBlockEntity(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        BlockEntity existing = level.getBlockEntity(pos);

        if (existing instanceof PortalBlockEntity portal) {
            return portal;
        }

        PortalBlockEntity portal = new PortalBlockEntity(pos, state);
        level.setBlockEntity(portal);

        return portal;
    }
}