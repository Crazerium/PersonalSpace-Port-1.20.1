package me.eigenraven.personalspace.client;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlock;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.client.gui.PersonalSpaceScreen;
import me.eigenraven.personalspace.client.gui.PersonalSpaceSettingsScreen;
import me.eigenraven.personalspace.client.gui.PortalTeleportConfirmScreen;
import me.eigenraven.personalspace.dimension.PSDimensions;
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
    private PortalBlockClientEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PortalBlock)) return;

        handleClientClick(level, pos, event.getEntity(), state);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void handleClientClick(Level level, BlockPos pos, Player player, BlockState state) {
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

    private static boolean isClientReturnPortal(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (state.hasProperty(PortalBlock.RETURN_PORTAL) && state.getValue(PortalBlock.RETURN_PORTAL)) return true;
        if (blockEntity instanceof PortalBlockEntity portal && portal.isReturnPortal()) return true;
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
        String targetDisplayName = "";
        if (blockEntity instanceof PortalBlockEntity portal) {
            if (portal.getTargetLevel() != null) targetLevelId = portal.getTargetLevel().location();
            targetDisplayName = portal.getTargetDisplayName();
        }

        Component dimensionName = returnPortal
                ? Component.translatable("screen.personalspace.portal.return_to", formatDimensionName(targetLevelId))
                : formatTargetDisplayName(targetLevelId, targetDisplayName);

        Minecraft.getInstance().setScreen(new PortalTeleportConfirmScreen(pos, clickedLevelId, dimensionName));
    }

    private static Component formatTargetDisplayName(ResourceLocation levelId, String targetDisplayName) {
        if (targetDisplayName != null && targetDisplayName.startsWith("team:")) {
            String teamName = targetDisplayName.substring("team:".length()).trim();
            if (!teamName.isBlank()) {
                return Component.translatable("screen.personalspace.portal.dimension.team_named", teamName);
            }
            return Component.translatable("screen.personalspace.portal.dimension.team_unknown");
        }
        if (targetDisplayName != null && targetDisplayName.startsWith("player:")) {
            String playerName = targetDisplayName.substring("player:".length()).trim();
            if (!playerName.isBlank()) {
                return Component.translatable("screen.personalspace.portal.dimension.personal_named", playerName);
            }
            return Component.translatable("screen.personalspace.portal.dimension.personal_unknown");
        }
        return formatDimensionName(levelId);
    }

    private static Component formatDimensionName(ResourceLocation levelId) {
        if (levelId == null) return Component.translatable("screen.personalspace.portal.dimension.unknown");
        if (levelId.getNamespace().equals("minecraft") && levelId.getPath().equals("overworld")) {
            return Component.translatable("screen.personalspace.portal.dimension.overworld");
        }
        if (levelId.getNamespace().equals(PersonalSpace.MODID)) {
            String path = levelId.getPath();
            String prefix = PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/";
            if (path.startsWith(prefix)) path = path.substring(prefix.length());
            if (path.startsWith("team_")) return Component.translatable("screen.personalspace.portal.dimension.team_unknown");
            if (path.startsWith("ps_")) {
                String playerName = path.substring(3);
                if (!playerName.isBlank()) {
                    return Component.translatable("screen.personalspace.portal.dimension.personal_named", playerName);
                }
                return Component.translatable("screen.personalspace.portal.dimension.personal_unknown");
            }
            return Component.literal(path);
        }
        return Component.literal(levelId.toString());
    }
}
