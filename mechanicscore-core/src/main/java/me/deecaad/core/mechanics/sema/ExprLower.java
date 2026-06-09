package me.deecaad.core.mechanics.sema;

import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.expression.ExpressionFunctions;
import me.deecaad.core.mechanics.expression.Properties;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lowers a parsed {@link ExprNode} into an executable {@link Expression}, while
 * validating functions, properties, arity, and the context half of property refs
 * (recording diagnostics, never throwing). The {@code contexts} set is the contexts
 * in scope; {@code null} skips context checks (standalone/test compilation). Variables
 * are not validated: they may be seeded by a caller and default to 0 at runtime.
 */
public final class ExprLower {

    private ExprLower() {
    }

    public static @NotNull Expression lower(@NotNull ExprNode node, @Nullable Set<String> contexts,
                                            @NotNull DiagnosticReporter reporter) {
        return switch (node) {
            case ExprNode.NumberLit n -> new Expression.NumberLiteral(n.value());
            case ExprNode.StringLit s -> new Expression.StringLiteral(s.value());
            case ExprNode.VarRef v -> new Expression.VarRef(v.name());
            case ExprNode.PropertyRef p -> {
                if (contexts != null && !contexts.contains(p.context()))
                    reporter.error(p.contextLoc(), "Unknown context '@" + p.context() + "'", suggest(p.context(), contexts));
                if (!Properties.exists(p.path())) {
                    reporter.error(p.loc(), "Unknown property '" + p.path() + "'",
                        suggest(p.path(), Properties.names()));
                    yield new Expression.NumberLiteral(0);
                }
                yield new Expression.PropertyRef(p.context(), p.path());
            }
            case ExprNode.Unary u -> new Expression.Unary(
                Expression.Unary.Op.valueOf(u.op().name()), lower(u.operand(), contexts, reporter));
            case ExprNode.Binary b -> {
                warnStringOperand(b, reporter);
                yield new Expression.Binary(Expression.Binary.Op.valueOf(b.op().name()),
                    lower(b.left(), contexts, reporter), lower(b.right(), contexts, reporter));
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
                    args.add(lower(arg, contexts, reporter));
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
        String best = StringUtil.didYouMean(actual, options, actual.length() + 2);
        return best == null ? null : "Did you mean '" + best + "'?";
    }
}
