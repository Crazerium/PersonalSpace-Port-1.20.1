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
                "minecraft:sea_lantern"
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

        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return fallbackId;
        }

        return id.toString();
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