package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.network.PersonalSpaceSettingsSync;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PortalBlockEntity extends BlockEntity {
    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_TARGET_LEVEL = "TargetLevel";
    private static final String TAG_TARGET_POS = "TargetPos";
    private static final String TAG_RETURN_PORTAL = "ReturnPortal";

    private boolean active = false;
    private boolean returnPortal = false;

    private ResourceKey<Level> targetLevel = null;
    private BlockPos targetPos = new BlockPos(0, 80, 0);
    private long lastTeleportGameTime = -1000L;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(PSBlockEntities.PERSONAL_PORTAL.get(), pos, state);
    }

    public boolean isActive() {
        return active && targetLevel != null;
    }

    public boolean isReturnPortal() {
        return returnPortal;
    }

    public void setReturnPortal(boolean returnPortal) {
        this.returnPortal = returnPortal;

        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ResourceKey<Level> getTargetLevel() {
        return targetLevel;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTarget(ResourceKey<Level> targetLevel, BlockPos targetPos) {
        this.targetLevel = targetLevel;
        this.targetPos = targetPos == null ? new BlockPos(0, 80, 0) : targetPos;
        this.active = targetLevel != null;

        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void teleport(ServerPlayer player) {
        if (!isActive()) {
            player.sendSystemMessage(Component.translatable("message.personalspace.portal_not_active"));
            return;
        }

        MinecraftServer server = player.server;

        if (server == null) {
            return;
        }

        if (level != null) {
            long now = level.getGameTime();

            if (lastTeleportGameTime >= 0L && now - lastTeleportGameTime < 10L) {
                return;
            }

            lastTeleportGameTime = now;
        }

        ServerLevel destination = server.getLevel(targetLevel);

        if (destination == null
                && targetLevel.location().getNamespace().equals(PersonalSpace.MODID)) {
            destination = PSDimensions.getOrCreate(server, targetLevel);
        }

        if (destination == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.personalspace.target_dimension_not_found",
                    targetLevel.location()
            ));
            return;
        }

        BlockPos teleportPos = getActualTeleportPos(destination);

        destination.getChunkAt(teleportPos);

        player.teleportTo(
                destination,
                teleportPos.getX() + 0.5D,
                teleportPos.getY(),
                teleportPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        PersonalSpaceSettingsSync.syncTo(player, destination);
    }

    private BlockPos getActualTeleportPos(ServerLevel destination) {
        if (!returnPortal && isPersonalSpaceDimension(destination)) {
            PersonalSpaceData data = PersonalSpaceData.load(destination);
            return data.getRespawnPos();
        }

        return targetPos;
    }

    private static boolean isPersonalSpaceDimension(Level level) {
        return level.dimension().location().getNamespace().equals(PersonalSpace.MODID);
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag blockEntityTag = new CompoundTag();
        saveAdditional(blockEntityTag);
        stack.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putBoolean(TAG_ACTIVE, active);
        tag.putBoolean(TAG_RETURN_PORTAL, returnPortal);

        if (targetLevel != null) {
            tag.putString(TAG_TARGET_LEVEL, targetLevel.location().toString());
        }

        if (targetPos != null) {
            tag.putLong(TAG_TARGET_POS, targetPos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        boolean savedActive = tag.getBoolean(TAG_ACTIVE);
        boolean savedReturnPortal = tag.getBoolean(TAG_RETURN_PORTAL);

        targetLevel = null;
        targetPos = new BlockPos(0, 80, 0);
        returnPortal = savedReturnPortal;

        if (tag.contains(TAG_TARGET_LEVEL)) {
            ResourceLocation targetLocation = ResourceLocation.tryParse(
                    tag.getString(TAG_TARGET_LEVEL)
            );

            if (targetLocation != null) {
                targetLevel = ResourceKey.create(Registries.DIMENSION, targetLocation);
            }
        }

        if (tag.contains(TAG_TARGET_POS)) {
            targetPos = BlockPos.of(tag.getLong(TAG_TARGET_POS));
        }

        active = savedActive && targetLevel != null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();

        if (tag != null) {
            load(tag);
        }
    }
}