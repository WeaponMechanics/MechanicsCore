package me.deecaad.core.transition;

import org.jetbrains.annotations.NotNull;

/**
 * Blends two values of type {@code T} by a normalized factor {@code t} in {@code [0, 1]}.
 * Interpolators are pure functions; implementations should not mutate {@code a} or {@code b}.
 *
 * <p>
 * Values that cannot be meaningfully blended (e.g. {@code BlockData}, {@code ItemStack}) should use
 * a stepped interpolator (see {@link Interpolators#stepped()}) which returns {@code a} and reports
 * {@link #isContinuous()} as {@code false}.
 */
@FunctionalInterface
public interface Interpolator<T> {

    /**
     * Returns a value between {@code a} and {@code b}. Implementations may clamp {@code t}.
     *
     * @param a The non-null start value.
     * @param b The non-null end value.
     * @param t The normalized blend factor.
     * @return The blended value.
     */
    @NotNull T interpolate(@NotNull T a, @NotNull T b, double t);

    /**
     * Whether this interpolator produces meaningful intermediate values (true) or merely returns
     * {@code a} until a discrete switch to {@code b} (false). Used by playback helpers to choose
     * between native interpolation and stepped scheduling.
     *
     * @return true if continuous.
     */
    default boolean isContinuous() {
        return true;
    }
}
