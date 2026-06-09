package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.expression.ExpressionConsumer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Evaluates an expression and passes when the result is non-zero. The primary
 * branching primitive, e.g. {@code ?Check{If=$jumps > 0}}. The {@code If} expression is compiled by
 * the mechanics compiler (via {@link ExpressionConsumer}), so it shares the AST pipeline's spans,
 * function/property checks, and folding rather than a separate parser.
 */
public class CheckCondition extends Condition implements ExpressionConsumer {

    private Expression expression;

    public CheckCondition() {
    }

    public CheckCondition(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public void acceptExpressions(@NotNull Map<String, Expression> compiled) {
        this.expression = compiled.get("If");
    }

    @Override
    protected boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        return expression.eval(scope).asBoolean();
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "check");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/check";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().exprKey("If").required();
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        // The 'If' expression is compiled and injected by the compiler (acceptExpressions); here we
        // only apply the shared parent args.
        return applyParentArgs(data, new CheckCondition());
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
