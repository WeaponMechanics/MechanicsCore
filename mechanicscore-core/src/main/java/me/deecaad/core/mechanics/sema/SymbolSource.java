package me.deecaad.core.mechanics.sema;

import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Resolves mechanic/targeter/condition prototypes by name for sema. Abstracts
 * the Bukkit-backed registries so sema (and its tests) do not depend on the
 * Bukkit {@code Registry} type, which cannot class-load without a server.
 */
public interface SymbolSource {

    @Nullable Mechanic mechanic(@NotNull String name);

    @Nullable Targeter targeter(@NotNull String name);

    @Nullable Condition condition(@NotNull String name);

    @NotNull Set<String> mechanicNames();

    @NotNull Set<String> targeterNames();

    @NotNull Set<String> conditionNames();

    /**
     * Names of globally-defined blocks (from the {@code mechanics/} folder) that a
     * call can resolve to. Defaults to none, so sema only sees the blocks declared
     * in the program being analyzed unless an implementation exposes globals.
     */
    default @NotNull Set<String> blockNames() {
        return Set.of();
    }
}
