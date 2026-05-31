package me.deecaad.core.mechanics.expression;

import me.deecaad.core.mechanics.expression.ExpressionLexer.Token;
import me.deecaad.core.mechanics.expression.ExpressionLexer.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A precedence-climbing recursive-descent parser. Functions and property names
 * are validated here (parse time) so config errors surface during load.
 */
public final class ExpressionParser {

    private final List<Token> tokens;
    private int pos;

    private ExpressionParser(@NotNull String source) {
        this.tokens = new ExpressionLexer(source).tokenize();
    }

    public static @NotNull Expression parse(@NotNull String source) {
        ExpressionParser parser = new ExpressionParser(source);
        Expression expression = parser.parseOr();
        parser.expect(Type.EOF, "Unexpected trailing input in expression");
        return expression;
    }

    private @NotNull Expression parseOr() {
        Expression left = parseAnd();
        while (match(Type.OR))
            left = new Expression.Binary(Expression.Binary.Op.OR, left, parseAnd());
        return left;
    }

    private @NotNull Expression parseAnd() {
        Expression left = parseComparison();
        while (match(Type.AND))
            left = new Expression.Binary(Expression.Binary.Op.AND, left, parseComparison());
        return left;
    }

    private @NotNull Expression parseComparison() {
        Expression left = parseAdditive();
        while (true) {
            Expression.Binary.Op op = switch (peek().type()) {
                case EQ -> Expression.Binary.Op.EQUAL;
                case NEQ -> Expression.Binary.Op.NOT_EQUAL;
                case LT -> Expression.Binary.Op.LESS;
                case LE -> Expression.Binary.Op.LESS_EQUAL;
                case GT -> Expression.Binary.Op.GREATER;
                case GE -> Expression.Binary.Op.GREATER_EQUAL;
                default -> null;
            };
            if (op == null)
                return left;
            advance();
            left = new Expression.Binary(op, left, parseAdditive());
        }
    }

    private @NotNull Expression parseAdditive() {
        Expression left = parseMultiplicative();
        while (true) {
            if (match(Type.PLUS))
                left = new Expression.Binary(Expression.Binary.Op.ADD, left, parseMultiplicative());
            else if (match(Type.MINUS))
                left = new Expression.Binary(Expression.Binary.Op.SUBTRACT, left, parseMultiplicative());
            else
                return left;
        }
    }

    private @NotNull Expression parseMultiplicative() {
        Expression left = parseUnary();
        while (true) {
            if (match(Type.STAR))
                left = new Expression.Binary(Expression.Binary.Op.MULTIPLY, left, parseUnary());
            else if (match(Type.SLASH))
                left = new Expression.Binary(Expression.Binary.Op.DIVIDE, left, parseUnary());
            else if (match(Type.PERCENT))
                left = new Expression.Binary(Expression.Binary.Op.MODULO, left, parseUnary());
            else
                return left;
        }
    }

    private @NotNull Expression parseUnary() {
        if (match(Type.MINUS))
            return new Expression.Unary(Expression.Unary.Op.NEGATE, parseUnary());
        if (match(Type.NOT))
            return new Expression.Unary(Expression.Unary.Op.NOT, parseUnary());
        return parsePrimary();
    }

    private @NotNull Expression parsePrimary() {
        Token token = peek();
        switch (token.type()) {
            case NUMBER -> {
                advance();
                try {
                    return new Expression.NumberLiteral(Double.parseDouble(token.text()));
                } catch (NumberFormatException e) {
                    throw new ExpressionException(token.index(), "Invalid number '" + token.text() + "'");
                }
            }
            case STRING -> {
                advance();
                return new Expression.StringLiteral(token.text());
            }
            case VARIABLE -> {
                advance();
                return new Expression.VarRef(token.text());
            }
            case IDENT -> {
                return parseIdent(token);
            }
            case LPAREN -> {
                advance();
                Expression inner = parseOr();
                expect(Type.RPAREN, "Missing closing ')'");
                return inner;
            }
            default -> throw new ExpressionException(token.index(), "Unexpected '" + token.text() + "'");
        }
    }

    private @NotNull Expression parseIdent(@NotNull Token token) {
        advance();
        String name = token.text();

        if (name.equals("true"))
            return new Expression.NumberLiteral(1);
        if (name.equals("false"))
            return new Expression.NumberLiteral(0);

        // Function call: name( args )
        if (check(Type.LPAREN))
            return parseFunctionCall(token);

        // Property read: context.path
        if (!check(Type.DOT))
            throw new ExpressionException(token.index(), "Expected '.property' or '(' after '" + name + "'");

        StringBuilder path = new StringBuilder();
        while (match(Type.DOT)) {
            Token segment = expect(Type.IDENT, "Expected a property name after '.'");
            if (path.length() > 0)
                path.append('.');
            path.append(segment.text());
        }

        String pathString = path.toString();
        if (!Properties.exists(pathString))
            throw new ExpressionException(token.index(), "Unknown property '" + pathString + "'");
        return new Expression.PropertyRef(name, pathString);
    }

    private @NotNull Expression parseFunctionCall(@NotNull Token nameToken) {
        String name = nameToken.text();
        ExpressionFunctions.Definition definition = ExpressionFunctions.get(name);
        if (definition == null)
            throw new ExpressionException(nameToken.index(), "Unknown function '" + name + "'");

        expect(Type.LPAREN, "Expected '(' after function name");
        List<Expression> args = new ArrayList<>();
        if (!check(Type.RPAREN)) {
            do {
                args.add(parseOr());
            } while (match(Type.COMMA));
        }
        expect(Type.RPAREN, "Missing closing ')' for function '" + name + "'");

        if (!definition.acceptsArgCount(args.size()))
            throw new ExpressionException(nameToken.index(), "Function '" + name + "' expects " + definition.arityDescription() + " arguments, got " + args.size());
        return new Expression.FunctionCall(name, args);
    }

    private @NotNull Token peek() {
        return tokens.get(pos);
    }

    private @NotNull Token advance() {
        return tokens.get(pos++);
    }

    private boolean check(@NotNull Type type) {
        return peek().type() == type;
    }

    private boolean match(@NotNull Type type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private @NotNull Token expect(@NotNull Type type, @NotNull String message) {
        if (check(type))
            return advance();
        throw new ExpressionException(peek().index(), message);
    }
}
