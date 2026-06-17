package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Pure-data expression AST produced by the parser. Unlike the executable
 * {@code expression.Expression}, these nodes carry source {@link Loc}s and have
 * no {@code eval} behavior; the optimizer folds them and sema lowers them.
 */
public sealed interface ExprNode {

    @NotNull Loc loc();

    enum UnaryOp { NEGATE, NOT }

    enum BinaryOp {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO,
        EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
        AND, OR
    }

    record NumberLit(double value, @NotNull Loc loc) implements ExprNode {
    }

    record StringLit(@NotNull String value, @NotNull Loc loc) implements ExprNode {
    }

    record VarRef(@NotNull String name, @NotNull Loc loc) implements ExprNode {
    }

    record PropertyRef(@NotNull String context, @NotNull String path, @NotNull Loc contextLoc, @NotNull Loc loc) implements ExprNode {
    }

    record Unary(@NotNull UnaryOp op, @NotNull ExprNode operand, @NotNull Loc loc) implements ExprNode {
    }

    record Binary(@NotNull BinaryOp op, @NotNull ExprNode left, @NotNull ExprNode right, @NotNull Loc loc) implements ExprNode {
    }

    record Call(@NotNull String name, @NotNull List<ExprNode> args, @NotNull Loc nameLoc, @NotNull Loc loc) implements ExprNode {
    }

    /**
     * A placeholder inserted on a syntax error so sibling nodes keep parsing.
     */
    record ErrorExpr(@NotNull Loc loc) implements ExprNode {
    }
}
