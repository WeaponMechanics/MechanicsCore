package me.deecaad.core.transition;

import org.jetbrains.annotations.NotNull;

/**
 * One sample in a {@link Transition}: the normalized {@code time} at which {@code value} should be
 * reached, and the {@code easingToNext} curve that shapes how the previous segment approaches this
 * keyframe.
 *
 * @param time Normalized time in {@code [0, 1]}. The first keyframe in a transition must have
 *             {@code time == 0}; the last must have {@code time == 1}.
 * @param value The non-null value at {@code time}.
 * @param easingToNext The non-null easing applied along the segment from THIS keyframe to the next.
 *                     Ignored on the last keyframe.
 */
public record Keyframe<T>(double time, @NotNull T value, @NotNull Easing easingToNext) {
}
