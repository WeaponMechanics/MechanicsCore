package me.deecaad.core.mechanics.expression;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an expression string into a list of {@link Token}s for the
 * {@link me.deecaad.core.mechanics.parse.ExpressionParser}.
 */
public final class ExpressionLexer {

    public enum Type {
        NUMBER, STRING, IDENT, VARIABLE,
        PLUS, MINUS, STAR, SLASH, PERCENT,
        EQ, NEQ, LT, LE, GT, GE, AND, OR, NOT,
        LPAREN, RPAREN, COMMA, DOT, EOF
    }

    public record Token(@NotNull Type type, @NotNull String text, int index) {
    }

    private final String input;
    private int pos;

    public ExpressionLexer(@NotNull String input) {
        this.input = input;
    }

    public @NotNull List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = next();
            tokens.add(token);
        } while (token.type() != Type.EOF);
        return tokens;
    }

    private @NotNull Token next() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
            pos++;

        if (pos >= input.length())
            return new Token(Type.EOF, "", pos);

        int start = pos;
        char c = input.charAt(pos);

        // Numbers
        if (Character.isDigit(c) || (c == '.' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
                pos++;
            return new Token(Type.NUMBER, input.substring(start, pos), start);
        }

        // Variables: $name
        if (c == '$') {
            pos++;
            int nameStart = pos;
            while (pos < input.length() && isIdentChar(input.charAt(pos)))
                pos++;
            if (pos == nameStart)
                throw new ExpressionException(start, "Expected a variable name after '$'");
            return new Token(Type.VARIABLE, input.substring(nameStart, pos), start);
        }

        // Identifiers (context names, properties, function names, true/false)
        if (Character.isLetter(c) || c == '_') {
            while (pos < input.length() && isIdentChar(input.charAt(pos)))
                pos++;
            return new Token(Type.IDENT, input.substring(start, pos), start);
        }

        // Strings: '...' or "..."
        if (c == '\'' || c == '"') {
            pos++;
            StringBuilder builder = new StringBuilder();
            while (pos < input.length() && input.charAt(pos) != c) {
                builder.append(input.charAt(pos));
                pos++;
            }
            if (pos >= input.length())
                throw new ExpressionException(start, "Unterminated string literal");
            pos++; // closing quote
            return new Token(Type.STRING, builder.toString(), start);
        }

        // Operators
        return switch (c) {
            case '+' -> single(Type.PLUS, start);
            case '-' -> single(Type.MINUS, start);
            case '*' -> single(Type.STAR, start);
            case '/' -> single(Type.SLASH, start);
            case '%' -> single(Type.PERCENT, start);
            case '(' -> single(Type.LPAREN, start);
            case ')' -> single(Type.RPAREN, start);
            case ',' -> single(Type.COMMA, start);
            case '.' -> single(Type.DOT, start);
            case '=' -> twoChar('=', Type.EQ, start, "Expected '==' for equality");
            case '!' -> input.startsWith("!=", pos) ? two(Type.NEQ, start) : single(Type.NOT, start);
            case '<' -> input.startsWith("<=", pos) ? two(Type.LE, start) : single(Type.LT, start);
            case '>' -> input.startsWith(">=", pos) ? two(Type.GE, start) : single(Type.GT, start);
            case '&' -> twoChar('&', Type.AND, start, "Expected '&&'");
            case '|' -> twoChar('|', Type.OR, start, "Expected '||'");
            default -> throw new ExpressionException(start, "Unexpected character '" + c + "'");
        };
    }

    private @NotNull Token single(@NotNull Type type, int start) {
        pos++;
        return new Token(type, input.substring(start, pos), start);
    }

    private @NotNull Token two(@NotNull Type type, int start) {
        pos += 2;
        return new Token(type, input.substring(start, pos), start);
    }

    private @NotNull Token twoChar(char second, @NotNull Type type, int start, @NotNull String error) {
        if (pos + 1 < input.length() && input.charAt(pos + 1) == second)
            return two(type, start);
        throw new ExpressionException(start, error);
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
