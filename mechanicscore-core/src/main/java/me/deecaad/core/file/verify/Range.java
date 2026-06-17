package me.deecaad.core.file.verify;

import org.jetbrains.annotations.Nullable;

/**
 * Inclusive numeric bounds for an {@link KeyType#INT} or {@link KeyType#DOUBLE} key. A null bound
 * means unbounded on that side.
 */
public record Range(@Nullable Double min, @Nullable Double max) {

    public static Range of(@Nullable Double min, @Nullable Double max) {
        return new Range(min, max);
    }

    public static Range of(@Nullable Integer min, @Nullable Integer max) {
        return new Range(min == null ? null : min.doubleValue(), max == null ? null : max.doubleValue());
    }

    public boolean contains(double value) {
        if (min != null && value < min)
            return false;
        return max == null || !(value > max);
    }
}
