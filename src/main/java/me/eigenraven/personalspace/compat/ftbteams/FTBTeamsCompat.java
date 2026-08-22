package me.eigenraven.personalspace.compat.ftbteams;

import com.mojang.authlib.GameProfile;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Optional FTB Teams bridge kept reflection-only so the NeoForge build has no hard dependency. */
public final class FTBTeamsCompat {
    private static final String MOD_ID = "ftbteams";

    private FTBTeamsCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static Optional<UUID> getTeamId(ServerPlayer player) {
        return getRealTeam(player).flatMap(FTBTeamsCompat::readTeamId);
    }

    public static Optional<String> getTeamDimensionName(ServerPlayer player) {
        return getTeamId(player)
                .map(UUID::toString)
                .map(value -> value.replace("-", "").toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .map(value -> "team_" + value);
    }

    public static Optional<String> getTeamDisplayName(ServerPlayer player) {
        return getRealTeam(player).map(FTBTeamsCompat::readTeamDisplayName).filter(value -> !value.isBlank());
    }

    public static boolean isPlayerInTeam(ServerPlayer player, String compactTeamId) {
        if (player == null || compactTeamId == null || compactTeamId.isBlank()) return false;
        String expected = compactTeamId.replace("-", "").toLowerCase(Locale.ROOT);
        return getTeamId(player).map(UUID::toString)
                .map(value -> value.replace("-", "").toLowerCase(Locale.ROOT))
                .map(expected::equals).orElse(false);
    }

    public static Optional<UUID> findTeamIdForPlayer(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null || !isLoaded()) return Optional.empty();
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) return getTeamId(online);
        try {
            Object manager = getManager();
            if (manager == null) return Optional.empty();
            for (String name : new String[]{"getTeamForPlayerID", "getTeamForPlayerId", "getTeamForPlayer", "getPlayerTeam"}) {
                Optional<UUID> result = invokeTeamLookup(manager, name, playerId);
                if (result.isPresent()) return result;
            }
        } catch (Throwable t) {
            PersonalSpace.LOGGER.warn("Failed to resolve FTB Teams team for offline player {}", playerId, t);
        }
        return Optional.empty();
    }

    public static Optional<UUID> findPlayerUuid(MinecraftServer server, String playerName) {
        if (server == null || playerName == null || playerName.isBlank()) return Optional.empty();
        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);
        if (online != null) return Optional.of(online.getUUID());
        try {
            return server.getProfileCache().get(playerName).map(GameProfile::getId);
        } catch (Throwable t) {
            PersonalSpace.LOGGER.warn("Failed to resolve profile UUID for {}", playerName, t);
            return Optional.empty();
        }
    }

    private static Object getManager() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
        Object api = apiClass.getMethod("api").invoke(null);
        try {
            Object loaded = api.getClass().getMethod("isManagerLoaded").invoke(api);
            if (loaded instanceof Boolean b && !b) return null;
        } catch (NoSuchMethodException ignored) {}
        return api.getClass().getMethod("getManager").invoke(api);
    }

    private static Optional<Object> getRealTeam(ServerPlayer player) {
        if (player == null || !isLoaded()) return Optional.empty();
        try {
            Object manager = getManager();
            if (manager == null) return Optional.empty();
            Object value = null;
            for (Method m : manager.getClass().getMethods()) {
                if (!m.getName().equals("getTeamForPlayer") || m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(player.getClass())
                        && !m.getParameterTypes()[0].isAssignableFrom(ServerPlayer.class)) continue;
                value = m.invoke(manager, player);
                break;
            }
            Object team = unwrapOptional(value);
            if (team == null || isPlayerTeam(team)) return Optional.empty();
            return Optional.of(team);
        } catch (Throwable t) {
            PersonalSpace.LOGGER.warn("Failed to resolve FTB Teams team for player {}", player.getGameProfile().getName(), t);
            return Optional.empty();
        }
    }

    private static Optional<UUID> invokeTeamLookup(Object manager, String methodName, UUID playerId) {
        try {
            Method method = manager.getClass().getMethod(methodName, UUID.class);
            Object team = unwrapOptional(method.invoke(manager, playerId));
            if (team != null && !isPlayerTeam(team)) return readTeamId(team);
        } catch (ReflectiveOperationException ignored) {
        } catch (Throwable t) {
            PersonalSpace.LOGGER.debug("FTB Teams lookup method {} failed", methodName, t);
        }
        return Optional.empty();
    }

    private static boolean isPlayerTeam(Object team) {
        try {
            Object value = team.getClass().getMethod("isPlayerTeam").invoke(team);
            return value instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Optional<UUID> readTeamId(Object team) {
        try {
            Object value = team.getClass().getMethod("getTeamId").invoke(team);
            if (value instanceof UUID uuid) return Optional.of(uuid);
            if (value != null) return Optional.of(UUID.fromString(value.toString()));
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    private static String readTeamDisplayName(Object team) {
        for (String methodName : new String[]{"getName", "getShortName", "getDisplayName", "getTitle"}) {
            String value = readStringLikeMethod(team, methodName);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String readStringLikeMethod(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            if (value == null) return "";
            try {
                Object stringValue = value.getClass().getMethod("getString").invoke(value);
                return stringValue == null ? "" : stringValue.toString().trim();
            } catch (ReflectiveOperationException ignored) {}
            return value.toString().trim();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static Object unwrapOptional(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }
}
