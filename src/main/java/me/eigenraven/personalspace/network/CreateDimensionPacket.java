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

    public CreateDimensionPacket(
            PersonalSpaceData.WorldType type,
            int height,
            BlockPos sourcePortalPos
    ) {
        this(type, height, sourcePortalPos, null);
    }

    public CreateDimensionPacket(
            PersonalSpaceData.WorldType type,
            int height,
            BlockPos sourcePortalPos,
            ResourceLocation sourceLevelId
    ) {
        this.type = type;
        this.height = height;
        this.sourcePortalPos = sourcePortalPos;
        this.sourceLevelId = sourceLevelId;
    }

    public static void encode(CreateDimensionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeInt(msg.height);
        buf.writeBlockPos(msg.sourcePortalPos);

        buf.writeBoolean(msg.sourceLevelId != null);

        if (msg.sourceLevelId != null) {
            buf.writeResourceLocation(msg.sourceLevelId);
        }
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
                sourceLevelId
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
                msg.height
        );
        PersonalSpaceData data = PersonalSpaceData.load(newLevel);
        data.setReturnLevel(sourceLevel.dimension().location().toString());
        data.setReturnPos(msg.sourcePortalPos);
        PersonalSpaceData.save(newLevel, data);

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