package me.deecaad.core.mechanics.expression;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Implemented by a serializer (mechanic/condition/targeter) that takes one or more expression args,
 * declared via {@code ConfigSchema.Builder.exprKey(name)}. The compiler parses and lowers each such
 * arg through the AST pipeline, then hands the results here keyed by arg name. The serializer's own
 * {@code serialize()} must not parse the expression; it leaves the field unset and reads it here.
 */
public interface ExpressionConsumer {

    void acceptExpressions(@NotNull Map<String, Expression> compiled);
}
