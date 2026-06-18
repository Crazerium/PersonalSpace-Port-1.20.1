package me.eigenraven.personalspace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPersonalSpaceSettingsPacket {
    private final ResourceLocation levelId;
    private final long timeOfDay;
    private final int skyRed;
    private final int skyGreen;
    private final int skyBlue;

    public SyncPersonalSpaceSettingsPacket(
            ResourceLocation levelId,
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue
    ) {
        this.levelId = levelId;
        this.timeOfDay = timeOfDay;
        this.skyRed = skyRed;
        this.skyGreen = skyGreen;
        this.skyBlue = skyBlue;
    }

    public static void encode(SyncPersonalSpaceSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.levelId);
        buf.writeLong(msg.timeOfDay);
        buf.writeInt(msg.skyRed);
        buf.writeInt(msg.skyGreen);
        buf.writeInt(msg.skyBlue);
    }

    public static SyncPersonalSpaceSettingsPacket decode(FriendlyByteBuf buf) {
        return new SyncPersonalSpaceSettingsPacket(
                buf.readResourceLocation(),
                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(
            SyncPersonalSpaceSettingsPacket msg,
            Supplier<NetworkEvent.Context> ctx
    ) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> me.eigenraven.personalspace.client.ClientPersonalSpaceSettings.set(
                        msg.levelId,
                        msg.timeOfDay,
                        msg.skyRed,
                        msg.skyGreen,
                        msg.skyBlue
                )
        ));

        context.setPacketHandled(true);
    }
}