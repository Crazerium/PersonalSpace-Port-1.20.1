package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class PersonalSpaceSettingsSync {
    private PersonalSpaceSettingsSync() {
    }

    public static void syncTo(ServerPlayer player, ServerLevel level) {
        if (!isPersonalSpace(level)) {
            return;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);

        level.setDayTime(data.getTimeOfDay());

        if (!data.isWeatherEnabled()) {
            level.setWeatherParameters(6000, 0, false, false);
        }

        PersonalSpace.LOGGER.debug(
                "Personal Space settings sync packet is not implemented yet. Player={}, level={}",
                player.getGameProfile().getName(),
                level.dimension().location()
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