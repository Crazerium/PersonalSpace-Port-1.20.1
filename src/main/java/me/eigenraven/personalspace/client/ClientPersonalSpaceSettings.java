package me.eigenraven.personalspace.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ClientPersonalSpaceSettings {
    private static final Map<ResourceLocation, Settings> SETTINGS = new HashMap<>();

    private ClientPersonalSpaceSettings() {
    }

    public static Settings get(ResourceLocation levelId) {
        return SETTINGS.getOrDefault(levelId, Settings.DEFAULT);
    }

    public static void set(
            ResourceLocation levelId,
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue
    ) {
        SETTINGS.put(
                levelId,
                new Settings(
                        normalizeTime(timeOfDay),
                        clampColor(skyRed),
                        clampColor(skyGreen),
                        clampColor(skyBlue)
                )
        );
    }

    private static long normalizeTime(long value) {
        long result = value % 24000L;

        if (result < 0L) {
            result += 24000L;
        }

        return result;
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static final class Settings {
        public static final Settings DEFAULT = new Settings(6000L, 128, 192, 255);

        private final long timeOfDay;
        private final int skyRed;
        private final int skyGreen;
        private final int skyBlue;

        public Settings(long timeOfDay, int skyRed, int skyGreen, int skyBlue) {
            this.timeOfDay = timeOfDay;
            this.skyRed = skyRed;
            this.skyGreen = skyGreen;
            this.skyBlue = skyBlue;
        }

        public long timeOfDay() {
            return timeOfDay;
        }

        public int skyRed() {
            return skyRed;
        }

        public int skyGreen() {
            return skyGreen;
        }

        public int skyBlue() {
            return skyBlue;
        }
    }
}