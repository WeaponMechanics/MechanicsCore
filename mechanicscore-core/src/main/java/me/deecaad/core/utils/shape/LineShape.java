package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A line segment from {@code from} to {@code to}, both expressed in emitter-local coordinates.
 * {@code t} parameterizes position along the line. {@code direction} is the zero vector, so the
 * emitter falls back to its own direction provider.
 */
public final class LineShape implements Shape {

    private final Vector from;
    private final Vector to;

    public LineShape(@NotNull Vector from, @NotNull Vector to) {
        this.from = from.clone();
        this.to = to.clone();
    }

    /**
     * Convenience constructor: a line of {@code length} along the local +Z axis.
     */
    public LineShape(double length) {
        this(new Vector(0, 0, 0), new Vector(0, 0, length));
    }

    public @NotNull Vector getFrom() {
        return from.clone();
    }

    public @NotNull Vector getTo() {
        return to.clone();
    }

    @Override
    public @NotNull ShapePoint getPoint(double t) {
        Vector offset = new Vector(
            from.getX() + (to.getX() - from.getX()) * t,
            from.getY() + (to.getY() - from.getY()) * t,
            from.getZ() + (to.getZ() - from.getZ()) * t);
        return new ShapePoint(offset, new Vector(0, 0, 0));
    }
}
