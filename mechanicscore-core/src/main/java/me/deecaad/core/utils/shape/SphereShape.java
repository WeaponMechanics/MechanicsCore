package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A sphere centered at the emitter origin. When {@code hollow} is {@code true}, samples lie on the
 * surface; otherwise they fill the volume. Sampling is random; {@code t} is ignored. The emission
 * direction is the radial outward normal.
 */
public final class SphereShape implements Shape {

    private final double radius;
    private final boolean hollow;

    public SphereShape(double radius, boolean hollow) {
        if (radius < 0)
            throw new IllegalArgumentException("radius must be >= 0, got " + radius);
        this.radius = radius;
        this.hollow = hollow;
    }

    public SphereShape(double radius) {
        this(radius, true);
    }

    public double getRadius() {
        return radius;
    }

    public boolean isHollow() {
        return hollow;
    }

    @Override
    public @NotNull ShapePoint getPoint(double t) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Uniform direction on the unit sphere.
        double z = rng.nextDouble(-1.0, 1.0);
        double phi = rng.nextDouble(0.0, Math.PI * 2.0);
        double r = Math.sqrt(1.0 - z * z);
        Vector direction = new Vector(r * Math.cos(phi), r * Math.sin(phi), z);

        double radial = hollow ? radius : radius * Math.cbrt(rng.nextDouble());
        Vector offset = direction.clone().multiply(radial);
        return new ShapePoint(offset, direction);
    }
}
