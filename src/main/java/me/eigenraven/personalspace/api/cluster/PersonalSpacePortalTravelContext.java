package me.eigenraven.personalspace.api.cluster;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes attempted travel through a Personal Space portal.
 */
public final class PersonalSpacePortalTravelContext {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final ResourceKey<Level> dimension;
    private final BlockPos destination;
    private final float yaw;
    private final float pitch;
    private final Runnable localContinuation;
    private final AtomicBoolean continued = new AtomicBoolean();

    public PersonalSpacePortalTravelContext(
            MinecraftServer server,
            ServerPlayer player,
            ResourceKey<Level> dimension,
            BlockPos destination,
            float yaw,
            float pitch,
            Runnable localContinuation
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.player = Objects.requireNonNull(player, "player");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.destination = Objects.requireNonNull(destination, "destination").immutable();
        this.yaw = yaw;
        this.pitch = pitch;
        this.localContinuation = Objects.requireNonNull(
                localContinuation,
                "localContinuation"
        );
    }

    public MinecraftServer server() {
        return server;
    }

    public ServerPlayer player() {
        return player;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public BlockPos destination() {
        return destination;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    /**
     * Resumes the original local portal teleport exactly once on the server thread.
     */
    public void continueLocally() {
        if (!continued.compareAndSet(false, true)) {
            return;
        }

        if (server.isSameThread()) {
            localContinuation.run();
        } else {
            server.execute(localContinuation);
        }
    }
}
