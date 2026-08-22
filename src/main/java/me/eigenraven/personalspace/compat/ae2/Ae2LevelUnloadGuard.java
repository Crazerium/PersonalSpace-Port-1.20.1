package me.eigenraven.personalspace.compat.ae2;

import net.minecraft.world.level.LevelAccessor;


public final class Ae2LevelUnloadGuard {
    private static final ThreadLocal<LevelAccessor> UNLOADING_LEVEL = new ThreadLocal<>();

    private Ae2LevelUnloadGuard() {
    }

    public static void begin(LevelAccessor level) {
        UNLOADING_LEVEL.set(level);
    }

    public static void end(LevelAccessor level) {
        if (UNLOADING_LEVEL.get() == level) {
            UNLOADING_LEVEL.remove();
        }
    }

    public static boolean isUnloading(LevelAccessor level) {
        return level != null && UNLOADING_LEVEL.get() == level;
    }
}
