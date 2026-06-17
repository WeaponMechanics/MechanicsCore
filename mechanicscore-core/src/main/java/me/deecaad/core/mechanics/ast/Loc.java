package me.deecaad.core.mechanics.ast;

import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
import org.jetbrains.annotations.NotNull;

/**
 * A source location: the {@link SourceRef} of the line plus the {@link Span}
 * within it. Every AST node carries one.
 */
public record Loc(@NotNull SourceRef source, @NotNull Span span) {

    public @NotNull Loc to(@NotNull Loc other) {
        return new Loc(source, span.to(other.span));
    }
}
