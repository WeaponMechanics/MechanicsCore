package me.deecaad.core.transition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Realizes a {@link Transition} into a schedule of {@link Segment} pushes.
 *
 * <p>
 * Continuous transitions are sampled into segments whose density matches the easing curve: linear
 * segments contribute only their endpoints, while curved segments contribute additional intermediate
 * samples. Consumers push each segment in turn, so client-side interpolation (e.g. a display
 * entity's {@code setInterpolationDuration}) reproduces the eased curve as a chain of linear lerps.
 *
 * <p>
 * Stepped transitions bypass sampling: one segment per keyframe, {@code interpolationDuration = 0}.
 */
public final class TransitionPlayback {

    private static final int CURVED_SAMPLE_INTERVAL_TICKS = 4;
    private static final int MAX_SAMPLES_PER_SEGMENT = 10;

    private TransitionPlayback() {
    }

    /**
     * One step in a transition schedule.
     *
     * @param tickOffset Tick (relative to the transition's start) at which this segment begins.
     * @param interpolationDuration Number of ticks the consumer should pass to native interpolation
     *                              while reaching {@code value}. {@code 0} = instant set.
     * @param value The target value at {@code tickOffset + interpolationDuration}.
     */
    public record Segment<T>(int tickOffset, int interpolationDuration, @NotNull T value) {
    }

    /**
     * Builds a schedule for {@code transition}. Picks stepped or continuous sampling based on the
     * transition's interpolator.
     *
     * @param transition The non-null transition.
     * @return A non-null, non-empty, ascending-by-tickOffset list of segments.
     */
    public static <T> @NotNull List<Segment<T>> sample(@NotNull Transition<T> transition) {
        return transition.isStepped() ? sampleStepped(transition) : sampleContinuous(transition);
    }

    private static <T> @NotNull List<Segment<T>> sampleStepped(@NotNull Transition<T> transition) {
        int duration = transition.getDurationTicks();
        List<Keyframe<T>> keyframes = transition.getKeyframes();
        List<Segment<T>> out = new ArrayList<>(keyframes.size());
        for (Keyframe<T> kf : keyframes) {
            int tick = (int) Math.round(kf.time() * duration);
            out.add(new Segment<>(tick, 0, kf.value()));
        }
        return out;
    }

    private static <T> @NotNull List<Segment<T>> sampleContinuous(@NotNull Transition<T> transition) {
        int duration = transition.getDurationTicks();
        List<Keyframe<T>> keyframes = transition.getKeyframes();

        TreeSet<Integer> sampleTicks = sampleTicks(transition);

        List<Segment<T>> out = new ArrayList<>(sampleTicks.size());
        int prev = -1;
        for (int tick : sampleTicks) {
            if (prev >= 0) {
                int interp = tick - prev;
                T value = transition.evaluateAtTick(tick);
                out.add(new Segment<>(prev, interp, value));
            }
            prev = tick;
        }
        return out;
    }

    /**
     * Computes the union of authored keyframe ticks plus extra samples inside non-linear segments.
     * Exposed for {@link DisplayTransformTrack} to combine tick sets across multiple transitions.
     *
     * @param transition The non-null transition.
     * @return The non-null sorted set of sample ticks.
     */
    static @NotNull TreeSet<Integer> sampleTicks(@NotNull Transition<?> transition) {
        int duration = transition.getDurationTicks();
        List<? extends Keyframe<?>> keyframes = transition.getKeyframes();

        TreeSet<Integer> out = new TreeSet<>();
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe<?> a = keyframes.get(i);
            Keyframe<?> b = keyframes.get(i + 1);
            int aTick = (int) Math.round(a.time() * duration);
            int bTick = (int) Math.round(b.time() * duration);
            out.add(aTick);
            out.add(bTick);

            if (a.easingToNext() != Easing.LINEAR && bTick > aTick) {
                int segLen = bTick - aTick;
                int samples = Math.min(MAX_SAMPLES_PER_SEGMENT, Math.max(1, (int) Math.ceil(segLen / (double) CURVED_SAMPLE_INTERVAL_TICKS)));
                for (int s = 1; s < samples; s++) {
                    int sampleTick = aTick + (int) Math.round((double) s / samples * segLen);
                    out.add(sampleTick);
                }
            }
        }
        return out;
    }
}
