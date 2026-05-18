package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An axis-aligned cube centered at the emitter origin with edge length {@code size}. Samples are
 * uniform random points; {@code hollow} restricts them to the surface (one of six faces). The
 * emission direction is zero, so the emitter falls back to its own direction provider.
 */
public final class CubeShape implements Shape {

    private final double size;
    private final boolean hollow;

    public CubeShape(double size, boolean hollow) {
        if (size < 0)
            throw new IllegalArgumentException("size must be >= 0, got " + size);
        this.size = size;
        this.hollow = hollow;
    }

    public CubeShape(double size) {
        this(size, false);
    }

    public double getSize() {
        return size;
    }

    public boolean isHollow() {
        return hollow;
    }

    @Override
    public @NotNull ShapePoint getPoint(double t) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double half = size / 2.0;

        if (!hollow) {
            return new ShapePoint(
                new Vector(rng.nextDouble(-half, half), rng.nextDouble(-half, half), rng.nextDouble(-half, half)),
                new Vector(0, 0, 0));
        }

        // Surface sample: pick a face (one of 6), then a uniform point on it.
        int face = rng.nextInt(6);
        double a = rng.nextDouble(-half, half);
        double b = rng.nextDouble(-half, half);
        Vector offset = switch (face) {
            case 0 -> new Vector(+half, a, b);
            case 1 -> new Vector(-half, a, b);
            case 2 -> new Vector(a, +half, b);
            case 3 -> new Vector(a, -half, b);
            case 4 -> new Vector(a, b, +half);
            default -> new Vector(a, b, -half);
        };
        return new ShapePoint(offset, new Vector(0, 0, 0));
    }
}
