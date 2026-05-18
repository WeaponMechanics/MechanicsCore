package me.deecaad.core.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Something that can participate in a {@link Transform} hierarchy. Implementing classes only need
 * to expose their {@link Transform} node; the hierarchy (parent/children) lives inside the
 * {@link Transform} itself.
 */
public interface TransformLike {

    /**
     * The transform node that places this object in a transform hierarchy.
     *
     * @return The non-null transform.
     */
    @NotNull Transform getTransform();

    /**
     * Called when this object's world transform may have changed (e.g. a parent moved). Apply it to
     * the real world. Implementations must not mutate {@link #getTransform()} from here.
     */
    default void applyTransform() {
    }

    /**
     * Called once per tick by a ticking system (e.g. {@code me.deecaad.core.tick.TickManager} via
     * {@code TransformTree}). Dynamic transforms (ones that track an external source or animate
     * over time) override this to recompute their local transform and propagate the change to
     * children. Implementations must not restructure the tree from here.
     */
    default void update() {
    }
}
