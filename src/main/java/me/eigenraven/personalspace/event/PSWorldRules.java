package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.network.PersonalSpaceSettingsSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!isPersonalSpace(event.getLevel())) {
            return;
        }

        // All mobs are banned in Personal Space.
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

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
                continue;
            }

            PersonalSpaceData data = PersonalSpaceData.load(level);

            level.setDayTime(data.getTimeOfDay());

            if (!data.isWeatherEnabled()) {
                level.setRainLevel(0.0F);
                level.setThunderLevel(0.0F);
                level.setWeatherParameters(
                        6000,
                        0,
                        false,
                        false
                );
            }
        }
    }

    private static boolean isPersonalSpace(LevelAccessor level) {
        if (level instanceof Level realLevel) {
            return realLevel.dimension().location().getNamespace().equals(PersonalSpace.MODID);
        }

        return false;
    }
}