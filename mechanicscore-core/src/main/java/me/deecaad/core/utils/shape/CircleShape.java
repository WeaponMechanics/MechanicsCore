package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A circle centered at the emitter origin, lying in the plane perpendicular to {@link Axis axis}.
 * {@code t} parameterizes the angle around the circle ({@code t * 2π}), giving deterministic, evenly
 * distributed points. The emission direction is the radial outward normal in the circle's plane.
 */
public final class CircleShape implements Shape {

    public enum Axis { X, Y, Z }

    private final double radius;
    private final Axis axis;

    public CircleShape(double radius, @NotNull Axis axis) {
        if (radius < 0)
            throw new IllegalArgumentException("radius must be >= 0, got " + radius);
        this.radius = radius;
        this.axis = axis;
    }

    public CircleShape(double radius) {
        this(radius, Axis.Y);
    }

    public double getRadius() {
        return radius;
    }

    public @NotNull Axis getAxis() {
        return axis;
    }

    @Override
    public @NotNull ShapePoint getPoint(double t) {
        double angle = t * Math.PI * 2.0;
        double c = Math.cos(angle);
        double s = Math.sin(angle);

        Vector direction = switch (axis) {
            case X -> new Vector(0.0, c, s);
            case Y -> new Vector(c, 0.0, s);
            case Z -> new Vector(c, s, 0.0);
        };
        Vector offset = direction.clone().multiply(radius);
        return new ShapePoint(offset, direction);
    }
}
