package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.api.cluster.PersonalSpaceClusterApi;
import me.eigenraven.personalspace.api.cluster.PersonalSpaceClusterResult;
import me.eigenraven.personalspace.api.cluster.PersonalSpacePortalTravelContext;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.network.PersonalSpaceSettingsSync;
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
import net.minecraft.server.MinecraftServer;
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
    private static final String TAG_TARGET_DISPLAY_NAME = "TargetDisplayName";

    private boolean active;
    private boolean returnPortal;
    private ResourceKey<Level> targetLevel;
    private BlockPos targetPos = new BlockPos(0, 80, 0);
    private String targetDisplayName = "";
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

    public ResourceKey<Level> getTargetLevel() {
        return targetLevel;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public String getTargetDisplayName() {
        return targetDisplayName == null ? "" : targetDisplayName;
    }

    public void setReturnPortal(boolean returnPortal) {
        this.returnPortal = returnPortal;
        setChangedAndSync();
    }

    public void setTarget(ResourceKey<Level> targetLevel, BlockPos targetPos) {
        setTarget(targetLevel, targetPos, "");
    }

    public void setTarget(ResourceKey<Level> targetLevel, BlockPos targetPos, String targetDisplayName) {
        this.targetLevel = targetLevel;
        this.targetPos = targetPos == null ? new BlockPos(0, 80, 0) : targetPos;
        this.targetDisplayName = targetDisplayName == null ? "" : targetDisplayName;
        this.active = targetLevel != null;
        setChangedAndSync();
    }

    public void clearTarget() {
        this.active = false;
        this.returnPortal = false;
        this.targetLevel = null;
        this.targetPos = new BlockPos(0, 80, 0);
        this.targetDisplayName = "";
        setChangedAndSync();
    }

    public void teleport(ServerPlayer player) {
        if (!isActive()) {
            player.sendSystemMessage(Component.translatable("message.personalspace.portal_not_active"));
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (level != null) {
            long now = level.getGameTime();
            if (lastTeleportGameTime >= 0L && now - lastTeleportGameTime < 10L) return;
            lastTeleportGameTime = now;
        }

        PersonalSpaceClusterResult result = PersonalSpaceClusterApi.handlePortalTravel(
                new PersonalSpacePortalTravelContext(
                        server,
                        player,
                        targetLevel,
                        targetPos,
                        player.getYRot(),
                        player.getXRot(),
                        () -> teleportLocally(player)
                )
        );

        if (result != PersonalSpaceClusterResult.HANDLED) {
            teleportLocally(player);
        }
    }

    private void teleportLocally(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || targetLevel == null) return;

        ServerLevel destination = server.getLevel(targetLevel);
        if (destination == null && PersonalSpaceClusterApi.isPersonalSpaceDimension(targetLevel.location())) {
            destination = PersonalSpaceClusterApi.loadDimension(server, targetLevel);
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
            return findTeleportPosForPersonalSpace(destination, data.getRespawnPos());
        }
        return findTeleportPosAbovePortal(destination, targetPos);
    }

    private static BlockPos findTeleportPosForPersonalSpace(ServerLevel level, BlockPos respawnPos) {
        BlockPos aboveRespawn = respawnPos.above();
        if (hasFreePlayerSpace(level, aboveRespawn)) return aboveRespawn;

        for (int offsetY = 1; offsetY <= 8; offsetY++) {
            BlockPos pos = aboveRespawn.above(offsetY);
            if (hasFreePlayerSpace(level, pos)) return pos;
        }
        for (int offsetY = 1; offsetY <= 8; offsetY++) {
            BlockPos pos = aboveRespawn.below(offsetY);
            if (hasFreePlayerSpace(level, pos)) return pos;
        }
        return aboveRespawn;
    }

    private static BlockPos findTeleportPosAbovePortal(ServerLevel level, BlockPos portalPos) {
        BlockPos safePortalPos = portalPos == null ? new BlockPos(0, 80, 0) : portalPos;
        BlockPos basePos = safePortalPos.above();
        for (int offsetY = 0; offsetY <= 6; offsetY++) {
            BlockPos pos = basePos.above(offsetY);
            if (hasFreePlayerSpace(level, pos)) return pos;
        }
        for (int offsetY = 1; offsetY <= 3; offsetY++) {
            BlockPos pos = basePos.below(offsetY);
            if (hasFreePlayerSpace(level, pos)) return pos;
        }
        return basePos;
    }

    private static boolean hasFreePlayerSpace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static boolean isPersonalSpaceDimension(Level level) {
        return level.dimension().location().getNamespace().equals(PersonalSpace.MODID);
    }

    public void saveToItem(ItemStack stack) {
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createBlockEntityDataTag()));
    }

    private CompoundTag createBlockEntityDataTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, "personal_portal").toString());
        writePortalData(tag);
        return tag;
    }

    private void writePortalData(CompoundTag tag) {
        tag.putBoolean(TAG_ACTIVE, active);
        tag.putBoolean(TAG_RETURN_PORTAL, returnPortal);
        if (targetLevel != null) tag.putString(TAG_TARGET_LEVEL, targetLevel.location().toString());
        if (targetDisplayName != null && !targetDisplayName.isBlank()) tag.putString(TAG_TARGET_DISPLAY_NAME, targetDisplayName);
        if (targetPos != null) tag.putLong(TAG_TARGET_POS, targetPos.asLong());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writePortalData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        boolean savedActive = tag.getBoolean(TAG_ACTIVE);
        returnPortal = tag.getBoolean(TAG_RETURN_PORTAL);
        targetLevel = null;
        targetPos = new BlockPos(0, 80, 0);
        targetDisplayName = "";

        if (tag.contains(TAG_TARGET_LEVEL)) {
            ResourceLocation targetLocation = ResourceLocation.tryParse(tag.getString(TAG_TARGET_LEVEL));
            if (targetLocation != null) targetLevel = ResourceKey.create(Registries.DIMENSION, targetLocation);
        }
        if (tag.contains(TAG_TARGET_POS)) targetPos = BlockPos.of(tag.getLong(TAG_TARGET_POS));
        if (tag.contains(TAG_TARGET_DISPLAY_NAME)) targetDisplayName = tag.getString(TAG_TARGET_DISPLAY_NAME);
        active = savedActive && targetLevel != null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writePortalData(tag);
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
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) loadAdditional(tag, registries);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }
}
