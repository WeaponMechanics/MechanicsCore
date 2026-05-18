package me.deecaad.core.tick;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Something that wants to be driven once per tick by a {@link TickManager}. Implementations should
 * return {@code true} from {@link #tick()} when they are finished, so the manager can remove and
 * clean them up.
 *
 * <p>
 * On Folia, the manager schedules per-region tasks based on {@link #getTickLocation()}, so it must
 * return a non-null location. On non-Folia servers the location is ignored.
 */
public interface Tickable {

    /**
     * Performs one tick of work. Return {@code true} when finished; the manager will cancel and
     * call {@link #remove()}.
     *
     * @return true to stop ticking.
     */
    boolean tick();

    /**
     * The location used by {@link TickManager} implementations to choose a region for scheduling.
     * Must return non-null on Folia.
     *
     * @return The location, or {@code null} if region-agnostic (non-Folia only).
     */
    default @Nullable Location getTickLocation() {
        return null;
    }

    /**
     * Cleanup hook. Called by the {@link TickManager} when {@link #tick()} returns {@code true}, or
     * during {@link TickManager#shutdown()}. Default is a no-op.
     */
    default void remove() {
    }
}
