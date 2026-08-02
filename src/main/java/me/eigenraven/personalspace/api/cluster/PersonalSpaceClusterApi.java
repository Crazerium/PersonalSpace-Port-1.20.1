package me.eigenraven.personalspace.api.cluster;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Public server-side API for integrating Personal Space with a cluster router.
 */
public final class PersonalSpaceClusterApi {
    public static final int API_VERSION = 1;

    private static volatile Registration registration;

    private PersonalSpaceClusterApi() {
    }

    /**
     * Registers the single cluster integration responsible for routing Personal
     * Space operations. Registering a second different integration is rejected.
     */
    public static synchronized void register(
            String integrationId,
            PersonalSpaceClusterHandler handler
    ) {
        String safeId = Objects.requireNonNull(integrationId, "integrationId").trim();
        if (safeId.isEmpty()) {
            throw new IllegalArgumentException("integrationId must not be blank");
        }

        PersonalSpaceClusterHandler safeHandler = Objects.requireNonNull(
                handler,
                "handler"
        );

        Registration current = registration;
        if (current != null) {
            if (current.integrationId.equals(safeId)
                    && current.handler == safeHandler) {
                return;
            }

            throw new IllegalStateException(
                    "Personal Space cluster integration is already registered by "
                            + current.integrationId
            );
        }

        registration = new Registration(safeId, safeHandler);
        PersonalSpace.LOGGER.info(
                "Registered Personal Space cluster integration: {}",
                safeId
        );
    }

    /** Returns the id of the active integration, or an empty string. */
    public static String registeredIntegrationId() {
        Registration current = registration;
        return current == null ? "" : current.integrationId;
    }

    /** Returns whether a cluster integration is currently registered. */
    public static boolean isRegistered() {
        return registration != null;
    }

    /** Returns whether the id belongs to a Personal Space dimension. */
    public static boolean isPersonalSpaceDimension(ResourceLocation id) {
        return PSDimensions.isPersonalSpaceDimension(id);
    }

    /** Returns whether the Personal Space is loaded or present on disk. */
    public static boolean dimensionExists(
            MinecraftServer server,
            ResourceKey<Level> dimension
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(dimension, "dimension");
        return isPersonalSpaceDimension(dimension.location())
                && PSDimensions.levelExists(server, dimension);
    }

    /** Returns the canonical world directory used by the Personal Space. */
    public static Path dimensionDirectory(
            MinecraftServer server,
            ResourceKey<Level> dimension
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(dimension, "dimension");
        if (!isPersonalSpaceDimension(dimension.location())) {
            throw new IllegalArgumentException(
                    "Not a Personal Space dimension: " + dimension.location()
            );
        }
        return PSDimensions.getPersonalSpaceDimensionFolder(server, dimension);
    }

    /**
     * Loads an existing Personal Space using the normal Personal Space loader.
     * Returns {@code null} when the supplied key is not a Personal Space.
     */
    public static ServerLevel loadDimension(
            MinecraftServer server,
            ResourceKey<Level> dimension
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(dimension, "dimension");

        if (!isPersonalSpaceDimension(dimension.location())) {
            return null;
        }

        ServerLevel loaded = server.getLevel(dimension);
        if (loaded != null) {
            return loaded;
        }
        if (!PSDimensions.levelExists(server, dimension)) {
            return null;
        }

        return PSDimensions.getOrCreate(server, dimension);
    }

    /**
     * Flushes the supplied Personal Space level before an external archive or
     * migration operation reads its files.
     */
    public static void saveDimension(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        level.save(null, true, false);
    }

    public static PersonalSpaceClusterResult handleCreated(
            PersonalSpaceCreationContext context
    ) {
        Registration current = registration;
        if (current == null) {
            return PersonalSpaceClusterResult.CONTINUE;
        }

        try {
            PersonalSpaceClusterResult result =
                    current.handler.onPersonalSpaceCreated(context);
            return result == null
                    ? PersonalSpaceClusterResult.CONTINUE
                    : result;
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.error(
                    "Personal Space cluster integration {} failed while handling creation",
                    current.integrationId,
                    throwable
            );
            return PersonalSpaceClusterResult.CONTINUE;
        }
    }

    public static PersonalSpaceClusterResult handlePortalTravel(
            PersonalSpacePortalTravelContext context
    ) {
        Registration current = registration;
        if (current == null) {
            return PersonalSpaceClusterResult.CONTINUE;
        }

        try {
            PersonalSpaceClusterResult result =
                    current.handler.onPortalTravel(context);
            return result == null
                    ? PersonalSpaceClusterResult.CONTINUE
                    : result;
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.error(
                    "Personal Space cluster integration {} failed while handling portal travel",
                    current.integrationId,
                    throwable
            );
            return PersonalSpaceClusterResult.CONTINUE;
        }
    }

    private record Registration(
            String integrationId,
            PersonalSpaceClusterHandler handler
    ) {
    }
}
