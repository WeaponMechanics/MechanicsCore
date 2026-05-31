package me.deecaad.core.mechanics.optimize;

import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.scope.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Folds pure-constant expression subtrees to literals (e.g. {@code 8 * (1 + 2)}
 * to {@code 24}). Nondeterministic functions ({@code random}) are never folded.
 * The only optimizer pass that rewrites expressions.
 */
public final class ConstantFolder {

    private ConstantFolder() {
    }

    public static @NotNull Program apply(@NotNull Program program) {
        Map<String, MechanicBlock> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, MechanicBlock> entry : program.blocks().entrySet()) {
            MechanicBlock block = entry.getValue();
            List<Statement> statements = new ArrayList<>(block.statements().size());
            for (Statement statement : block.statements())
                statements.add(foldStatement(statement));
            blocks.put(entry.getKey(), new MechanicBlock(block.name(), statements));
        }
        return new Program(blocks, program.entry());
    }

    private static @NotNull Statement foldStatement(@NotNull Statement statement) {
        if (statement instanceof Statement.Assignment assign)
            return new Statement.Assignment(assign.name(), fold(assign.expression()));
        return statement;
    }

    /**
     * Folds an expression, replacing pure-constant subtrees with literals.
     */
    public static @NotNull Expression fold(@NotNull Expression expression) {
        if (isConstant(expression))
            return toLiteral(constEval(expression));
        return switch (expression) {
            case Expression.Unary u -> new Expression.Unary(u.op(), fold(u.operand()));
            case Expression.Binary b -> new Expression.Binary(b.op(), fold(b.left()), fold(b.right()));
            case Expression.FunctionCall c -> {
                List<Expression> args = new ArrayList<>(c.args().size());
                for (Expression arg : c.args())
                    args.add(fold(arg));
                yield new Expression.FunctionCall(c.name(), args);
            }
            default -> expression;
        };
    }

    /**
     * True if the expression has no variable/property/context reads and no
     * nondeterministic function, so it evaluates to the same value every cast.
     */
    public static boolean isConstant(@NotNull Expression expression) {
        return switch (expression) {
            case Expression.NumberLiteral ignored -> true;
            case Expression.StringLiteral ignored -> true;
            case Expression.VarRef ignored -> false;
            case Expression.PropertyRef ignored -> false;
            case Expression.Unary u -> isConstant(u.operand());
            case Expression.Binary b -> isConstant(b.left()) && isConstant(b.right());
            case Expression.FunctionCall c -> {
                if (c.name().equals("random"))
                    yield false;
                for (Expression arg : c.args())
                    if (!isConstant(arg))
                        yield false;
                yield true;
            }
        };
    }

    /**
     * Evaluates a constant subtree. The scope is never read for a constant tree,
     * so passing null is safe and reuses the runtime eval semantics exactly.
     */
    public static @NotNull Value constEval(@NotNull Expression expression) {
        return expression.eval(null);
    }

    private static @NotNull Expression toLiteral(@NotNull Value value) {
        if (value instanceof Value.NumberValue number)
            return new Expression.NumberLiteral(number.value());
        return new Expression.StringLiteral(value.asString());
    }
}
