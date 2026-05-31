package me.deecaad.core.mechanics.sema;

import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.mechanics.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.expression.ExpressionFunctions;
import me.deecaad.core.mechanics.expression.Properties;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Lowers a parsed {@link ExprNode} into an executable {@link Expression}, while
 * validating functions, properties, and arity (recording diagnostics, never
 * throwing). Variables/contexts are not validated for existence: they may be
 * seeded by a caller or trigger the compiler cannot see, and unset variables
 * default to 0 at runtime.
 */
public final class ExprLower {

    private ExprLower() {
    }

    public static @NotNull Expression lower(@NotNull ExprNode node, @NotNull DiagnosticReporter reporter) {
        return switch (node) {
            case ExprNode.NumberLit n -> new Expression.NumberLiteral(n.value());
            case ExprNode.StringLit s -> new Expression.StringLiteral(s.value());
            case ExprNode.VarRef v -> new Expression.VarRef(v.name());
            case ExprNode.PropertyRef p -> {
                if (!Properties.exists(p.path())) {
                    reporter.error(p.loc(), "Unknown property '" + p.path() + "'",
                        suggest(p.path(), Properties.names()));
                    yield new Expression.NumberLiteral(0);
                }
                yield new Expression.PropertyRef(p.context(), p.path());
            }
            case ExprNode.Unary u -> new Expression.Unary(
                Expression.Unary.Op.valueOf(u.op().name()), lower(u.operand(), reporter));
            case ExprNode.Binary b -> {
                warnStringOperand(b, reporter);
                yield new Expression.Binary(Expression.Binary.Op.valueOf(b.op().name()),
                    lower(b.left(), reporter), lower(b.right(), reporter));
            }
            case ExprNode.Call c -> {
                ExpressionFunctions.Definition def = ExpressionFunctions.get(c.name());
                if (def == null) {
                    reporter.error(c.nameLoc(), "Unknown function '" + c.name() + "'", suggest(c.name(), ExpressionFunctions.names()));
                } else if (!def.acceptsArgCount(c.args().size())) {
                    reporter.error(c.loc(), "Function '" + c.name() + "' expects " + def.arityDescription()
                        + " arguments, got " + c.args().size());
                }
                List<Expression> args = new ArrayList<>(c.args().size());
                for (ExprNode arg : c.args())
                    args.add(lower(arg, reporter));
                yield new Expression.FunctionCall(c.name(), args);
            }
            case ExprNode.ErrorExpr e -> new Expression.NumberLiteral(0);
        };
    }

    private static void warnStringOperand(@NotNull ExprNode.Binary b, @NotNull DiagnosticReporter reporter) {
        if (b.op() == ExprNode.BinaryOp.AND || b.op() == ExprNode.BinaryOp.OR)
            return;
        if (b.left() instanceof ExprNode.StringLit s)
            reporter.warning(s.loc(), "String used in a numeric operation; it will be coerced to 0 unless numeric");
        if (b.right() instanceof ExprNode.StringLit s)
            reporter.warning(s.loc(), "String used in a numeric operation; it will be coerced to 0 unless numeric");
    }

    private static String suggest(@NotNull String actual, @NotNull Iterable<String> options) {
        String best = StringUtil.didYouMean(actual, options);
        return best == null ? null : "Did you mean '" + best + "'?";
    }
}
