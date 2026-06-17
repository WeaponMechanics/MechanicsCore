package me.deecaad.core.mechanics.scope;

import org.jetbrains.annotations.NotNull;

/**
 * The result type of a variable or expression. Either a number or a string.
 */
public sealed interface Value permits Value.NumberValue, Value.StringValue {

    double asNumber();

    @NotNull String asString();

    boolean asBoolean();

    static Value of(double number) {
        return new NumberValue(number);
    }

    static Value of(boolean bool) {
        return new NumberValue(bool ? 1 : 0);
    }

    static Value of(@NotNull String string) {
        return new StringValue(string);
    }

    record NumberValue(double value) implements Value {
        @Override
        public double asNumber() {
            return value;
        }

        @Override
        public @NotNull String asString() {
            // Print whole numbers without a trailing ".0".
            if (value == Math.rint(value) && !Double.isInfinite(value))
                return String.valueOf((long) value);
            return String.valueOf(value);
        }

        @Override
        public boolean asBoolean() {
            return value != 0;
        }
    }

    record StringValue(String value) implements Value {
        @Override
        public double asNumber() {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        @Override
        public @NotNull String asString() {
            return value;
        }

        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }
    }
}
