package me.deecaad.core.transition;

import java.util.function.DoubleUnaryOperator;

/**
 * Maps a normalized time {@code t} in {@code [0, 1]} to an eased value in {@code [0, 1]}. Used by
 * {@link Keyframe} to shape how a {@link Transition} segment progresses.
 */
public enum Easing implements DoubleUnaryOperator {

    LINEAR {
        @Override public double applyAsDouble(double t) {
            return t;
        }
    },

    EASE_IN {
        @Override public double applyAsDouble(double t) {
            return t * t;
        }
    },

    EASE_OUT {
        @Override public double applyAsDouble(double t) {
            double u = 1.0 - t;
            return 1.0 - u * u;
        }
    },

    EASE_IN_OUT {
        @Override public double applyAsDouble(double t) {
            if (t < 0.5) return 2.0 * t * t;
            double u = -2.0 * t + 2.0;
            return 1.0 - 0.5 * u * u;
        }
    },

    EASE_IN_CUBIC {
        @Override public double applyAsDouble(double t) {
            return t * t * t;
        }
    },

    EASE_OUT_CUBIC {
        @Override public double applyAsDouble(double t) {
            double u = 1.0 - t;
            return 1.0 - u * u * u;
        }
    },

    EASE_IN_OUT_CUBIC {
        @Override public double applyAsDouble(double t) {
            if (t < 0.5) return 4.0 * t * t * t;
            double u = -2.0 * t + 2.0;
            return 1.0 - 0.5 * u * u * u;
        }
    };
}
