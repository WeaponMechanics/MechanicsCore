package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;

/**
 * The subject of a mechanic line: either a reference to a named context
 * ({@code @enemies}) or an inline targeter ({@code @Nearby{...}}).
 */
public sealed interface SubjectNode {

    @NotNull Loc loc();

    record Ref(@NotNull String contextName, @NotNull Loc loc) implements SubjectNode {
    }

    record Inline(@NotNull InlineCallNode targeter, @NotNull Loc loc) implements SubjectNode {
    }
}
