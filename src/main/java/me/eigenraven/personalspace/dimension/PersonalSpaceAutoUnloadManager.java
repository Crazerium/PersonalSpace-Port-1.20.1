package me.eigenraven.personalspace.dimension;

import commoble.infiniverse.api.InfiniverseAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.compat.ftbchunks.FTBChunksForceLoadCompat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PersonalSpaceAutoUnloadManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type OFFLINE_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
    private static final String OFFLINE_FILE = "personalspace_offline_players.json";
    private static final int SCAN_INTERVAL_TICKS = 100;
    private static final int REMOVAL_WAIT_TICKS = 10;

    private static final Map<ResourceKey<Level>, Integer> IDLE_TICKS = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> OFFLINE_PLAYER_DIMENSIONS = new HashMap<>();

    private static boolean loadedOfflineMap;
    private static int scanCountdown;
    private static ResourceKey<Level> waitingForRemoval;
    private static int removalWaitTicks;
    private static long completedUnloads;
    private static long skippedForced;
    private static long skippedFtbForced;
    private static long skippedOfflinePlayers;
    private static String lastError = "";

    private PersonalSpaceAutoUnloadManager() {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ensureOfflineMapLoaded(server);
        ResourceKey<Level> current = player.level().dimension();

        if (PSDimensions.isPersonalSpaceDimension(current.location())) {
            OFFLINE_PLAYER_DIMENSIONS.put(player.getUUID(), current);
        } else {
            OFFLINE_PLAYER_DIMENSIONS.remove(player.getUUID());
        }

        saveOfflineMap(server);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ensureOfflineMapLoaded(server);
        OFFLINE_PLAYER_DIMENSIONS.remove(player.getUUID());
        saveOfflineMap(server);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        ensureOfflineMapLoaded(server);

        if (!PSConfig.SERVER.autoUnloadPersonalSpaces.get()) {
            IDLE_TICKS.clear();
            return;
        }

        if (PersonalSpaceLazyMigrationManager.isRunning()) {
            return;
        }

        if (waitingForRemoval != null) {
            finishOrFailRemoval(server);
            return;
        }

        if (scanCountdown > 0) {
            scanCountdown--;
            return;
        }
        scanCountdown = SCAN_INTERVAL_TICKS;

        int requiredIdleTicks = PSConfig.SERVER.autoUnloadDelaySeconds.get() * 20;
        Set<ResourceKey<Level>> loadedPersonalSpaces = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> key = level.dimension();
            if (!PSDimensions.isPersonalSpaceDimension(key.location())) {
                continue;
            }

            loadedPersonalSpaces.add(key);

            if (!level.players().isEmpty()) {
                IDLE_TICKS.remove(key);
                continue;
            }

            if (!level.getForcedChunks().isEmpty()) {
                skippedForced++;
                IDLE_TICKS.remove(key);
                continue;
            }

            if (FTBChunksForceLoadCompat.hasForceLoadedChunks(key)) {
                skippedFtbForced++;
                IDLE_TICKS.remove(key);
                continue;
            }

            if (isOfflinePlayerProtected(key)) {
                skippedOfflinePlayers++;
                IDLE_TICKS.remove(key);
                continue;
            }

            int idle = IDLE_TICKS.getOrDefault(key, 0) + SCAN_INTERVAL_TICKS;
            IDLE_TICKS.put(key, idle);

            if (idle >= requiredIdleTicks) {
                beginUnload(server, level);
                return;
            }
        }

        IDLE_TICKS.keySet().removeIf(key -> !loadedPersonalSpaces.contains(key));
    }

    public static String status(MinecraftServer server) {
        ensureOfflineMapLoaded(server);
        return "autoUnload=" + PSConfig.SERVER.autoUnloadPersonalSpaces.get()
                + ", delaySeconds=" + PSConfig.SERVER.autoUnloadDelaySeconds.get()
                + ", trackedIdle=" + IDLE_TICKS.size()
                + ", offlineProtected=" + OFFLINE_PLAYER_DIMENSIONS.size()
                + ", current=" + (waitingForRemoval == null ? "none" : waitingForRemoval.location())
                + ", completed=" + completedUnloads
                + ", skippedForced=" + skippedForced
                + ", skippedFtbForced=" + skippedFtbForced
                + ", skippedOffline=" + skippedOfflinePlayers
                + (lastError.isBlank() ? "" : ", lastError=" + lastError);
    }

    private static void beginUnload(MinecraftServer server, ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        if (!level.players().isEmpty()) {
            IDLE_TICKS.remove(key);
            return;
        }

        if (!level.getForcedChunks().isEmpty()) {
            skippedForced++;
            IDLE_TICKS.remove(key);
            return;
        }

        if (FTBChunksForceLoadCompat.hasForceLoadedChunks(key)) {
            skippedFtbForced++;
            IDLE_TICKS.remove(key);
            return;
        }

        if (isOfflinePlayerProtected(key)) {
            skippedOfflinePlayers++;
            IDLE_TICKS.remove(key);
            return;
        }

        try {
            level.save(null, true, false);
            level.noSave = true;
            waitingForRemoval = key;
            removalWaitTicks = REMOVAL_WAIT_TICKS;
            InfiniverseAPI.get().markDimensionForUnregistration(server, key);
            PersonalSpace.LOGGER.info("Scheduled idle Personal Space {} for automatic unloading", key.location());
        } catch (Throwable throwable) {
            level.noSave = false;
            waitingForRemoval = null;
            lastError = throwable.getClass().getSimpleName() + ": "
                    + (throwable.getMessage() == null ? "no message" : throwable.getMessage());
            PersonalSpace.LOGGER.error("Failed to auto-unload Personal Space {}", key.location(), throwable);
        }
    }

    private static void finishOrFailRemoval(MinecraftServer server) {
        ServerLevel remaining = server.getLevel(waitingForRemoval);

        if (remaining == null) {
            completedUnloads++;
            IDLE_TICKS.remove(waitingForRemoval);
            PersonalSpace.LOGGER.info("Automatically unloaded idle Personal Space {}", waitingForRemoval.location());
            waitingForRemoval = null;
            removalWaitTicks = 0;
            return;
        }

        if (removalWaitTicks > 0) {
            removalWaitTicks--;
            return;
        }

        remaining.noSave = false;
        lastError = "Infiniverse did not unregister " + waitingForRemoval.location();
        PersonalSpace.LOGGER.error(lastError);
        IDLE_TICKS.remove(waitingForRemoval);
        waitingForRemoval = null;
    }

    private static boolean isOfflinePlayerProtected(ResourceKey<Level> key) {
        return OFFLINE_PLAYER_DIMENSIONS.containsValue(key);
    }

    private static void ensureOfflineMapLoaded(MinecraftServer server) {
        if (loadedOfflineMap) {
            return;
        }

        loadedOfflineMap = true;
        Path file = offlineMapPath(server);
        if (!Files.isRegularFile(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, String> raw = GSON.fromJson(reader, OFFLINE_MAP_TYPE);
            if (raw == null) {
                return;
            }

            for (Map.Entry<String, String> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    ResourceLocation id = ResourceLocation.tryParse(entry.getValue());
                    if (id != null && PSDimensions.isPersonalSpaceDimension(id)) {
                        OFFLINE_PLAYER_DIMENSIONS.put(
                                uuid,
                                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id)
                        );
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception exception) {
            PersonalSpace.LOGGER.warn("Failed to read {}", file, exception);
        }
    }

    private static void saveOfflineMap(MinecraftServer server) {
        Path file = offlineMapPath(server);
        Map<String, String> raw = new HashMap<>();

        OFFLINE_PLAYER_DIMENSIONS.forEach((uuid, key) ->
                raw.put(uuid.toString(), key.location().toString())
        );

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(raw, OFFLINE_MAP_TYPE, writer);
            }
        } catch (Exception exception) {
            PersonalSpace.LOGGER.warn("Failed to write {}", file, exception);
        }
    }

    private static Path offlineMapPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(OFFLINE_FILE);
    }
}
