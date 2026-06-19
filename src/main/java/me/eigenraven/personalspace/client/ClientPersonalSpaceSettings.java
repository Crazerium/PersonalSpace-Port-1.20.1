package me.eigenraven.personalspace.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPersonalSpaceSettings {
    private static final Settings DEFAULT = new Settings(
            6000L,
            127,
            178,
            255,
            1.0F,
            "minecraft:plains",
            false,
            false,
            false,
            false,
            "minecraft:bedrock*1;minecraft:dirt*3;minecraft:grass_block*1"
    );

    private static final Map<ResourceLocation, Settings> SETTINGS = new ConcurrentHashMap<>();

    private ClientPersonalSpaceSettings() {
    }

    public static Settings get(ResourceLocation levelId) {
        return SETTINGS.getOrDefault(levelId, DEFAULT);
    }

    public static void set(
            ResourceLocation levelId,
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue
    ) {
        Settings old = get(levelId);

        set(
                levelId,
                timeOfDay,
                skyRed,
                skyGreen,
                skyBlue,
                old.starBrightness(),
                old.biomeName(),
                old.treesEnabled(),
                old.foliageEnabled(),
                old.weatherEnabled(),
                old.cloudsEnabled(),
                old.layersPreset()
        );
    }

    public static void set(
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
        SETTINGS.put(
                levelId,
                new Settings(
                        timeOfDay,
                        skyRed,
                        skyGreen,
                        skyBlue,
                        starBrightness,
                        biomeName,
                        treesEnabled,
                        foliageEnabled,
                        weatherEnabled,
                        cloudsEnabled,
                        layersPreset
                )
        );
    }

    public record Settings(
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
    }
}