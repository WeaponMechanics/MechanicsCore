package me.deecaad.core.mechanics.program;

import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.jetbrains.annotations.NotNull;

/**
 * The subject of a mechanic line: the context it acts on. Either a reference to
 * a named context ({@code @enemies}) or an inline targeter ({@code @Nearby{...}}).
 */
public sealed interface Subject permits Subject.Reference, Subject.Inline {

    @NotNull Context resolve(@NotNull CastScope scope);

    /**
     * A reference to a named context, e.g. {@code @enemies} or the default
     * {@code @target}.
     */
    record Reference(@NotNull String name) implements Subject {
        @Override
        public @NotNull Context resolve(@NotNull CastScope scope) {
            Context context = scope.getContext(name);
            return context == null ? Context.empty() : context;
        }
    }

    /**
     * An inline targeter, e.g. {@code @NearbyEntities{Radius=5}}.
     */
    record Inline(@NotNull Targeter targeter) implements Subject {
        @Override
        public @NotNull Context resolve(@NotNull CastScope scope) {
            return targeter.target(scope);
        }
    }
}
