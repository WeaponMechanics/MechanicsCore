package me.deecaad.core.utils.shape;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A geometric region that produces emission points + directions. Implementations are immutable and
 * may sample randomly (ignoring {@code t}) or deterministically (using {@code t} as a normalized
 * parameter, e.g. position along a line or angle around a circle).
 *
 * <p>
 * Shapes have no scheduling, ticking, or world coordinate concept: they return offsets relative to
 * an emitter origin. The emitter rotates and translates these into world space.
 */
public interface Shape {

    /**
     * Returns one sample from this shape. For random shapes (sphere, cone, cube) {@code t} is
     * ignored; for deterministic shapes (point, line, circle) {@code t} parameterizes the sample.
     *
     * @param t Normalized parameter in {@code [0, 1)}.
     * @return The non-null shape point.
     */
    @NotNull ShapePoint getPoint(double t);

    /**
     * Returns {@code count} samples. By default, samples at evenly distributed {@code t} values.
     * Random shapes naturally ignore {@code t} and produce {@code count} random samples.
     *
     * @param count The non-negative number of samples.
     * @return A non-null list of shape points.
     */
    default @NotNull List<ShapePoint> getPoints(int count) {
        List<ShapePoint> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            out.add(getPoint(count <= 1 ? 0.0 : (double) i / count));
        return out;
    }
}
