package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceRuntimeSettings;
import me.eigenraven.personalspace.network.PersonalSpaceSettingsSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;

public final class PSWorldRules {
    private PSWorldRules() {
    }

    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (isPersonalSpace(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    public static void onMobSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (isPersonalSpace(event.getLevel())) {
            event.setResult(Event.Result.DENY);
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!isPersonalSpace(event.getLevel())) {
            return;
        }

        // All mobs are banned in PD
        // if (event.getEntity() instanceof Mob) {
        //     event.setCanceled(true);
        // }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level() instanceof ServerLevel level) {
            PersonalSpaceSettingsSync.syncTo(player, level);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level() instanceof ServerLevel level) {
            PersonalSpaceSettingsSync.syncTo(player, level);
        }
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!isPersonalSpace(level)) {
            return;
        }

        long fixedTime = PersonalSpaceRuntimeSettings.getTimeOfDay(level);
        level.setDayTime(fixedTime);
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!isPersonalSpace(level)) {
            return;
        }

        PersonalSpaceRuntimeSettings.forget(level);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Раз в секунду. Не каждый тик.
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!isPersonalSpace(level)) {
                continue;
            }

            long fixedTime = PersonalSpaceRuntimeSettings.getTimeOfDay(level);
            long currentTime = level.getDayTime() % 24000L;

            if (currentTime < 0L) {
                currentTime += 24000L;
            }

            if (currentTime != fixedTime) {
                level.setDayTime(fixedTime);
            }

            level.setWeatherParameters(
                    6000,
                    0,
                    false,
                    false
            );
        }
    }

    private static boolean isPersonalSpace(LevelAccessor level) {
        if (level instanceof Level realLevel) {
            return realLevel.dimension().location().getNamespace().equals(PersonalSpace.MODID);
        }

        return false;
    }
}