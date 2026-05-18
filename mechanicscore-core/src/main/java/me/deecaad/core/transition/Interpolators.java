package me.deecaad.core.transition;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3f;

/**
 * Built-in {@link Interpolator} constants for the value types typically transitioned: scalars,
 * Bukkit and JOML vectors, JOML quaternions (slerp), plus a stepped interpolator for discrete
 * types like {@code BlockData} or {@code ItemStack}.
 */
public final class Interpolators {

    private Interpolators() {
    }

    public static final @NotNull Interpolator<Double> DOUBLE =
        (a, b, t) -> a + (b - a) * t;

    public static final @NotNull Interpolator<Vector> VECTOR =
        (a, b, t) -> new Vector(
            a.getX() + (b.getX() - a.getX()) * t,
            a.getY() + (b.getY() - a.getY()) * t,
            a.getZ() + (b.getZ() - a.getZ()) * t);

    public static final @NotNull Interpolator<Vector3f> VECTOR3F =
        (a, b, t) -> new Vector3f(a).lerp(b, (float) t);

    public static final @NotNull Interpolator<Quaterniond> QUATERNIOND =
        (a, b, t) -> new Quaterniond(a).slerp(b, t);

    private static final @NotNull Interpolator<Object> STEPPED = new Interpolator<>() {
        @Override public @NotNull Object interpolate(@NotNull Object a, @NotNull Object b, double t) {
            return a;
        }

        @Override public boolean isContinuous() {
            return false;
        }
    };

    /**
     * A stepped interpolator for any type {@code T}. Returns {@code a} until a discrete switch is
     * scheduled by the playback helper.
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull Interpolator<T> stepped() {
        return (Interpolator<T>) STEPPED;
    }
}
