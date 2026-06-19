package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.client.ClientPersonalSpaceSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPersonalSpaceSettingsPacket {
    private final ResourceLocation levelId;

    private final long timeOfDay;
    private final int skyRed;
    private final int skyGreen;
    private final int skyBlue;

    private final float starBrightness;
    private final String biomeName;

    private final boolean treesEnabled;
    private final boolean foliageEnabled;
    private final boolean weatherEnabled;
    private final boolean cloudsEnabled;

    private final String layersPreset;

    public SyncPersonalSpaceSettingsPacket(
            ResourceLocation levelId,
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue,
            float starBrightness,
            String biomeName,
            boolean treesEnabled,
            boolean foliageEnabled,
            boolean weatherEnabled,
            boolean cloudsEnabled,
            String layersPreset
    ) {
        this.levelId = levelId;
        this.timeOfDay = timeOfDay;
        this.skyRed = skyRed;
        this.skyGreen = skyGreen;
        this.skyBlue = skyBlue;
        this.starBrightness = starBrightness;
        this.biomeName = biomeName;
        this.treesEnabled = treesEnabled;
        this.foliageEnabled = foliageEnabled;
        this.weatherEnabled = weatherEnabled;
        this.cloudsEnabled = cloudsEnabled;
        this.layersPreset = layersPreset;
    }

    public static void encode(SyncPersonalSpaceSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.levelId);

        buf.writeLong(msg.timeOfDay);
        buf.writeInt(msg.skyRed);
        buf.writeInt(msg.skyGreen);
        buf.writeInt(msg.skyBlue);

        buf.writeFloat(msg.starBrightness);
        buf.writeUtf(msg.biomeName);

        buf.writeBoolean(msg.treesEnabled);
        buf.writeBoolean(msg.foliageEnabled);
        buf.writeBoolean(msg.weatherEnabled);
        buf.writeBoolean(msg.cloudsEnabled);

        buf.writeUtf(msg.layersPreset);
    }

    public static SyncPersonalSpaceSettingsPacket decode(FriendlyByteBuf buf) {
        return new SyncPersonalSpaceSettingsPacket(
                buf.readResourceLocation(),

                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),

                buf.readFloat(),
                buf.readUtf(),

                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),

                buf.readUtf()
        );
    }

    public static void handle(
            SyncPersonalSpaceSettingsPacket msg,
            Supplier<NetworkEvent.Context> ctx
    ) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> ClientPersonalSpaceSettings.set(
                msg.levelId,
                msg.timeOfDay,
                msg.skyRed,
                msg.skyGreen,
                msg.skyBlue,
                msg.starBrightness,
                msg.biomeName,
                msg.treesEnabled,
                msg.foliageEnabled,
                msg.weatherEnabled,
                msg.cloudsEnabled,
                msg.layersPreset
        ));

        context.setPacketHandled(true);
    }
}