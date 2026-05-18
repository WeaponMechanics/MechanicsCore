package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A single point at the emitter origin. {@code direction} is zero so the emitter falls back to its
 * own direction provider.
 */
public final class PointShape implements Shape {

    @Override
    public @NotNull ShapePoint getPoint(double t) {
        return new ShapePoint(new Vector(0, 0, 0), new Vector(0, 0, 0));
    }
}
