package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.client.gui.PersonalSpaceScreen;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

public final class PortalBlock extends BaseEntityBlock {
    public PortalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(25.0F, 3_600_000.0F)
                .noOcclusion()
                .lightLevel(state -> 8));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PortalBlockEntity portal && !portal.isActive()) {
                openGui(player, pos);
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PortalBlockEntity portal && player instanceof ServerPlayer serverPlayer) {
            if (portal.isActive() && portal.getTargetLevel() != null) {
                portal.teleport(serverPlayer);
                return InteractionResult.CONSUME;
            } else {
                serverPlayer.sendSystemMessage(Component.literal("Portal is not active! Use GUI to create a world."));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @OnlyIn(Dist.CLIENT)
    private void openGui(Player player, BlockPos pos) {
        if (player instanceof net.minecraft.client.player.LocalPlayer) {
            Minecraft.getInstance().setScreen(new PersonalSpaceScreen(player.level(), pos));
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PortalBlockEntity portal) {
            ItemStack stack = new ItemStack(PSItems.PERSONAL_PORTAL.get());
            portal.saveToItem(stack);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) level.removeBlockEntity(pos);
        super.onRemove(oldState, level, pos, newState, moving);
    }
}