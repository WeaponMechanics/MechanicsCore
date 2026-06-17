package me.deecaad.core.mechanics.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

/**
 * The built-in expression function library. There are no user-defined functions
 * in the expression language; this fixed set covers common damage-formula needs.
 */
public final class ExpressionFunctions {

    /**
     * A function with an inclusive arity range ({@code maxArgs < 0} means varargs).
     */
    public record Definition(int minArgs, int maxArgs, @NotNull ToDoubleFunction<double[]> apply) {
        public boolean acceptsArgCount(int count) {
            return count >= minArgs && (maxArgs < 0 || count <= maxArgs);
        }

        public @NotNull String arityDescription() {
            if (maxArgs < 0)
                return "at least " + minArgs;
            if (minArgs == maxArgs)
                return String.valueOf(minArgs);
            return minArgs + " to " + maxArgs;
        }
    }

    private static final Map<String, Definition> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("min", new Definition(1, -1, args -> {
            double result = args[0];
            for (double arg : args)
                result = Math.min(result, arg);
            return result;
        }));
        REGISTRY.put("max", new Definition(1, -1, args -> {
            double result = args[0];
            for (double arg : args)
                result = Math.max(result, arg);
            return result;
        }));
        REGISTRY.put("abs", new Definition(1, 1, args -> Math.abs(args[0])));
        REGISTRY.put("sqrt", new Definition(1, 1, args -> Math.sqrt(args[0])));
        REGISTRY.put("floor", new Definition(1, 1, args -> Math.floor(args[0])));
        REGISTRY.put("ceil", new Definition(1, 1, args -> Math.ceil(args[0])));
        REGISTRY.put("round", new Definition(1, 1, args -> Math.round(args[0])));
        REGISTRY.put("clamp", new Definition(3, 3, args -> Math.max(args[1], Math.min(args[2], args[0]))));
        REGISTRY.put("random", new Definition(2, 2, args -> {
            double low = Math.min(args[0], args[1]);
            double high = Math.max(args[0], args[1]);
            if (low == high)
                return low;
            return ThreadLocalRandom.current().nextDouble(low, high);
        }));
    }

    private ExpressionFunctions() {
    }

    public static @Nullable Definition get(@NotNull String name) {
        return REGISTRY.get(name);
    }

    public static boolean exists(@NotNull String name) {
        return REGISTRY.containsKey(name);
    }

    public static @NotNull java.util.Set<String> names() {
        return REGISTRY.keySet();
    }
}
