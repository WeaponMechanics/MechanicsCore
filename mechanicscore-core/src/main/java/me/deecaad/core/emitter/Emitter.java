package me.deecaad.core.emitter;

import me.deecaad.core.tick.Tickable;
import me.deecaad.core.utils.TransformLike;

/**
 * A particle-system style emitter. Lives in the {@link me.deecaad.core.utils.Transform} hierarchy
 * (via {@link TransformLike}) so it can be attached/parented, and is driven by a
 * {@link me.deecaad.core.tick.TickManager} (via {@link Tickable}).
 */
public interface Emitter extends TransformLike, Tickable {

    /**
     * @return {@code true} when the emitter's duration has elapsed AND no live emitted items remain
     *         (so the tick manager can remove it).
     */
    boolean isFinished();
}
