package me.eigenraven.personalspace.client;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlock;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.client.gui.PersonalSpaceScreen;
import me.eigenraven.personalspace.client.gui.PersonalSpaceSettingsScreen;
import me.eigenraven.personalspace.client.gui.PortalTeleportConfirmScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(
        modid = PersonalSpace.MODID,
        value = net.neoforged.api.distmarker.Dist.CLIENT
)
public final class PortalBlockClientEvents {
    private PortalBlockClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();

        if (!level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PortalBlock)) {
            return;
        }

        Player player = event.getEntity();

        handleClientClick(level, pos, player, state);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void handleClientClick(
            Level level,
            BlockPos pos,
            Player player,
            BlockState state
    ) {
        if (player.isShiftKeyDown() && isPersonalSpaceDimension(level)) {
            openSettingsGui(level.dimension().location());
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        boolean returnPortal = isClientReturnPortal(level, pos, state, blockEntity);
        boolean activePortal = blockEntity instanceof PortalBlockEntity portal && portal.isActive();

        if (returnPortal || activePortal) {
            openTeleportConfirmGui(level, pos, returnPortal);
            return;
        }

        openCreateGui(player, pos);
    }

    private static boolean isClientReturnPortal(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        if (state.hasProperty(PortalBlock.RETURN_PORTAL) && state.getValue(PortalBlock.RETURN_PORTAL)) {
            return true;
        }

        if (blockEntity instanceof PortalBlockEntity portal && portal.isReturnPortal()) {
            return true;
        }

        return isPersonalSpaceDimension(level) && pos.getX() == 7 && pos.getZ() == 7;
    }

    private static boolean isPersonalSpaceDimension(Level level) {
        return level.dimension().location().getNamespace().equals(PersonalSpace.MODID);
    }

    private static void openCreateGui(Player player, BlockPos pos) {
        Minecraft.getInstance().setScreen(new PersonalSpaceScreen(player.level(), pos));
    }

    private static void openSettingsGui(ResourceLocation levelId) {
        Minecraft.getInstance().setScreen(new PersonalSpaceSettingsScreen(levelId));
    }

    private static void openTeleportConfirmGui(Level level, BlockPos pos, boolean returnPortal) {
        ResourceLocation clickedLevelId = level.dimension().location();

        BlockEntity blockEntity = level.getBlockEntity(pos);

        ResourceLocation targetLevelId = null;

        if (blockEntity instanceof PortalBlockEntity portal && portal.getTargetLevel() != null) {
            targetLevelId = portal.getTargetLevel().location();
        }

        Component dimensionName;

        if (returnPortal) {
            dimensionName = Component.translatable(
                    "screen.personalspace.portal.return_to",
                    formatDimensionName(targetLevelId)
            );
        } else {
            dimensionName = Component.translatable(
                    "screen.personalspace.portal.enter_dimension",
                    formatDimensionName(targetLevelId)
            );
        }

        Minecraft.getInstance().setScreen(new PortalTeleportConfirmScreen(
                pos,
                clickedLevelId,
                dimensionName
        ));
    }

    private static String formatDimensionName(ResourceLocation levelId) {
        if (levelId == null) {
            return "Unknown";
        }

        if (levelId.getNamespace().equals("minecraft") && levelId.getPath().equals("overworld")) {
            return "Overworld";
        }

        if (levelId.getNamespace().equals(PersonalSpace.MODID)) {
            String path = levelId.getPath();

            if (path.startsWith("ps_")) {
                path = path.substring(3);
            }

            return path;
        }

        return levelId.toString();
    }
}