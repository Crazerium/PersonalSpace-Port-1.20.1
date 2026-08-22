package me.eigenraven.personalspace.dimension;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.PersonalSpacePermissions;
import me.eigenraven.personalspace.compat.ftbteams.FTBTeamsCompat;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class PersonalSpaceProtectionManager {
    private PersonalSpaceProtectionManager() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!PSConfig.SERVER.privacyEnabled.get() || !PSConfig.SERVER.privacyProtectExisting.get()) {
            return;
        }

        MinecraftServer server = event.getServer();
        int changed = 0;

        for (ResourceKey<Level> key : PersonalSpaceDimensionCatalog.listDimensionKeys(server)) {
            PersonalSpaceData data = PersonalSpaceData.load(server, key);
            if (ensureMetadata(server, key, data, null)) {
                PersonalSpaceData.save(server, key, data);
                changed++;
            }
        }

        PersonalSpace.LOGGER.info("Personal Space privacy metadata migration finished: {} updated", changed);
    }

    public static PersonalSpaceData getDataForCheck(MinecraftServer server, ResourceKey<Level> key, ServerPlayer actor) {
        PersonalSpaceData data = PersonalSpaceData.load(server, key);
        if (ensureMetadata(server, key, data, actor)) {
            PersonalSpaceData.save(server, key, data);
        }
        return data;
    }

    public static boolean canModify(ServerPlayer player) {
        if (player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return false;
        }

        if (!PSConfig.SERVER.privacyEnabled.get()) {
            return true;
        }

        if (!PSDimensions.isPersonalSpaceDimension(level.dimension().location())) {
            return true;
        }

        if (PersonalSpacePermissions.canBypassProtection(player)) {
            return true;
        }

        if (PSConfig.SERVER.privacyAllowOpBypass.get() && player.hasPermissions(2)) {
            return true;
        }

        PersonalSpaceData data = getDataForCheck(player.getServer(), level.dimension(), player);
        String type = data.getProtectionOwnerType();

        if ("team".equals(type)) {
            return FTBTeamsCompat.isPlayerInTeam(player, data.getProtectionOwnerId());
        }

        if ("player".equals(type)) {
            if (matchesPlayerOwner(player, data)) {
                return true;
            }

            return PSConfig.SERVER.privacyAllowTeamMembers.get()
                    && FTBTeamsCompat.isPlayerInTeam(player, data.getProtectionTeamId());
        }

        return false;
    }

    private static boolean matchesPlayerOwner(ServerPlayer player, PersonalSpaceData data) {
        String ownerId = data.getProtectionOwnerId();
        if (!ownerId.isBlank()) {
            try {
                if (player.getUUID().equals(UUID.fromString(ownerId))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return !data.getProtectionOwnerName().isBlank()
                && data.getProtectionOwnerName().equalsIgnoreCase(player.getGameProfile().getName());
    }

    public static boolean ensureMetadata(
            MinecraftServer server,
            ResourceKey<Level> key,
            PersonalSpaceData data,
            ServerPlayer actor
    ) {
        String path = key.location().getPath();
        String prefix = PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/";

        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }

        if (path.startsWith("team_")) {
            String teamId = path
                    .substring("team_".length())
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);

            if (!"team".equals(data.getProtectionOwnerType())
                    || !teamId.equals(data.getProtectionOwnerId())) {

                data.setProtectionOwnerTeam(teamId);
                return true;
            }

            return false;
        }

        if (!path.startsWith("ps_")) {
            return false;
        }
        if ("player".equals(data.getProtectionOwnerType())
                && !data.getProtectionOwnerId().isBlank()) {

            try {
                UUID storedOwnerId = UUID.fromString(
                        data.getProtectionOwnerId()
                );
                if (actor != null && actor.getUUID().equals(storedOwnerId)) {
                    String currentName = actor.getGameProfile().getName();

                    UUID currentTeamId = FTBTeamsCompat
                            .getTeamId(actor)
                            .orElse(null);

                    String compactCurrentTeamId = currentTeamId == null
                            ? ""
                            : currentTeamId.toString()
                            .replace("-", "")
                            .toLowerCase(Locale.ROOT);

                    boolean nameChanged =
                            !currentName.equals(data.getProtectionOwnerName());

                    boolean teamChanged =
                            !compactCurrentTeamId.equals(
                                    data.getProtectionTeamId()
                            );

                    if (nameChanged || teamChanged) {
                        data.setProtectionOwnerPlayer(
                                actor.getUUID(),
                                currentName,
                                currentTeamId
                        );

                        return true;
                    }
                }

                return false;
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (actor == null) {
            return false;
        }

        String dimensionName = path.substring("ps_".length());
        String playerName = actor.getGameProfile().getName();

        String currentSafeName =
                PSDimensions.sanitizeDimensionName(playerName);

        String legacySafeName =
                legacySanitizeDimensionName(playerName);

        boolean matchesCurrentName =
                matchesDimensionName(dimensionName, currentSafeName);

        boolean matchesLegacyName =
                matchesDimensionName(dimensionName, legacySafeName);

        if (!matchesCurrentName && !matchesLegacyName) {
            return false;
        }

        data.setProtectionOwnerPlayer(
                actor.getUUID(),
                playerName,
                FTBTeamsCompat.getTeamId(actor).orElse(null)
        );

        return true;
    }

    private static boolean matchesDimensionName(
            String dimensionName,
            String playerName
    ) {
        if (dimensionName == null
                || playerName == null
                || playerName.isBlank()) {
            return false;
        }

        if (dimensionName.equalsIgnoreCase(playerName)) {
            return true;
        }

        if (dimensionName.length() <= playerName.length()) {
            return false;
        }

        if (!dimensionName.regionMatches(
                true,
                0,
                playerName,
                0,
                playerName.length()
        )) {
            return false;
        }

        String suffix = dimensionName.substring(playerName.length());

        if (!suffix.startsWith("_") || suffix.length() <= 1) {
            return false;
        }

        for (int i = 1; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static String legacySanitizeDimensionName(String rawName) {
        String safe = PSDimensions.sanitizeDimensionName(rawName);

        while (safe.contains("__")) {
            safe = safe.replace("__", "_");
        }

        while (safe.startsWith("_")) {
            safe = safe.substring(1);
        }

        while (safe.endsWith("_")) {
            safe = safe.substring(0, safe.length() - 1);
        }

        return safe;
    }
}
