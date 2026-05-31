package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure-data statement AST produced by the parser. Sema resolves these into the
 * executable instance IR; whether an {@link Invoke} is a builtin mechanic or a
 * block call is decided in sema, not here.
 */
public sealed interface StmtNode {

    @NotNull Loc loc();

    /** {@code $name = <expr>} */
    record Assign(@NotNull String var, @NotNull ExprNode value, @NotNull Loc loc) implements StmtNode {
    }

    /** {@code @name = Targeter{...}} */
    record Bind(@NotNull String contextName, @NotNull InlineCallNode targeter, @NotNull Loc loc) implements StmtNode {
    }

    /** {@code Name{...} @subject ?Cond...} - Name resolves to a mechanic or block in sema. */
    record Invoke(@NotNull InlineCallNode call, @Nullable SubjectNode subject,
                  @NotNull List<InlineCallNode> conditions, @NotNull Loc loc) implements StmtNode {
    }

    /** A placeholder inserted on a syntax error so sibling statements keep parsing. */
    record Error(@NotNull Loc loc) implements StmtNode {
    }
}
