package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.block.PortalBlock;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.registry.PSBlocks;
import net.minecraft.core.BlockPos;
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
            boolean centerMarkerEnabled
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

                buf.readBoolean()
        );
    }

    public static void handle(CreateDimensionPacket msg, Supplier<NetworkEvent.Context> ctx) {
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

        ResourceKey<Level> newLevelKey = PSDimensions.randomPersonalKey();

        ServerLevel newLevel = PSDimensions.createPersonalDimension(
                server,
                newLevelKey,
                msg.type,
                msg.height,
                msg.biomeName
        );

        PersonalSpaceData data = PersonalSpaceData.load(newLevel);

        data.setType(msg.type);
        data.setGroundLevel(msg.height);

        data.setReturnLevel(sourceLevel.dimension().location().toString());
        data.setReturnPos(msg.sourcePortalPos);

        data.setTimeOfDay(msg.timeOfDay);
        data.setSkyColor(msg.skyRed, msg.skyGreen, msg.skyBlue);

        data.setStarBrightness(msg.starBrightness);
        data.setBiomeName(msg.biomeName);

        data.setTreesEnabled(msg.treesEnabled);
        data.setFoliageEnabled(msg.foliageEnabled);
        data.setWeatherEnabled(msg.weatherEnabled);
        data.setCloudsEnabled(msg.cloudsEnabled);

        data.setLayersPreset(msg.layersPreset);
        data.setBoundaryChunksX(msg.boundaryChunksX);
        data.setBoundaryChunksZ(msg.boundaryChunksZ);
        data.setGapChunks(msg.gapChunks);

        data.setBoundaryBlock(msg.boundaryBlock);
        data.setRoadBlock(msg.roadBlock);
        data.setCenterMarkerBlock(msg.centerMarkerBlock);

        data.setCenterMarkerEnabled(msg.centerMarkerEnabled);

        PersonalSpaceData.save(newLevel, data);

        newLevel.setDayTime(data.getTimeOfDay());

        int groundY = data.getGroundLevel();
        BlockPos innerPortalPos = new BlockPos(7, groundY + 1, 7);

        PSDimensions.prepareSpawnArea(
                newLevel,
                data.getType(),
                groundY,
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

    private static ServerLevel getSourceLevel(CreateDimensionPacket msg, ServerPlayer player) {
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