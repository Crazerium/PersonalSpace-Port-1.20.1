package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdatePersonalSpaceSettingsPacket {
    private final long timeOfDay;
    private final int skyRed;
    private final int skyGreen;
    private final int skyBlue;

    public UpdatePersonalSpaceSettingsPacket(
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue
    ) {
        this.timeOfDay = timeOfDay;
        this.skyRed = skyRed;
        this.skyGreen = skyGreen;
        this.skyBlue = skyBlue;
    }

    public static void encode(UpdatePersonalSpaceSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.timeOfDay);
        buf.writeInt(msg.skyRed);
        buf.writeInt(msg.skyGreen);
        buf.writeInt(msg.skyBlue);
    }

    public static UpdatePersonalSpaceSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdatePersonalSpaceSettingsPacket(
                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(
            UpdatePersonalSpaceSettingsPacket msg,
            Supplier<NetworkEvent.Context> ctx
    ) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }

            if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
                return;
            }

            PersonalSpaceData data = PersonalSpaceData.load(level);

            data.setTimeOfDay(msg.timeOfDay);
            data.setSkyColor(msg.skyRed, msg.skyGreen, msg.skyBlue);

            PersonalSpaceData.save(level, data);

            level.setDayTime(data.getTimeOfDay());

            PersonalSpaceSettingsSync.syncToPlayersIn(level);
        });

        context.setPacketHandled(true);
    }
}