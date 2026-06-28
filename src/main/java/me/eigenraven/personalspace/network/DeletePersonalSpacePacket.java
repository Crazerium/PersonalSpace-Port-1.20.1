package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.dimension.PersonalSpaceDeletionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeletePersonalSpacePacket {
    private final ResourceLocation levelId;

    public DeletePersonalSpacePacket(ResourceLocation levelId) {
        this.levelId = levelId;
    }

    public static void encode(DeletePersonalSpacePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.levelId);
    }

    public static DeletePersonalSpacePacket decode(FriendlyByteBuf buffer) {
        return new DeletePersonalSpacePacket(buffer.readResourceLocation());
    }

    public static void handle(DeletePersonalSpacePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            PersonalSpaceDeletionManager.requestDeleteFromButton(player, packet.levelId);
        });

        context.setPacketHandled(true);
    }
}