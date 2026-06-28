package me.eigenraven.personalspace.dimension;

import commoble.infiniverse.api.InfiniverseAPI;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PersonalSpaceDeletionManager {
    private static final Map<ResourceLocation, PendingDeletion> PENDING_DELETIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Integer> DELETE_QUEUE = new HashMap<>();

    private PersonalSpaceDeletionManager() {
    }

    public static void requestDeleteFromButton(ServerPlayer player, ResourceLocation levelId) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return;
        }

        if (!PSDimensions.isPersonalSpaceDimension(levelId)) {
            player.sendSystemMessage(translatable("delete_not_personalspace"));
            return;
        }

        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, levelId);

        if (!PSDimensions.levelExists(server, levelKey)) {
            player.sendSystemMessage(translatable("delete_not_found", levelKey.location()));
            return;
        }

        if (isTeamDimension(levelId)) {
            requestTeamDelete(player, levelKey);
            return;
        }

        deleteNow(server, levelKey);
        player.sendSystemMessage(translatable("delete_done"));
    }

    public static int adminDelete(CommandSourceStack source, ResourceKey<Level> levelKey) {
        MinecraftServer server = source.getServer();

        if (!PSDimensions.isPersonalSpaceDimension(levelKey.location())) {
            source.sendFailure(translatable("delete_not_personalspace"));
            return 0;
        }

        if (!PSDimensions.levelExists(server, levelKey)) {
            source.sendFailure(translatable("delete_not_found", levelKey.location()));
            return 0;
        }

        deleteNow(server, levelKey);

        source.sendSuccess(
                () -> translatable("delete_started", levelKey.location()),
                false
        );

        return 1;
    }

    public static int approveDelete(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(translatable("delete_player_only"));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ResourceKey<Level> playerPersonalKey = PSDimensions.personalKeyForPlayer(player);
        PendingDeletion pending = PENDING_DELETIONS.get(playerPersonalKey.location());

        if (pending == null) {
            source.sendFailure(translatable("delete_no_pending_team"));
            return 0;
        }

        if (!pending.requiredApprovals().contains(player.getUUID())) {
            source.sendFailure(translatable("delete_not_team_member"));
            return 0;
        }

        pending.approvals().add(player.getUUID());

        int approved = pending.approvals().size();
        int required = pending.requiredApprovals().size();

        if (approved >= required) {
            PENDING_DELETIONS.remove(playerPersonalKey.location());
            deleteNow(server, playerPersonalKey);
            player.sendSystemMessage(translatable("delete_team_done"));
            return 1;
        }

        broadcastToRequired(
                server,
                pending,
                translatable("delete_approved", approved, required)
        );

        return 1;
    }

    public static int cancelDelete(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(translatable("delete_player_only"));
            return 0;
        }

        ResourceKey<Level> playerPersonalKey = PSDimensions.personalKeyForPlayer(player);
        PendingDeletion pending = PENDING_DELETIONS.remove(playerPersonalKey.location());

        if (pending == null) {
            source.sendFailure(translatable("delete_no_pending"));
            return 0;
        }

        broadcastToRequired(
                source.getServer(),
                pending,
                translatable("delete_cancelled", player.getGameProfile().getName())
        );

        return 1;
    }

    private static void requestTeamDelete(ServerPlayer requester, ResourceKey<Level> levelKey) {
        MinecraftServer server = requester.getServer();

        if (server == null) {
            return;
        }

        Set<UUID> required = getOnlineTeamMembersForDimension(server, levelKey);

        if (required.isEmpty()) {
            required.add(requester.getUUID());
        }

        PendingDeletion pending = new PendingDeletion(levelKey, required, new HashSet<>());
        pending.approvals().add(requester.getUUID());

        PENDING_DELETIONS.put(levelKey.location(), pending);

        if (pending.approvals().containsAll(pending.requiredApprovals())) {
            PENDING_DELETIONS.remove(levelKey.location());
            deleteNow(server, levelKey);
            requester.sendSystemMessage(translatable("delete_team_done"));
            return;
        }

        broadcastToRequired(
                server,
                pending,
                translatable("delete_requested", requester.getGameProfile().getName())
        );
    }

    private static Set<UUID> getOnlineTeamMembersForDimension(MinecraftServer server, ResourceKey<Level> levelKey) {
        Set<UUID> result = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceKey<Level> playerKey = PSDimensions.personalKeyForPlayer(player);

            if (playerKey.location().equals(levelKey.location())) {
                result.add(player.getUUID());
            }
        }

        return result;
    }

    private static void deleteNow(MinecraftServer server, ResourceKey<Level> levelKey) {
        teleportPlayersOut(server, levelKey);

        clearLoadedPortalsTargeting(server, levelKey);

        try {
            InfiniverseAPI.get().markDimensionForUnregistration(server, levelKey);
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn("Failed to unregister Personal Space dimension {}", levelKey.location(), throwable);
        }

        DELETE_QUEUE.put(levelKey, 40);
    }

    private static void teleportPlayersOut(MinecraftServer server, ResourceKey<Level> levelKey) {
        ServerLevel deletedLevel = server.getLevel(levelKey);
        ServerLevel fallbackLevel = server.overworld();

        ResourceKey<Level> returnLevelKey = Level.OVERWORLD;
        BlockPos returnPos = fallbackLevel.getSharedSpawnPos();

        if (deletedLevel != null) {
            PersonalSpaceData data = PersonalSpaceData.load(deletedLevel);

            String returnLevelId = data.getReturnLevel();

            if (returnLevelId != null && !returnLevelId.isBlank()) {
                ResourceLocation returnLocation = ResourceLocation.tryParse(returnLevelId);

                if (returnLocation != null) {
                    ResourceKey<Level> parsedReturnKey = ResourceKey.create(
                            Registries.DIMENSION,
                            returnLocation
                    );

                    ServerLevel parsedReturnLevel = server.getLevel(parsedReturnKey);

                    if (parsedReturnLevel != null && !parsedReturnKey.equals(levelKey)) {
                        returnLevelKey = parsedReturnKey;
                        returnPos = data.getReturnPos();
                    }
                }
            }
        }

        ServerLevel returnLevel = server.getLevel(returnLevelKey);

        if (returnLevel == null) {
            returnLevel = fallbackLevel;
            returnPos = fallbackLevel.getSharedSpawnPos();
        }

        BlockPos safePos = findSafeTeleportPos(returnLevel, returnPos.above());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(levelKey)) {
                continue;
            }

            player.teleportTo(
                    returnLevel,
                    safePos.getX() + 0.5D,
                    safePos.getY(),
                    safePos.getZ() + 0.5D,
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }

    private static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos preferredPos) {
        BlockPos.MutableBlockPos mutable = preferredPos.mutable();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 2;

        int startY = Math.max(minY, Math.min(maxY, preferredPos.getY()));

        for (int y = startY; y <= maxY; y++) {
            mutable.set(preferredPos.getX(), y, preferredPos.getZ());

            if (isSafeTeleportPos(level, mutable)) {
                return mutable.immutable();
            }
        }

        for (int y = startY; y >= minY; y--) {
            mutable.set(preferredPos.getX(), y, preferredPos.getZ());

            if (isSafeTeleportPos(level, mutable)) {
                return mutable.immutable();
            }
        }

        return preferredPos;
    }

    private static boolean isSafeTeleportPos(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

    private static void clearLoadedPortalsTargeting(MinecraftServer server, ResourceKey<Level> deletedLevelKey) {
        for (ServerLevel level : server.getAllLevels()) {
            clearLoadedPortalsTargetingInLevel(level, deletedLevelKey);
        }
    }

    private static void clearLoadedPortalsTargetingInLevel(ServerLevel level, ResourceKey<Level> deletedLevelKey) {
        Set<Long> checkedChunks = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            int playerChunkX = player.chunkPosition().x;
            int playerChunkZ = player.chunkPosition().z;

            clearLoadedPortalChunksAround(level, deletedLevelKey, playerChunkX, playerChunkZ, checkedChunks);
        }

        BlockPos spawn = level.getSharedSpawnPos();

        clearLoadedPortalChunksAround(
                level,
                deletedLevelKey,
                spawn.getX() >> 4,
                spawn.getZ() >> 4,
                checkedChunks
        );
    }

    private static void clearLoadedPortalChunksAround(
            ServerLevel level,
            ResourceKey<Level> deletedLevelKey,
            int centerChunkX,
            int centerChunkZ,
            Set<Long> checkedChunks
    ) {
        int radius = 12;

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);

                if (!checkedChunks.add(chunkKey)) {
                    continue;
                }

                ChunkAccess chunkAccess = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                if (!(chunkAccess instanceof LevelChunk chunk)) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof PortalBlockEntity portal)) {
                        continue;
                    }

                    if (portal.getTargetLevel() == null) {
                        continue;
                    }

                    if (!portal.getTargetLevel().location().equals(deletedLevelKey.location())) {
                        continue;
                    }

                    portal.clearTarget();

                    PersonalSpace.LOGGER.info(
                            "Cleared portal target at {} in {} because target dimension {} was deleted",
                            portal.getBlockPos(),
                            level.dimension().location(),
                            deletedLevelKey.location()
                    );
                }
            }
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (DELETE_QUEUE.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();

        DELETE_QUEUE.replaceAll((key, ticks) -> ticks - 1);

        Set<ResourceKey<Level>> ready = new HashSet<>();

        for (Map.Entry<ResourceKey<Level>, Integer> entry : DELETE_QUEUE.entrySet()) {
            ResourceKey<Level> key = entry.getKey();

            if (entry.getValue() <= 0 && server.getLevel(key) == null) {
                ready.add(key);
            }

            if (entry.getValue() <= -200) {
                ready.add(key);
            }
        }

        for (ResourceKey<Level> key : ready) {
            DELETE_QUEUE.remove(key);
            deleteDimensionFiles(server, key);
        }
    }

    private static void deleteDimensionFiles(MinecraftServer server, ResourceKey<Level> levelKey) {
        ResourceLocation id = levelKey.location();

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);

        Path modernPath = worldRoot
                .resolve("dimensions")
                .resolve(id.getNamespace())
                .resolve(id.getPath());

        String path = id.getPath();

        if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
            path = path.substring((PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/").length());
        }

        Path legacyPath = worldRoot
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER)
                .resolve(path);

        deleteDirectory(modernPath);
        deleteDirectory(legacyPath);

        PersonalSpace.LOGGER.info("Deleted Personal Space dimension files for {}", id);
    }

    private static void deleteDirectory(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException exception) {
                            PersonalSpace.LOGGER.warn("Failed to delete {}", file, exception);
                        }
                    });
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn("Failed to walk directory {}", path, exception);
        }
    }

    private static boolean isTeamDimension(ResourceLocation levelId) {
        String path = levelId.getPath();

        if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
            path = path.substring((PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/").length());
        }

        return path.startsWith("team_");
    }

    private static void broadcastToRequired(MinecraftServer server, PendingDeletion pending, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (pending.requiredApprovals().contains(player.getUUID())) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static Component translatable(String key, Object... args) {
        return Component.translatable("command.personalspace." + key, args);
    }

    private record PendingDeletion(
            ResourceKey<Level> levelKey,
            Set<UUID> requiredApprovals,
            Set<UUID> approvals
    ) {
    }
}