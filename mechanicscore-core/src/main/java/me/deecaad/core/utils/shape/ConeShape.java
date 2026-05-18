package me.deecaad.core.utils.shape;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A cone with apex at the emitter origin, axis along local +Z, half-angle {@code halfAngleRadians}
 * and {@code length}. Samples are random uniform-solid-angle directions within the cone; the offset
 * places each sample on the cone's "spread surface" at the chosen direction times {@code length}.
 * The emission direction is the chosen unit direction (outward along the cone).
 */
public final class ConeShape implements Shape {

    private final double halfAngleRadians;
    private final double length;

    /**
     * @param halfAngleRadians Half the cone's opening angle, in radians. {@code 0} = pure +Z;
     *                         {@code π/2} = full hemisphere.
     * @param length Distance from the apex along the chosen direction at which the offset is placed.
     */
    public ConeShape(double halfAngleRadians, double length) {
        if (halfAngleRadians < 0 || halfAngleRadians > Math.PI)
            throw new IllegalArgumentException("halfAngleRadians must be in [0, π]");
        if (length < 0)
            throw new IllegalArgumentException("length must be >= 0");
        this.halfAngleRadians = halfAngleRadians;
        this.length = length;
    }

    public double getHalfAngleRadians() {
        return halfAngleRadians;
    }

    public double getLength() {
        return length;
    }

    @Override
    public @NotNull ShapePoint getPoint(double t) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Uniform sample within the cone's solid angle around +Z.
        double cosTheta = 1.0 - rng.nextDouble() * (1.0 - Math.cos(halfAngleRadians));
        double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        double phi = rng.nextDouble(0.0, Math.PI * 2.0);

        Vector direction = new Vector(sinTheta * Math.cos(phi), sinTheta * Math.sin(phi), cosTheta);
        Vector offset = direction.clone().multiply(length);
        return new ShapePoint(offset, direction);
    }
}
