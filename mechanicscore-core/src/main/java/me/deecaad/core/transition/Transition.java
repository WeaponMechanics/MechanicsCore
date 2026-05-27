package me.deecaad.core.transition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A value-over-time curve. Stores an ordered list of {@link Keyframe}s in normalized time and an
 * {@link Interpolator} used to blend between consecutive keyframes. Immutable.
 *
 * <p>
 * The first keyframe must have {@code time == 0} and the last must have {@code time == 1}, with
 * monotonically increasing times in between.
 *
 * @param <T> The value type.
 */
public final class Transition<T> {

    private final List<Keyframe<T>> keyframes;
    private final Interpolator<T> interpolator;
    private final int durationTicks;

    /**
     * @param keyframes The non-null keyframe list (copied, then sorted and validated).
     * @param interpolator The non-null interpolator for blending between consecutive keyframes.
     * @param durationTicks The duration in ticks; must be {@code > 0}. Used by
     *                      {@link #evaluateAtTick(int)} and playback helpers.
     */
    public Transition(@NotNull List<Keyframe<T>> keyframes, @NotNull Interpolator<T> interpolator, int durationTicks) {
        if (keyframes.isEmpty())
            throw new IllegalArgumentException("Transition needs at least one keyframe");
        if (durationTicks <= 0)
            throw new IllegalArgumentException("durationTicks must be > 0, got " + durationTicks);

        List<Keyframe<T>> copy = new ArrayList<>(keyframes);
        copy.sort(Comparator.comparingDouble(Keyframe::time));

        if (copy.getFirst().time() != 0.0)
            throw new IllegalArgumentException("First keyframe must have time == 0, got " + copy.getFirst().time());
        if (copy.getLast().time() != 1.0)
            throw new IllegalArgumentException("Last keyframe must have time == 1, got " + copy.getLast().time());
        for (int i = 1; i < copy.size(); i++) {
            if (copy.get(i).time() <= copy.get(i - 1).time())
                throw new IllegalArgumentException("Keyframe times must be strictly increasing");
        }

        this.keyframes = Collections.unmodifiableList(copy);
        this.interpolator = interpolator;
        this.durationTicks = durationTicks;
    }

    /**
     * @return The non-null immutable sorted keyframe list.
     */
    public @NotNull List<Keyframe<T>> getKeyframes() {
        return keyframes;
    }

    public @NotNull Interpolator<T> getInterpolator() {
        return interpolator;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    /**
     * @return {@code true} if the interpolator is stepped (no meaningful intermediate values).
     */
    public boolean isStepped() {
        return !interpolator.isContinuous();
    }

    /**
     * Evaluates the curve at normalized time {@code t}, clamped to {@code [0, 1]}.
     *
     * @param t The normalized time.
     * @return The non-null value.
     */
    public @NotNull T evaluate(double t) {
        if (t <= 0.0)
            return keyframes.getFirst().value();
        if (t >= 1.0)
            return keyframes.getLast().value();

        // Find segment [i, i+1] where keyframes[i].time <= t < keyframes[i+1].time.
        int i = 0;
        while (i < keyframes.size() - 1 && keyframes.get(i + 1).time() <= t)
            i++;

        Keyframe<T> a = keyframes.get(i);
        Keyframe<T> b = keyframes.get(i + 1);
        double u = (t - a.time()) / (b.time() - a.time());
        double eased = a.easingToNext().applyAsDouble(u);
        return interpolator.interpolate(a.value(), b.value(), eased);
    }

    /**
     * Convenience: evaluates at {@code tick / durationTicks}.
     *
     * @param tick The current tick.
     * @return The non-null value.
     */
    public @NotNull T evaluateAtTick(int tick) {
        return evaluate(tick / (double) durationTicks);
    }

    /**
     * Single-keyframe transition that returns {@code value} at every time.
     */
    public static <T> @NotNull Transition<T> constant(@NotNull T value, @NotNull Interpolator<T> interpolator, int durationTicks) {
        return new Transition<>(
            List.of(
                new Keyframe<>(0.0, value, Easing.LINEAR),
                new Keyframe<>(1.0, value, Easing.LINEAR)),
            interpolator,
            durationTicks);
    }

    /**
     * Two-keyframe transition from {@code from} to {@code to} with {@link Easing#LINEAR} easing.
     */
    public static <T> @NotNull Transition<T> lerp(@NotNull T from, @NotNull T to, @NotNull Interpolator<T> interpolator, int durationTicks) {
        return lerp(from, to, interpolator, durationTicks, Easing.LINEAR);
    }

    /**
     * Two-keyframe transition from {@code from} to {@code to} with the given easing applied across
     * the segment.
     */
    public static <T> @NotNull Transition<T> lerp(@NotNull T from, @NotNull T to, @NotNull Interpolator<T> interpolator, int durationTicks, @NotNull Easing easing) {
        return new Transition<>(
            List.of(
                new Keyframe<>(0.0, from, easing),
                new Keyframe<>(1.0, to, Easing.LINEAR)),
            interpolator,
            durationTicks);
    }

    /**
     * Three-keyframe pulse: {@code start} → {@code peak} (at t=0.5) → {@code end}, with {@code in}
     * easing on the rise and {@code out} easing on the fall.
     */
    public static <T> @NotNull Transition<T> pulse(@NotNull T start, @NotNull T peak, @NotNull T end, @NotNull Interpolator<T> interpolator, int durationTicks, @NotNull Easing in, @NotNull Easing out) {
        return new Transition<>(
            List.of(
                new Keyframe<>(0.0, start, in),
                new Keyframe<>(0.5, peak, out),
                new Keyframe<>(1.0, end, Easing.LINEAR)),
            interpolator,
            durationTicks);
    }
}
