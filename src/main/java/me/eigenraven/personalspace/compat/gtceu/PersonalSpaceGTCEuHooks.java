package me.eigenraven.personalspace.compat.gtceu;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class PersonalSpaceGTCEuHooks {
    private PersonalSpaceGTCEuHooks() {
    }

    public static void onPersonalDimensionCreated(ResourceKey<Level> levelKey) {
        if (levelKey == null) {
            return;
        }

        if (!ModList.get().isLoaded("gtceu")) {
            return;
        }

        if (!PersonalSpaceGTCEuConfig.ENABLED.get()) {
            return;
        }

        /*
         * ВАЖНО:
         * Добавляем Personal Space dimension только в НАШИ собственные
         * personalspace:void_* bedrock fluid definitions.
         *
         * Не патчим существующие GTCEu/GTO жилы, иначе туда может попасть
         * мусорная/служебная жила вроде "Воздух".
         */
        try {
            Class<?> veinsClass = Class.forName(
                    "me.eigenraven.personalspace.compat.gtceu.PersonalSpaceBedrockFluidVeins"
            );

            Method addOwnDimensionMethod = veinsClass.getMethod(
                    "addPersonalSpaceDimension",
                    ResourceKey.class
            );

            addOwnDimensionMethod.invoke(null, levelKey);

            PersonalSpace.LOGGER.info(
                    "Added Personal Space dimension '{}' to own GTCEu bedrock fluid vein definition(s).",
                    levelKey.location()
            );
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn(
                    "Failed to add Personal Space dimension '{}' to own GTCEu bedrock fluid vein definition(s).",
                    levelKey.location(),
                    throwable
            );
        }
    }
}