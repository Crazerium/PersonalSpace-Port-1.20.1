package me.eigenraven.personalspace.api.cluster;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a newly created Personal Space before the creator enters it.
 */
public final class PersonalSpaceCreationContext {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final ResourceKey<Level> dimension;
    private final ServerLevel createdLevel;
    private final BlockPos destination;
    private final float yaw;
    private final float pitch;
    private final Runnable localContinuation;
    private final AtomicBoolean continued = new AtomicBoolean();

    public PersonalSpaceCreationContext(
            MinecraftServer server,
            ServerPlayer player,
            ResourceKey<Level> dimension,
            ServerLevel createdLevel,
            BlockPos destination,
            float yaw,
            float pitch,
            Runnable localContinuation
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.player = Objects.requireNonNull(player, "player");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.createdLevel = Objects.requireNonNull(createdLevel, "createdLevel");
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

    public ServerLevel createdLevel() {
        return createdLevel;
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
     * Resumes the original local teleport exactly once on the server thread.
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
