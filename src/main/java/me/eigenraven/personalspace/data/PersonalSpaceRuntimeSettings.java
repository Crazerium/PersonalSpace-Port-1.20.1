package me.eigenraven.personalspace.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalSpaceRuntimeSettings {
    private static final Map<ResourceLocation, Long> TIME_OF_DAY = new ConcurrentHashMap<>();

    private PersonalSpaceRuntimeSettings() {
    }

    public static long getTimeOfDay(ServerLevel level) {
        ResourceLocation levelId = level.dimension().location();

        return TIME_OF_DAY.computeIfAbsent(levelId, id -> {
            PersonalSpaceData data = PersonalSpaceData.load(level);
            return normalizeTime(data.getTimeOfDay());
        });
    }

    public static void setTimeOfDay(ServerLevel level, long timeOfDay) {
        TIME_OF_DAY.put(level.dimension().location(), normalizeTime(timeOfDay));
    }

    public static void forget(ServerLevel level) {
        TIME_OF_DAY.remove(level.dimension().location());
    }

    private static long normalizeTime(long value) {
        long result = value % 24000L;
        if (result < 0L) {
            result += 24000L;
        }
        return result;
    }
}