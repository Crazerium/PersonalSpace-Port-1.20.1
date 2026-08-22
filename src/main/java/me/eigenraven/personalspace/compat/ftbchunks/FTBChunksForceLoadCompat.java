package me.eigenraven.personalspace.compat.ftbchunks;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;


public final class FTBChunksForceLoadCompat {
    private static final String MANAGER_CLASS =
            "dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl";

    private static boolean reflectionInitialized;
    private static boolean reflectionAvailable;
    private static boolean loggedFailure;
    private static Method getInstanceMethod;
    private static Method getForceLoadedChunksMethod;

    private FTBChunksForceLoadCompat() {
    }

    public static boolean hasForceLoadedChunks(ResourceKey<Level> dimension) {
        if (!ModList.get().isLoaded("ftbchunks")) {
            return false;
        }

        initializeReflection();
        if (!reflectionAvailable) {
            return true;
        }

        try {
            Object manager = getInstanceMethod.invoke(null);
            if (manager == null) {
                return true;
            }

            Object chunks = getForceLoadedChunksMethod.invoke(manager, dimension);
            if (chunks instanceof Map<?, ?> map) {
                return !map.isEmpty();
            }
            logFailureOnce("FTB Chunks returned an unexpected force-load map type", null);
            return true;
        } catch (Throwable throwable) {
            logFailureOnce("Failed to query FTB Chunks force-loaded chunks", throwable);
            return true;
        }
    }

    private static synchronized void initializeReflection() {
        if (reflectionInitialized) {
            return;
        }

        reflectionInitialized = true;
        try {
            Class<?> managerClass = Class.forName(MANAGER_CLASS);
            getInstanceMethod = managerClass.getMethod("getInstance");
            getForceLoadedChunksMethod = managerClass.getMethod(
                    "getForceLoadedChunks",
                    ResourceKey.class
            );
            reflectionAvailable = true;
        } catch (Throwable throwable) {
            reflectionAvailable = false;
            logFailureOnce("Could not initialize FTB Chunks force-load compatibility", throwable);
        }
    }

    private static void logFailureOnce(String message, Throwable throwable) {
        if (loggedFailure) {
            return;
        }

        loggedFailure = true;
        if (throwable == null) {
            PersonalSpace.LOGGER.error(message + "; Personal Space auto-unload will stay blocked for safety");
        } else {
            PersonalSpace.LOGGER.error(
                    message + "; Personal Space auto-unload will stay blocked for safety",
                    throwable
            );
        }
    }
}
