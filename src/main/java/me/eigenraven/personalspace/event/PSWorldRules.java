package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceRuntimeSettings;
import me.eigenraven.personalspace.network.PersonalSpaceSettingsSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class PSWorldRules {
    private PSWorldRules() {}

    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (isPersonalSpace(event.getLevel())) event.setCanceled(true);
    }

    public static void onMobSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (isPersonalSpace(event.getLevel())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isPersonalSpace(event.getLevel())) return;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            PersonalSpaceSettingsSync.syncTo(player, level);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            PersonalSpaceSettingsSync.syncTo(player, level);
        }
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isPersonalSpace(level)) return;
        PersonalSpaceRuntimeSettings.setTimeOfDay(level, PersonalSpaceRuntimeSettings.getTimeOfDay(level));
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isPersonalSpace(level)) return;
        PersonalSpaceRuntimeSettings.forget(level);
    }

    private static boolean isPersonalSpace(LevelAccessor level) {
        return level instanceof Level realLevel
                && realLevel.dimension().location().getNamespace().equals(PersonalSpace.MODID);
    }
}
