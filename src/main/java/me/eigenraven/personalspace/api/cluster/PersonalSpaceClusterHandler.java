package me.eigenraven.personalspace.api.cluster;

/**
 * Integration point for server clusters that route Personal Space dimensions
 * between multiple Minecraft servers.
 *
 * <p>Implementations must return quickly. Long-running database, archive, or
 * network work should be scheduled asynchronously. When an operation later
 * needs to continue on the current server, call {@code continueLocally()} on
 * the supplied context.</p>
 */
public interface PersonalSpaceClusterHandler {
    /**
     * Called after a new Personal Space has been fully initialized and linked
     * to its source portal, but before the creator is teleported into it.
     */
    default PersonalSpaceClusterResult onPersonalSpaceCreated(
            PersonalSpaceCreationContext context
    ) {
        return PersonalSpaceClusterResult.CONTINUE;
    }

    /**
     * Called before a Personal Space portal enters its destination dimension.
     * The destination can be either a Personal Space or a return dimension.
     */
    default PersonalSpaceClusterResult onPortalTravel(
            PersonalSpacePortalTravelContext context
    ) {
        return PersonalSpaceClusterResult.CONTINUE;
    }
}
