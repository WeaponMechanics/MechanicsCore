package me.deecaad.core.emitter;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3fc;

/**
 * Read-only view of one live emission, passed to user-supplied kill/onKilled hooks on a
 * {@link DisplayEntityEmitter}.
 */
public sealed interface EmittedDisplay permits DisplayEntityEmitter.Emitted {

    int age();

    int lifetime();

    /**
     * The entity's spawn location plus the accumulated translation offset. Allocates a fresh
     * {@link Location}.
     */
    @NotNull Location worldLocation();

    /**
     * Read-only view of the accumulated translation offset (world-space when the entity was spawned
     * with yaw=pitch=0, which the emitter guarantees).
     */
    @NotNull Vector3fc translationOffset();

    /**
     * The entity's current per-tick velocity. Defensive clone.
     */
    @NotNull Vector velocity();

    /**
     * The scale vector from the most recently pushed transformation, or identity if nothing has been
     * pushed yet.
     */
    @NotNull Vector3fc currentScale();
}
