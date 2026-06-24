package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(
        modid = PersonalSpace.MODID
)
public final class PersonalSpaceLimitEvents {
    private PersonalSpaceLimitEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        int maxSizeBlocks = PSConfig.SERVER.maxPersonalSpaceSizeBlocks.get();

        if (maxSizeBlocks <= 0) {
            resetWorldBorder(level);
            syncWorldBorderToPlayer(player, level);
            return;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);

        int centerX = 7;
        int centerZ = 7;

        if (data.isRepeatingGridEnabled()) {
            int plotBlocksX = Math.max(1, data.getBoundaryChunksX()) * 16;
            int plotBlocksZ = Math.max(1, data.getBoundaryChunksZ()) * 16;

            centerX = data.getRepeatingGridOriginX() + plotBlocksX / 2;
            centerZ = data.getRepeatingGridOriginZ() + plotBlocksZ / 2;
        }

        applyPersonalSpaceWorldBorder(level, centerX, centerZ, maxSizeBlocks);
        syncWorldBorderToPlayer(player, level);
        keepPlayerInsideBorder(player, level, centerX, centerZ, maxSizeBlocks, data);
    }

    private static void applyPersonalSpaceWorldBorder(
            ServerLevel level,
            int centerX,
            int centerZ,
            int maxSizeBlocks
    ) {
        WorldBorder border = level.getWorldBorder();

        double borderCenterX = centerX + 0.5D;
        double borderCenterZ = centerZ + 0.5D;

        if (Math.abs(border.getCenterX() - borderCenterX) > 0.01D
                || Math.abs(border.getCenterZ() - borderCenterZ) > 0.01D) {
            border.setCenter(borderCenterX, borderCenterZ);
        }

        if (Math.abs(border.getSize() - maxSizeBlocks) > 0.01D) {
            border.setSize(maxSizeBlocks);
        }

        int warningBlocks = Math.min(maxSizeBlocks / 2, 256);

        border.setWarningBlocks(warningBlocks);
        border.setWarningTime(15);
        border.setDamageSafeZone(0.0D);
        border.setDamagePerBlock(0.2D);
    }

    private static void syncWorldBorderToPlayer(
            ServerPlayer player,
            ServerLevel level
    ) {
        player.connection.send(
                new ClientboundInitializeBorderPacket(level.getWorldBorder())
        );
    }

    private static void keepPlayerInsideBorder(
            ServerPlayer player,
            ServerLevel level,
            int centerX,
            int centerZ,
            int maxSizeBlocks,
            PersonalSpaceData data
    ) {
        double halfSize = maxSizeBlocks / 2.0D;

        double minX = centerX + 0.5D - halfSize + 1.0D;
        double maxX = centerX + 0.5D + halfSize - 1.0D;
        double minZ = centerZ + 0.5D - halfSize + 1.0D;
        double maxZ = centerZ + 0.5D + halfSize - 1.0D;

        boolean outside =
                player.getX() < minX ||
                        player.getX() > maxX ||
                        player.getZ() < minZ ||
                        player.getZ() > maxZ;

        if (!outside) {
            return;
        }

        double safeX = Mth.clamp(player.getX(), minX, maxX);
        double safeZ = Mth.clamp(player.getZ(), minZ, maxZ);

        player.teleportTo(
                level,
                safeX,
                data.getGroundLevel() + 2.0D,
                safeZ,
                player.getYRot(),
                player.getXRot()
        );

        player.displayClientMessage(
                Component.translatable("message.personalspace.boundary_limit"),
                true
        );
    }

    private static void resetWorldBorder(ServerLevel level) {
        WorldBorder border = level.getWorldBorder();

        border.setCenter(0.0D, 0.0D);
        border.setSize(59999968.0D);
        border.setWarningBlocks(5);
        border.setWarningTime(15);
        border.setDamageSafeZone(5.0D);
        border.setDamagePerBlock(0.2D);
    }
}