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
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

        PersonalSpaceData data = getDataForCheck(player.server, level.dimension(), player);
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
            String teamId = path.substring("team_".length()).replace("-", "").toLowerCase(Locale.ROOT);
            if (!"team".equals(data.getProtectionOwnerType()) || !teamId.equals(data.getProtectionOwnerId())) {
                data.setProtectionOwnerTeam(teamId);
                return true;
            }
            return false;
        }

        if (!path.startsWith("ps_")) {
            return false;
        }

        String inferredName = inferPlayerName(path.substring("ps_".length()));
        UUID ownerId = null;
        UUID teamId = null;

        if (actor != null && actor.getGameProfile().getName().equalsIgnoreCase(inferredName)) {
            ownerId = actor.getUUID();
            teamId = FTBTeamsCompat.getTeamId(actor).orElse(null);
        } else {
            Optional<UUID> resolvedOwner = FTBTeamsCompat.findPlayerUuid(server, inferredName);
            if (resolvedOwner.isPresent()) {
                ownerId = resolvedOwner.get();
                teamId = FTBTeamsCompat.findTeamIdForPlayer(server, ownerId).orElse(null);
            }
        }

        String oldType = data.getProtectionOwnerType();
        String oldId = data.getProtectionOwnerId();
        String oldName = data.getProtectionOwnerName();
        String oldTeam = data.getProtectionTeamId();

        if (ownerId != null) {
            data.setProtectionOwnerPlayer(ownerId, inferredName, teamId);
        } else if (!"player".equals(oldType) || oldName.isBlank()) {
            data.setProtectionOwnerPlayer(null, inferredName, teamId);
        }

        return !oldType.equals(data.getProtectionOwnerType())
                || !oldId.equals(data.getProtectionOwnerId())
                || !oldName.equals(data.getProtectionOwnerName())
                || !oldTeam.equals(data.getProtectionTeamId());
    }

    private static String inferPlayerName(String raw) {
        return raw.replaceFirst("_[0-9]+$", "");
    }
}
