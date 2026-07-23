package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.data.PersonalSpaceRuntimeSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PersonalSpaceSettingsSync {
    private PersonalSpaceSettingsSync() {
    }

    public static void syncTo(ServerPlayer player, ServerLevel level) {
        if (!isPersonalSpace(level)) {
            return;
        }
        PersonalSpaceData data = PersonalSpaceData.load(level);
        PersonalSpaceRuntimeSettings.setTimeOfDay(
                level,
                data.getTimeOfDay()
        );
        if (!data.isWeatherEnabled()) {
            level.setWeatherParameters(6000, 0, false, false);
        }
        PersonalSpace.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPersonalSpaceSettingsPacket(
                        level.dimension().location(),
                        data.getTimeOfDay(),
                        data.getSkyRed(),
                        data.getSkyGreen(),
                        data.getSkyBlue(),
                        data.getStarBrightness(),
                        data.getBiomeName(),
                        data.isTreesEnabled(),
                        data.isFoliageEnabled(),
                        data.isWeatherEnabled(),
                        data.isCloudsEnabled(),
                        data.getLayersPreset()
                )
        );
    }

    public static void syncToPlayersIn(ServerLevel level) {
        if (!isPersonalSpace(level)) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            syncTo(player, level);
        }
    }

    private static boolean isPersonalSpace(ServerLevel level) {
        return level.dimension().location().getNamespace().equals(PersonalSpace.MODID);
    }
}