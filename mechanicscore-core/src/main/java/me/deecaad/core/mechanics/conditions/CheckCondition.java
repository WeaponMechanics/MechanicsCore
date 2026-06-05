package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.expression.ExpressionException;
import me.deecaad.core.mechanics.expression.ExpressionParser;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evaluates an expression and passes when the result is non-zero. The primary
 * branching primitive, e.g. {@code ?Check{If=$jumps > 0}}.
 */
public class CheckCondition extends Condition {

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
        return super.schemaBuilder().stringKey("If").required();
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        String raw = data.of("If").assertExists().get(String.class).get();
        try {
            Expression expression = ExpressionParser.parse(raw);
            return applyParentArgs(data, new CheckCondition(expression));
        } catch (ExpressionException ex) {
            throw data.exception("If", "Invalid expression: " + ex.getMessage());
        }
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
