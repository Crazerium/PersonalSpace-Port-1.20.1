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
        if (!ModList.get().isLoaded("gtceu")) {
            return;
        }

        try {
            Class<?> veinsClass = Class.forName(
                    "me.eigenraven.personalspace.compat.gtceu.PersonalSpaceBedrockFluidVeins"
            );

            Method method = veinsClass.getMethod(
                    "addPersonalSpaceDimension",
                    ResourceKey.class
            );

            method.invoke(null, levelKey);
        } catch (ReflectiveOperationException exception) {
            PersonalSpace.LOGGER.warn(
                    "Failed to add Personal Space dimension '{}' to GTCEu bedrock fluid veins.",
                    levelKey.location(),
                    exception
            );
        }
    }
}