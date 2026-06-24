package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PSNetwork {
    public static final String VERSION = "1";

    private PSNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(
                UsePortalPacket.TYPE,
                UsePortalPacket.STREAM_CODEC,
                PSNetwork::handleUsePortal
        );

        registrar.playToServer(
                UpdatePersonalSpaceSettingsPacket.TYPE,
                UpdatePersonalSpaceSettingsPacket.STREAM_CODEC,
                PSNetwork::handleUpdatePersonalSpaceSettings
        );

        registrar.playToServer(
                CreateDimensionPacket.TYPE,
                CreateDimensionPacket.STREAM_CODEC,
                PSNetwork::handleCreateDimension
        );

        registrar.playToClient(
                SyncPersonalSpaceSettingsPacket.TYPE,
                SyncPersonalSpaceSettingsPacket.STREAM_CODEC,
                PSNetwork::handleSyncPersonalSpaceSettings
        );
    }

    private static void handleUsePortal(UsePortalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                packet.handleServerSide(player);
            }
        }).exceptionally(exception -> {
            PersonalSpace.LOGGER.error("Failed to handle UsePortalPacket", exception);
            return null;
        });
    }

    private static void handleUpdatePersonalSpaceSettings(
            UpdatePersonalSpaceSettingsPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                packet.handleServerSide(player);
            }
        }).exceptionally(exception -> {
            PersonalSpace.LOGGER.error("Failed to handle UpdatePersonalSpaceSettingsPacket", exception);
            return null;
        });
    }

    private static void handleCreateDimension(
            CreateDimensionPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                packet.handleServerSide(player);
            }
        }).exceptionally(exception -> {
            PersonalSpace.LOGGER.error("Failed to handle CreateDimensionPacket", exception);
            return null;
        });
    }

    private static void handleSyncPersonalSpaceSettings(
            SyncPersonalSpaceSettingsPacket packet,
            IPayloadContext context
    ) {
        context.enqueueWork(packet::handleClientSide).exceptionally(exception -> {
            PersonalSpace.LOGGER.error("Failed to handle SyncPersonalSpaceSettingsPacket", exception);
            return null;
        });
    }
}