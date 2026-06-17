package me.deecaad.core.mechanics.expression;

import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves a dotted property read on a {@link Context}, e.g. {@code enemies.size},
 * {@code target.x}, {@code source.velocity.length}. Registered in {@link Properties}.
 */
@FunctionalInterface
public interface Property {

    @NotNull Value get(@NotNull Context context);
}
