package me.eigenraven.personalspace.compat.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class FTBTeamsCompat {
    private static final String FTB_TEAMS_MOD_ID = "ftbteams";

    private FTBTeamsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(FTB_TEAMS_MOD_ID);
    }

    public static Optional<String> getTeamDimensionName(ServerPlayer player) {
        Optional<Team> optionalTeam = getRealTeam(player);

        if (optionalTeam.isEmpty()) {
            return Optional.empty();
        }

        Team team = optionalTeam.get();

        try {
            String teamId = team.getTeamId()
                    .toString()
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);

            if (teamId.isBlank()) {
                return Optional.empty();
            }

            return Optional.of("team_" + teamId);
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn(
                    "Failed to resolve FTB Teams dimension id for player {}",
                    player.getGameProfile().getName(),
                    throwable
            );
            return Optional.empty();
        }
    }

    public static Optional<String> getTeamDisplayName(ServerPlayer player) {
        Optional<Team> optionalTeam = getRealTeam(player);

        if (optionalTeam.isEmpty()) {
            return Optional.empty();
        }

        Team team = optionalTeam.get();

        String displayName = readTeamDisplayName(team);

        if (displayName.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(displayName);
    }

    private static Optional<Team> getRealTeam(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }

        if (!isLoaded()) {
            return Optional.empty();
        }

        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }

            Optional<Team> optionalTeam = FTBTeamsAPI.api()
                    .getManager()
                    .getTeamForPlayer(player);

            if (optionalTeam.isEmpty()) {
                return Optional.empty();
            }

            Team team = optionalTeam.get();

            if (team.isPlayerTeam()) {
                return Optional.empty();
            }

            return Optional.of(team);
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

        String fromMethod = readStringLikeMethod(team, "getName");

        if (!fromMethod.isBlank()) {
            return fromMethod;
        }

        fromMethod = readStringLikeMethod(team, "getShortName");

        if (!fromMethod.isBlank()) {
            return fromMethod;
        }

        fromMethod = readStringLikeMethod(team, "getDisplayName");

        if (!fromMethod.isBlank()) {
            return fromMethod;
        }

        fromMethod = readStringLikeMethod(team, "getTitle");

        if (!fromMethod.isBlank()) {
            return fromMethod;
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