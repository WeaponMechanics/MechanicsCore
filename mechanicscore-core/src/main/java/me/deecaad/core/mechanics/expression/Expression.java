package me.deecaad.core.mechanics.expression;

import me.deecaad.core.mechanics.scope.CastAbortException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A parsed expression. Evaluate against a {@link CastScope} to get a {@link Value}.
 * Comparisons and boolean operators return {@code 1} or {@code 0}, so there is no
 * separate boolean type and {@code ?Check{If=...}} just tests non-zero.
 */
public sealed interface Expression {

    @NotNull Value eval(@NotNull CastScope scope);

    record NumberLiteral(double value) implements Expression {
        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            return Value.of(value);
        }
    }

    record StringLiteral(String value) implements Expression {
        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            return Value.of(value);
        }
    }

    record VarRef(String name) implements Expression {
        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            Value value = scope.getVariable(name);
            return value == null ? Value.of(0) : value;
        }
    }

    record PropertyRef(String contextName, String path) implements Expression {
        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            Context context = scope.getContext(contextName);
            if (context == null)
                throw new CastAbortException("Unknown context '" + contextName + "' in expression");
            Property property = Properties.get(path);
            if (property == null)
                throw new CastAbortException("Unknown property '" + path + "'");
            return property.get(context);
        }
    }

    record Unary(Op op, Expression operand) implements Expression {
        public enum Op { NEGATE, NOT }

        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            Value value = operand.eval(scope);
            return switch (op) {
                case NEGATE -> Value.of(-value.asNumber());
                case NOT -> Value.of(!value.asBoolean());
            };
        }
    }

    record Binary(Op op, Expression left, Expression right) implements Expression {
        public enum Op { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, AND, OR }

        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            // Short-circuit boolean operators.
            if (op == Op.AND)
                return Value.of(left.eval(scope).asBoolean() && right.eval(scope).asBoolean());
            if (op == Op.OR)
                return Value.of(left.eval(scope).asBoolean() || right.eval(scope).asBoolean());

            double a = left.eval(scope).asNumber();
            double b = right.eval(scope).asNumber();
            return switch (op) {
                case ADD -> Value.of(a + b);
                case SUBTRACT -> Value.of(a - b);
                case MULTIPLY -> Value.of(a * b);
                case DIVIDE -> Value.of(a / b);
                case MODULO -> Value.of(a % b);
                case EQUAL -> Value.of(a == b);
                case NOT_EQUAL -> Value.of(a != b);
                case LESS -> Value.of(a < b);
                case LESS_EQUAL -> Value.of(a <= b);
                case GREATER -> Value.of(a > b);
                case GREATER_EQUAL -> Value.of(a >= b);
                case AND, OR -> throw new AssertionError();
            };
        }
    }

    record FunctionCall(String name, List<Expression> args) implements Expression {
        @Override
        public @NotNull Value eval(@NotNull CastScope scope) {
            ExpressionFunctions.Definition definition = ExpressionFunctions.get(name);
            if (definition == null)
                throw new CastAbortException("Unknown function '" + name + "'");
            double[] values = new double[args.size()];
            for (int i = 0; i < values.length; i++)
                values[i] = args.get(i).eval(scope).asNumber();
            return Value.of(definition.apply().applyAsDouble(values));
        }
    }
}
