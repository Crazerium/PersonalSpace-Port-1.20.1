package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PortalBlockEntity extends BlockEntity {
    private static final String TAG_ID = "id";
    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_RETURN_PORTAL = "ReturnPortal";
    private static final String TAG_TARGET_LEVEL = "TargetLevel";
    private static final String TAG_TARGET_POS = "TargetPos";

    private boolean active = false;
    private boolean returnPortal = false;
    private ResourceKey<Level> targetLevel = null;
    private BlockPos targetPos = BlockPos.ZERO;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(PSBlockEntities.PERSONAL_PORTAL.get(), pos, state);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isReturnPortal() {
        return returnPortal;
    }

    public ResourceKey<Level> getTargetLevel() {
        return targetLevel;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setReturnPortal(boolean returnPortal) {
        this.returnPortal = returnPortal;
        setChangedAndSync();
    }

    public void setTarget(ResourceKey<Level> targetLevel, BlockPos targetPos) {
        this.active = targetLevel != null;
        this.targetLevel = targetLevel;
        this.targetPos = targetPos == null ? BlockPos.ZERO : targetPos;
        setChangedAndSync();
    }

    public void clearTarget() {
        this.active = false;
        this.targetLevel = null;
        this.targetPos = BlockPos.ZERO;
        setChangedAndSync();
    }

    public void teleport(ServerPlayer player) {
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("Portal has no target dimension."));
            return;
        }

        ServerLevel destination = player.server.getLevel(targetLevel);

        if (destination == null) {
            player.sendSystemMessage(Component.literal("Target dimension does not exist: " + targetLevel.location()));
            return;
        }

        BlockPos destinationPos = targetPos == null ? BlockPos.ZERO : targetPos;

        destination.getChunkAt(destinationPos);

        player.teleportTo(
                destination,
                destinationPos.getX() + 0.5D,
                destinationPos.getY() + 1.0D,
                destinationPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag blockEntityTag = createBlockEntityDataTag();
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
    }

    private CompoundTag createBlockEntityDataTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString(
                TAG_ID,
                ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, "personal_portal").toString()
        );

        tag.putBoolean(TAG_ACTIVE, active);
        tag.putBoolean(TAG_RETURN_PORTAL, returnPortal);

        if (targetLevel != null) {
            tag.putString(TAG_TARGET_LEVEL, targetLevel.location().toString());
        }

        tag.putLong(TAG_TARGET_POS, targetPos.asLong());

        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putBoolean(TAG_ACTIVE, active);
        tag.putBoolean(TAG_RETURN_PORTAL, returnPortal);

        if (targetLevel != null) {
            tag.putString(TAG_TARGET_LEVEL, targetLevel.location().toString());
        }

        tag.putLong(TAG_TARGET_POS, targetPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        active = tag.getBoolean(TAG_ACTIVE);
        returnPortal = tag.getBoolean(TAG_RETURN_PORTAL);

        if (tag.contains(TAG_TARGET_LEVEL)) {
            ResourceLocation targetLocation = ResourceLocation.tryParse(tag.getString(TAG_TARGET_LEVEL));

            if (targetLocation != null) {
                targetLevel = ResourceKey.create(
                        Registries.DIMENSION,
                        targetLocation
                );
            } else {
                targetLevel = null;
                active = false;
            }
        } else {
            targetLevel = null;
            active = false;
        }

        if (tag.contains(TAG_TARGET_POS)) {
            targetPos = BlockPos.of(tag.getLong(TAG_TARGET_POS));
        } else {
            targetPos = BlockPos.ZERO;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = packet.getTag();

        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    private void setChangedAndSync() {
        setChanged();

        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }
}