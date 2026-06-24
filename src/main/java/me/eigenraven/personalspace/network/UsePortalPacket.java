package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.block.PortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class UsePortalPacket {
    private final BlockPos portalPos;
    private final ResourceLocation sourceLevelId;

    public UsePortalPacket(BlockPos portalPos, ResourceLocation sourceLevelId) {
        this.portalPos = portalPos;
        this.sourceLevelId = sourceLevelId;
    }

    public static void encode(UsePortalPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.portalPos);
        buf.writeResourceLocation(msg.sourceLevelId);
    }

    public static UsePortalPacket decode(FriendlyByteBuf buf) {
        return new UsePortalPacket(
                buf.readBlockPos(),
                buf.readResourceLocation()
        );
    }

    public void handleServerSide(ServerPlayer player) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return;
        }

        ResourceKey<Level> sourceKey = ResourceKey.create(
                Registries.DIMENSION,
                sourceLevelId
        );

        ServerLevel sourceLevel = server.getLevel(sourceKey);

        if (sourceLevel == null && player.level() instanceof ServerLevel currentLevel) {
            sourceLevel = currentLevel;
        }

        if (sourceLevel == null) {
            return;
        }

        PortalBlock.handlePortalUseOnServer(
                sourceLevel,
                portalPos,
                player
        );
    }

    public BlockPos portalPos() {
        return portalPos;
    }

    public ResourceLocation sourceLevelId() {
        return sourceLevelId;
    }
}