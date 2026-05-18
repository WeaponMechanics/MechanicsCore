package me.deecaad.core.transition;

import me.deecaad.core.transition.TransitionPlayback.Segment;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Combines up to three transform-component transitions (translation, scale, rotation) for one
 * display entity into a single schedule of {@link Transformation} segments. Required because
 * {@code FakeDisplayEntity.setTransformation} replaces translation + scale + rotation atomically,
 * so the three transitions must be sampled at a unified tick set.
 *
 * <p>
 * All non-null transitions must have the same {@code durationTicks}. The right-side rotation in
 * the emitted {@link Transformation} is always identity.
 */
public final class DisplayTransformTrack {

    private static final Vector3f IDENTITY_TRANSLATION = new Vector3f(0, 0, 0);
    private static final Vector3f IDENTITY_SCALE = new Vector3f(1, 1, 1);
    private static final Quaternionf IDENTITY_ROTATION = new Quaternionf();

    private final @Nullable Transition<Vector3f> translation;
    private final @Nullable Transition<Vector3f> scale;
    private final @Nullable Transition<Quaterniond> rotation;
    private final int durationTicks;

    /**
     * @param translation Optional translation track.
     * @param scale Optional scale track.
     * @param rotation Optional rotation track (applied as the left rotation).
     */
    public DisplayTransformTrack(
        @Nullable Transition<Vector3f> translation,
        @Nullable Transition<Vector3f> scale,
        @Nullable Transition<Quaterniond> rotation
    ) {
        if (translation == null && scale == null && rotation == null)
            throw new IllegalArgumentException("DisplayTransformTrack needs at least one non-null transition");

        int duration = -1;
        if (translation != null) duration = translation.getDurationTicks();
        if (scale != null) {
            if (duration != -1 && scale.getDurationTicks() != duration)
                throw new IllegalArgumentException("All transitions in a DisplayTransformTrack must share durationTicks");
            duration = scale.getDurationTicks();
        }
        if (rotation != null) {
            if (duration != -1 && rotation.getDurationTicks() != duration)
                throw new IllegalArgumentException("All transitions in a DisplayTransformTrack must share durationTicks");
            duration = rotation.getDurationTicks();
        }

        this.translation = translation;
        this.scale = scale;
        this.rotation = rotation;
        this.durationTicks = duration;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    /**
     * Builds the {@link Transformation} that should be active at the given tick. Useful for setting
     * a display's initial state before scheduling segments.
     *
     * @param tick The tick to sample.
     * @return The non-null transformation.
     */
    public @NotNull Transformation valueAt(int tick) {
        Vector3f t = translation != null ? translation.evaluateAtTick(tick) : IDENTITY_TRANSLATION;
        Vector3f s = scale != null ? scale.evaluateAtTick(tick) : IDENTITY_SCALE;
        Quaternionf left = rotation != null ? new Quaternionf(rotation.evaluateAtTick(tick)) : IDENTITY_ROTATION;
        return new Transformation(new Vector3f(t), new Quaternionf(left), new Vector3f(s), new Quaternionf());
    }

    /**
     * Builds the union schedule of segments across the non-null component transitions. Each
     * segment's value is the combined {@link Transformation} sampled at that segment's end tick.
     *
     * @return A non-null, ascending-by-tickOffset list of segments. May be empty if there are no
     *         intervals to schedule.
     */
    public @NotNull List<Segment<Transformation>> schedule() {
        TreeSet<Integer> ticks = new TreeSet<>();
        if (translation != null) ticks.addAll(TransitionPlayback.sampleTicks(translation));
        if (scale != null) ticks.addAll(TransitionPlayback.sampleTicks(scale));
        if (rotation != null) ticks.addAll(TransitionPlayback.sampleTicks(rotation));

        List<Segment<Transformation>> out = new ArrayList<>(ticks.size());
        int prev = -1;
        for (int tick : ticks) {
            if (prev >= 0)
                out.add(new Segment<>(prev, tick - prev, valueAt(tick)));
            prev = tick;
        }
        return out;
    }
}
