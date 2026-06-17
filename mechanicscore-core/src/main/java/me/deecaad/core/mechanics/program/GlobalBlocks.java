package me.deecaad.core.mechanics.program;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The registry of globally-defined mechanic blocks loaded from the
 * {@code mechanics/} folder. Any {@link Program} resolves block calls against this
 * registry when the name is not a local block, so a block defined in one file can
 * be called from any other mechanics list (nesting, recursion, macros).
 *
 * <p>
 * Block names are matched case-insensitively (keyed by lowercase). Loads install a
 * fresh immutable snapshot atomically; casts on region threads read it lock-free.
 */
public final class GlobalBlocks {

    private static volatile Map<String, MechanicBlock> blocks = Map.of();

    private GlobalBlocks() {
    }

    /**
     * Replaces the registry with the given compiled blocks (keys are block names,
     * any case). Called on (re)load.
     */
    public static void install(@NotNull Map<String, MechanicBlock> compiled) {
        Map<String, MechanicBlock> copy = new HashMap<>();
        for (Map.Entry<String, MechanicBlock> entry : compiled.entrySet())
            copy.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        blocks = Map.copyOf(copy);
    }

    public static void clear() {
        blocks = Map.of();
    }

    public static @Nullable MechanicBlock get(@NotNull String name) {
        return blocks.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * The (lowercase) names of all registered global blocks.
     */
    public static @NotNull Set<String> names() {
        return blocks.keySet();
    }
}
