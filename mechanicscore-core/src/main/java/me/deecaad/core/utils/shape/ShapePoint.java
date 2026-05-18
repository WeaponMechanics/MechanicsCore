package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A sample from a {@link Shape}: an offset (local-space, relative to the emitter origin) and a
 * direction (local-space emission normal). A zero {@code direction} signals "fall back to the
 * emitter's own direction provider."
 *
 * @param offset The non-null local-space offset.
 * @param direction The non-null local-space emission normal; the zero vector means
 *                  "use the emitter's direction."
 */
public record ShapePoint(@NotNull Vector offset, @NotNull Vector direction) {

    /**
     * Creates a {@code ShapePoint} with the given offset and a zero direction (the consumer should
     * fall back to its own direction).
     *
     * @param offset The non-null offset (defensively copied).
     * @return The non-null shape point.
     */
    public static @NotNull ShapePoint at(@NotNull Vector offset) {
        return new ShapePoint(offset.clone(), new Vector(0, 0, 0));
    }
}
