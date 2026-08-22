package me.eigenraven.personalspace.dimension;

import net.commoble.infiniverse.api.InfiniverseAPI;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public final class PersonalSpaceLazyMigrationManager {
    private static final int DELAY_BETWEEN_DIMENSIONS_TICKS = 100;
    private static final Deque<ResourceKey<Level>> QUEUE = new ArrayDeque<>();

    private static boolean running;
    private static ResourceKey<Level> waitingForRemoval;
    private static int delayTicks;
    private static int removalWaitTicks;
    private static int total;
    private static int completed;
    private static int skipped;
    private static int failed;
    private static String lastError = "";

    private PersonalSpaceLazyMigrationManager() {
    }

    public static boolean isRunning() {
        return running || waitingForRemoval != null;
    }

    public static int start(CommandSourceStack source) {
        if (running) {
            source.sendFailure(Component.literal("Lazy migration is already running."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        List<ResourceKey<Level>> candidates = new ArrayList<>();
        int initiallySkipped = 0;

        for (ServerLevel level : server.getAllLevels()) {
            if (!PSDimensions.isPersonalSpaceDimension(level.dimension().location())) {
                continue;
            }

            if (!level.players().isEmpty() || !level.getForcedChunks().isEmpty()) {
                initiallySkipped++;
                continue;
            }

            candidates.add(level.dimension());
        }

        candidates.sort(Comparator.comparing(key -> key.location().toString()));

        QUEUE.clear();
        QUEUE.addAll(candidates);
        running = !QUEUE.isEmpty();
        waitingForRemoval = null;
        delayTicks = 0;
        removalWaitTicks = 0;
        total = candidates.size();
        completed = 0;
        skipped = initiallySkipped;
        failed = 0;
        lastError = "";

        if (!running) {
            source.sendSuccess(
                    () -> Component.literal(
                            "No idle Personal Space dimensions found. Skipped active/forced: " + skipped
                    ),
                    true
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Lazy migration started: queued=" + total
                                + ", skipped active/forced=" + skipped
                                + ". Dimensions will be processed one at a time."
                ),
                true
        );
        return 1;
    }

    public static int cancel(CommandSourceStack source) {
        if (!running && waitingForRemoval == null) {
            source.sendFailure(Component.literal("Lazy migration is not running."));
            return 0;
        }

        QUEUE.clear();
        running = false;

        if (waitingForRemoval != null) {
            ServerLevel level = source.getServer().getLevel(waitingForRemoval);
            if (level != null) {
                level.noSave = false;
            }
        }

        waitingForRemoval = null;
        removalWaitTicks = 0;
        source.sendSuccess(() -> Component.literal("Lazy migration cancelled."), true);
        return 1;
    }

    public static int status(CommandSourceStack source) {
        String current = waitingForRemoval == null
                ? "none"
                : waitingForRemoval.location().toString();

        source.sendSuccess(
                () -> Component.literal(
                        "Lazy migration: running=" + running
                                + ", completed=" + completed + "/" + total
                                + ", queued=" + QUEUE.size()
                                + ", skipped=" + skipped
                                + ", failed=" + failed
                                + ", current=" + current
                                + (lastError.isBlank() ? "" : ", lastError=" + lastError)
                ),
                false
        );
        return 1;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!running) {
            return;
        }

        MinecraftServer server = event.getServer();

        if (waitingForRemoval != null) {
            ServerLevel remaining = server.getLevel(waitingForRemoval);

            if (remaining == null) {
                completed++;
                PersonalSpace.LOGGER.info(
                        "Lazy migration unregistered Personal Space dimension {} ({}/{})",
                        waitingForRemoval.location(),
                        completed,
                        total
                );
                waitingForRemoval = null;
                delayTicks = DELAY_BETWEEN_DIMENSIONS_TICKS;
            } else if (removalWaitTicks > 0) {
                removalWaitTicks--;
            } else {
                remaining.noSave = false;
                failAndStop(
                        "Infiniverse did not unregister " + waitingForRemoval.location()
                );
            }
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        ResourceKey<Level> key = QUEUE.pollFirst();
        if (key == null) {
            running = false;
            PersonalSpace.LOGGER.info(
                    "Lazy migration finished: completed={}, skipped={}, failed={}",
                    completed,
                    skipped,
                    failed
            );
            return;
        }

        ServerLevel level = server.getLevel(key);
        if (level == null) {
            completed++;
            return;
        }

        if (!level.players().isEmpty() || !level.getForcedChunks().isEmpty()) {
            skipped++;
            delayTicks = DELAY_BETWEEN_DIMENSIONS_TICKS;
            return;
        }

        try {
            level.save(null, true, false);
            level.noSave = true;
            waitingForRemoval = key;
            removalWaitTicks = 5;
            InfiniverseAPI.get().markDimensionForUnregistration(server, key);
        } catch (Throwable throwable) {
            level.noSave = false;
            PersonalSpace.LOGGER.error(
                    "Lazy migration failed before unregistering {}",
                    key.location(),
                    throwable
            );
            failAndStop(
                    throwable.getClass().getSimpleName() + ": "
                            + (throwable.getMessage() == null ? "no message" : throwable.getMessage())
            );
        }
    }

    private static void failAndStop(String error) {
        failed++;
        lastError = error;
        running = false;
        QUEUE.clear();
        waitingForRemoval = null;
        removalWaitTicks = 0;
        PersonalSpace.LOGGER.error("Lazy migration stopped: {}", error);
    }
}
