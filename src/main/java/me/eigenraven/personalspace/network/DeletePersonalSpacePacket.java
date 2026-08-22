package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PersonalSpaceDeletionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DeletePersonalSpacePacket implements CustomPacketPayload {
    public static final Type<DeletePersonalSpacePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, "delete_personal_space")
    );

    public static final StreamCodec<FriendlyByteBuf, DeletePersonalSpacePacket> STREAM_CODEC =
            StreamCodec.ofMember(DeletePersonalSpacePacket::encode, DeletePersonalSpacePacket::decode);

    private final ResourceLocation levelId;

    public DeletePersonalSpacePacket(ResourceLocation levelId) {
        this.levelId = levelId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(levelId);
    }

    public static DeletePersonalSpacePacket decode(FriendlyByteBuf buffer) {
        return new DeletePersonalSpacePacket(buffer.readResourceLocation());
    }

    public void handleServerSide(ServerPlayer player) {
        PersonalSpaceDeletionManager.requestDeleteFromButton(player, levelId);
    }
}
