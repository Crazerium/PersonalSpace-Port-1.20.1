package me.eigenraven.personalspace.compat.ftbteams;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class FTBTeamsCompat {
    private static final String FTB_TEAMS_MOD_ID = "ftbteams";

    private FTBTeamsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(FTB_TEAMS_MOD_ID);
    }

    public static Optional<UUID> getTeamId(ServerPlayer player) {
        return getRealTeam(player).map(Team::getTeamId);
    }

    public static Optional<String> getTeamDimensionName(ServerPlayer player) {
        return getTeamId(player)
                .map(UUID::toString)
                .map(value -> value.replace("-", "").toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .map(value -> "team_" + value);
    }

    public static Optional<String> getTeamDisplayName(ServerPlayer player) {
        Optional<Team> optionalTeam = getRealTeam(player);

        if (optionalTeam.isEmpty()) {
            return Optional.empty();
        }

        String displayName = readTeamDisplayName(optionalTeam.get());
        return displayName.isBlank() ? Optional.empty() : Optional.of(displayName);
    }

    public static boolean isPlayerInTeam(ServerPlayer player, String compactTeamId) {
        if (player == null || compactTeamId == null || compactTeamId.isBlank()) {
            return false;
        }

        String normalizedExpected = compactTeamId.replace("-", "").toLowerCase(Locale.ROOT);
        return getTeamId(player)
                .map(UUID::toString)
                .map(value -> value.replace("-", "").toLowerCase(Locale.ROOT))
                .map(normalizedExpected::equals)
                .orElse(false);
    }
    public static Optional<UUID> findTeamIdForPlayer(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null || !isLoaded()) {
            return Optional.empty();
        }

        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return getTeamId(online);
        }

        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }

            Object manager = FTBTeamsAPI.api().getManager();
            String[] names = {
                    "getTeamForPlayerID",
                    "getTeamForPlayerId",
                    "getTeamForPlayer",
                    "getPlayerTeam"
            };

            for (String name : names) {
                Optional<UUID> result = invokeTeamLookup(manager, name, playerId);
                if (result.isPresent()) {
                    return result;
                }
            }
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn("Failed to resolve FTB Teams team for offline player {}", playerId, throwable);
        }

        return Optional.empty();
    }

    public static Optional<UUID> findPlayerUuid(MinecraftServer server, String playerName) {
        if (server == null || playerName == null || playerName.isBlank()) {
            return Optional.empty();
        }

        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);
        if (online != null) {
            return Optional.of(online.getUUID());
        }

        try {
            return server.getProfileCache()
                    .get(playerName)
                    .map(GameProfile::getId);
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn("Failed to resolve profile UUID for {}", playerName, throwable);
            return Optional.empty();
        }
    }

    private static Optional<UUID> invokeTeamLookup(Object manager, String methodName, UUID playerId) {
        try {
            Method method = manager.getClass().getMethod(methodName, UUID.class);
            Object value = method.invoke(manager, playerId);
            Object teamObject = unwrapOptional(value);

            if (teamObject instanceof Team team && !team.isPlayerTeam()) {
                return Optional.of(team.getTeamId());
            }
        } catch (ReflectiveOperationException ignored) {
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.debug("FTB Teams lookup method {} failed", methodName, throwable);
        }

        return Optional.empty();
    }

    private static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private static Optional<Team> getRealTeam(ServerPlayer player) {
        if (player == null || !isLoaded()) {
            return Optional.empty();
        }

        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }

            Optional<Team> optionalTeam = FTBTeamsAPI.api()
                    .getManager()
                    .getTeamForPlayer(player);

            if (optionalTeam.isEmpty() || optionalTeam.get().isPlayerTeam()) {
                return Optional.empty();
            }

            return optionalTeam;
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn(
                    "Failed to resolve FTB Teams team for player {}",
                    player.getGameProfile().getName(),
                    throwable
            );
            return Optional.empty();
        }
    }

    private static String readTeamDisplayName(Team team) {
        if (team == null) {
            return "";
        }

        for (String methodName : new String[]{"getName", "getShortName", "getDisplayName", "getTitle"}) {
            String value = readStringLikeMethod(team, methodName);
            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private static String readStringLikeMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);

            if (value == null) {
                return "";
            }

            try {
                Method getStringMethod = value.getClass().getMethod("getString");
                Object stringValue = getStringMethod.invoke(value);
                if (stringValue != null) {
                    return stringValue.toString().trim();
                }
            } catch (ReflectiveOperationException ignored) {
            }

            return value.toString().trim();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }
}