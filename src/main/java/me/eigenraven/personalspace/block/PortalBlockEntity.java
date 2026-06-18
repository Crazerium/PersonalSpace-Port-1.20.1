package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalBlockEntity extends BlockEntity {
    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_TARGET_LEVEL = "TargetLevel";
    private static final String TAG_TARGET_POS = "TargetPos";

    private boolean active = false;
    private ResourceKey<Level> targetLevel = null;
    private BlockPos targetPos = new BlockPos(0, 80, 0);

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(PSBlockEntities.PERSONAL_PORTAL.get(), pos, state);
    }

    public void teleport(ServerPlayer player) {
        if (!active || targetLevel == null) {
            player.sendSystemMessage(Component.literal("Portal is not active!"));
            return;
        }

        ServerLevel serverLevel = PSDimensions.getOrCreate(player.server, targetLevel);
        // Принудительно загружаем чанк (0,0) перед телепортацией
        serverLevel.getChunk(0, 0);

        player.teleportTo(
                serverLevel,
                targetPos.getX() + 0.5D,
                targetPos.getY() + 1.0D,
                targetPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    public void setTarget(ResourceKey<Level> targetLevel, BlockPos targetPos) {
        this.targetLevel = targetLevel;
        this.targetPos = targetPos;
        this.active = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isActive() { return active; }
    public ResourceKey<Level> getTargetLevel() { return targetLevel; }

    public void saveToItem(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_ACTIVE, active);
        if (targetLevel != null) tag.putString(TAG_TARGET_LEVEL, targetLevel.location().toString());
        tag.putLong(TAG_TARGET_POS, targetPos.asLong());
        stack.getOrCreateTag().put("BlockEntityTag", tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(TAG_ACTIVE, active);
        if (targetLevel != null) tag.putString(TAG_TARGET_LEVEL, targetLevel.location().toString());
        tag.putLong(TAG_TARGET_POS, targetPos.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean(TAG_ACTIVE);
        if (tag.contains(TAG_TARGET_LEVEL)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_TARGET_LEVEL));
            if (id != null) targetLevel = ResourceKey.create(Registries.DIMENSION, id);
        }
        if (tag.contains(TAG_TARGET_POS)) {
            targetPos = BlockPos.of(tag.getLong(TAG_TARGET_POS));
        }
    }
}