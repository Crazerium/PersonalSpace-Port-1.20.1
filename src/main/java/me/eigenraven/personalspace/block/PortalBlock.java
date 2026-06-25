package me.eigenraven.personalspace.block;

import com.mojang.serialization.MapCodec;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class PortalBlock extends BaseEntityBlock {
    public static final MapCodec<PortalBlock> CODEC = simpleCodec(properties -> new PortalBlock());

    public static final BooleanProperty RETURN_PORTAL = BooleanProperty.create("return_portal");

    public PortalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0F, 1200.0F)
                .noOcclusion()
                .lightLevel(state -> 8));

        registerDefaultState(stateDefinition.any()
                .setValue(RETURN_PORTAL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RETURN_PORTAL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (serverPlayer.isShiftKeyDown() && isPersonalSpaceDimension(serverLevel)) {
                return InteractionResult.CONSUME;
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (serverPlayer.isShiftKeyDown() && isPersonalSpaceDimension(serverLevel)) {
                return ItemInteractionResult.CONSUME;
            }

            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static void handlePortalUseOnServer(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player
    ) {
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PortalBlock)) {
            return;
        }

        PortalBlockEntity portal = getOrCreatePortalBlockEntity(level, pos, state);

        boolean returnPortal = isServerReturnPortal(level, pos, state, portal);

        if (returnPortal) {
            if (state.hasProperty(RETURN_PORTAL) && !state.getValue(RETURN_PORTAL)) {
                level.setBlock(pos, state.setValue(RETURN_PORTAL, true), 3);
            }

            portal.setReturnPortal(true);

            if (restoreReturnTargetFromData(level, portal)) {
                portal.teleport(player);
            }

            return;
        }

        if (portal.isActive()) {
            portal.teleport(player);
        }
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

    private static boolean isServerReturnPortal(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            PortalBlockEntity portal
    ) {
        if (!isPersonalSpaceDimension(level)) {
            return false;
        }

        if (state.hasProperty(RETURN_PORTAL) && state.getValue(RETURN_PORTAL)) {
            return true;
        }

        if (portal.isReturnPortal()) {
            return true;
        }

        if (pos.getX() != 7 || pos.getZ() != 7) {
            return false;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        String returnLevel = data.getReturnLevel();

        return returnLevel != null && !returnLevel.isBlank();
    }

    private static boolean restoreReturnTargetFromData(
            ServerLevel personalLevel,
            PortalBlockEntity portal
    ) {
        PersonalSpaceData data = PersonalSpaceData.load(personalLevel);
        String returnLevelId = data.getReturnLevel();

        if (returnLevelId == null || returnLevelId.isBlank()) {
            return false;
        }

        ResourceLocation returnLocation = ResourceLocation.tryParse(returnLevelId);

        if (returnLocation == null) {
            return false;
        }

        ResourceKey<Level> returnKey = ResourceKey.create(
                Registries.DIMENSION,
                returnLocation
        );

        portal.setTarget(returnKey, data.getReturnPos());
        return true;
    }

    private static boolean isPersonalSpaceDimension(Level level) {
        return PSDimensions.isPersonalSpaceDimension(level.dimension().location());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof PortalBlockEntity portal) {
            ItemStack stack = new ItemStack(PSItems.PERSONAL_PORTAL.get());
            portal.saveToItem(stack);
            return List.of(stack);
        }

        return super.getDrops(state, params);
    }

    @Override
    protected void onRemove(
            BlockState oldState,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean moving
    ) {
        if (!oldState.is(newState.getBlock())) {
            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }
}