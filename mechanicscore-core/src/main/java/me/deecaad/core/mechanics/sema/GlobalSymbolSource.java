package me.deecaad.core.mechanics.sema;

import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.mechanics.Targeters;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.program.GlobalBlocks;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.MutableRegistry;
import me.deecaad.core.utils.RegistryUtil;
import org.bukkit.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The production {@link SymbolSource} backed by the global registries. Only
 * loaded at runtime (with a Bukkit server present).
 */
public final class GlobalSymbolSource implements SymbolSource {

    private final @NotNull Set<String> providedContexts;

    public GlobalSymbolSource() {
        this(Set.of());
    }

    /**
     * @param providedContexts Context names the cast environment seeds beyond {@code source}/
     *                         {@code target} (e.g. an explosion's {@code AllEntitiesHitByExplosion}).
     *                         Must match the names seeded into the {@link me.deecaad.core.mechanics.scope.CastScope}
     *                         at cast time, or a reference compiles yet resolves empty at runtime.
     */
    public GlobalSymbolSource(@NotNull Set<String> providedContexts) {
        this.providedContexts = Set.copyOf(providedContexts);
    }

    @Override
    public @NotNull Set<String> providedContexts() {
        return providedContexts;
    }

    @Override
    public @Nullable Mechanic mechanic(@NotNull String name) {
        return RegistryUtil.matchAny(Mechanics.REGISTRY, name);
    }

    @Override
    public @Nullable Targeter targeter(@NotNull String name) {
        return RegistryUtil.matchAny(Targeters.REGISTRY, name);
    }

    @Override
    public @Nullable Condition condition(@NotNull String name) {
        return RegistryUtil.matchAny(Conditions.REGISTRY, name);
    }

    @Override
    public @NotNull Set<String> mechanicNames() {
        return names(Mechanics.REGISTRY);
    }

    @Override
    public @NotNull Set<String> targeterNames() {
        return names(Targeters.REGISTRY);
    }

    @Override
    public @NotNull Set<String> conditionNames() {
        return names(Conditions.REGISTRY);
    }

    @Override
    public @NotNull Set<String> blockNames() {
        return GlobalBlocks.names();
    }

    private static <T extends Keyed> @NotNull Set<String> names(@NotNull MutableRegistry<T> registry) {
        Set<String> names = new LinkedHashSet<>();
        for (T entry : registry)
            names.add(entry.getKey().getKey());
        return names;
    }
}
