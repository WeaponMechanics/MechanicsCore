package me.deecaad.core.mechanics.parse;

import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.mechanics.ast.Loc;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.expression.ExpressionException;
import me.deecaad.core.mechanics.expression.ExpressionLexer;
import me.deecaad.core.mechanics.expression.ExpressionLexer.Token;
import me.deecaad.core.mechanics.expression.ExpressionLexer.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses an expression substring into a pure-data {@link ExprNode} tree with
 * source spans. Purely syntactic: function/property/arity validation happens in
 * sema. On a syntax error it records a diagnostic and returns an
 * {@link ExprNode.ErrorExpr} (recovery at the expression boundary).
 */
public final class ExpressionParser {

    private static final class Bail extends RuntimeException {
        Bail() {
            super(null, null, false, false);
        }
    }

    private final List<Token> tokens;
    private final SourceRef source;
    private final int line;
    private final int baseColumn;
    private final String text;
    private final DiagnosticReporter reporter;
    private int pos;

    private ExpressionParser(@NotNull List<Token> tokens, @NotNull String text, @NotNull SourceRef source,
                             int line, int baseColumn, @NotNull DiagnosticReporter reporter) {
        this.tokens = tokens;
        this.text = text;
        this.source = source;
        this.line = line;
        this.baseColumn = baseColumn;
        this.reporter = reporter;
    }

    public static @NotNull ExprNode parse(@NotNull String text, @NotNull SourceRef source, int line,
                                          int baseColumn, @NotNull DiagnosticReporter reporter) {
        List<Token> tokens;
        try {
            tokens = new ExpressionLexer(text).tokenize();
        } catch (ExpressionException ex) {
            int col = baseColumn + ex.getIndex();
            reporter.error(new Loc(source, Span.of(line, col, col + 1)), "Invalid expression: " + ex.getMessage());
            return new ExprNode.ErrorExpr(fullLoc(source, line, baseColumn, text));
        }

        ExpressionParser parser = new ExpressionParser(tokens, text, source, line, baseColumn, reporter);
        try {
            ExprNode expression = parser.parseOr();
            parser.expect(Type.EOF, "Unexpected trailing input in expression");
            return expression;
        } catch (Bail bail) {
            return new ExprNode.ErrorExpr(fullLoc(source, line, baseColumn, text));
        }
    }

    private static @NotNull Loc fullLoc(@NotNull SourceRef source, int line, int baseColumn, @NotNull String text) {
        return new Loc(source, Span.of(line, baseColumn, baseColumn + text.length()));
    }

    private @NotNull ExprNode parseOr() {
        ExprNode left = parseAnd();
        while (match(Type.OR))
            left = binary(ExprNode.BinaryOp.OR, left, parseAnd());
        return left;
    }

    private @NotNull ExprNode parseAnd() {
        ExprNode left = parseComparison();
        while (match(Type.AND))
            left = binary(ExprNode.BinaryOp.AND, left, parseComparison());
        return left;
    }

    private @NotNull ExprNode parseComparison() {
        ExprNode left = parseAdditive();
        while (true) {
            ExprNode.BinaryOp op = switch (peek().type()) {
                case EQ -> ExprNode.BinaryOp.EQUAL;
                case NEQ -> ExprNode.BinaryOp.NOT_EQUAL;
                case LT -> ExprNode.BinaryOp.LESS;
                case LE -> ExprNode.BinaryOp.LESS_EQUAL;
                case GT -> ExprNode.BinaryOp.GREATER;
                case GE -> ExprNode.BinaryOp.GREATER_EQUAL;
                default -> null;
            };
            if (op == null)
                return left;
            advance();
            left = binary(op, left, parseAdditive());
        }
    }

    private @NotNull ExprNode parseAdditive() {
        ExprNode left = parseMultiplicative();
        while (true) {
            if (match(Type.PLUS))
                left = binary(ExprNode.BinaryOp.ADD, left, parseMultiplicative());
            else if (match(Type.MINUS))
                left = binary(ExprNode.BinaryOp.SUBTRACT, left, parseMultiplicative());
            else
                return left;
        }
    }

    private @NotNull ExprNode parseMultiplicative() {
        ExprNode left = parseUnary();
        while (true) {
            if (match(Type.STAR))
                left = binary(ExprNode.BinaryOp.MULTIPLY, left, parseUnary());
            else if (match(Type.SLASH))
                left = binary(ExprNode.BinaryOp.DIVIDE, left, parseUnary());
            else if (match(Type.PERCENT))
                left = binary(ExprNode.BinaryOp.MODULO, left, parseUnary());
            else
                return left;
        }
    }

    private @NotNull ExprNode parseUnary() {
        if (check(Type.MINUS)) {
            Loc opLoc = tokenLoc(advance());
            ExprNode operand = parseUnary();
            return new ExprNode.Unary(ExprNode.UnaryOp.NEGATE, operand, opLoc.to(operand.loc()));
        }
        if (check(Type.NOT)) {
            Loc opLoc = tokenLoc(advance());
            ExprNode operand = parseUnary();
            return new ExprNode.Unary(ExprNode.UnaryOp.NOT, operand, opLoc.to(operand.loc()));
        }
        return parsePrimary();
    }

    private @NotNull ExprNode parsePrimary() {
        Token token = peek();
        switch (token.type()) {
            case NUMBER -> {
                advance();
                double value;
                try {
                    value = Double.parseDouble(token.text());
                } catch (NumberFormatException e) {
                    reporter.error(tokenLoc(token), "Invalid number '" + token.text() + "'");
                    throw new Bail();
                }
                return new ExprNode.NumberLit(value, tokenLoc(token));
            }
            case STRING -> {
                advance();
                return new ExprNode.StringLit(token.text(), tokenLoc(token));
            }
            case VARIABLE -> {
                advance();
                return new ExprNode.VarRef(token.text(), tokenLoc(token));
            }
            case IDENT -> {
                return parseIdent(token);
            }
            case LPAREN -> {
                advance();
                ExprNode inner = parseOr();
                expect(Type.RPAREN, "Missing closing ')'");
                return inner;
            }
            default -> {
                reporter.error(tokenLoc(token), "Unexpected '" + token.text() + "' in expression");
                throw new Bail();
            }
        }
    }

    private @NotNull ExprNode parseIdent(@NotNull Token token) {
        advance();
        Loc nameLoc = tokenLoc(token);
        String name = token.text();

        if (name.equals("true"))
            return new ExprNode.NumberLit(1, nameLoc);
        if (name.equals("false"))
            return new ExprNode.NumberLit(0, nameLoc);

        // Function call: name( args )
        if (check(Type.LPAREN)) {
            advance();
            List<ExprNode> args = new ArrayList<>();
            if (!check(Type.RPAREN)) {
                do {
                    args.add(parseOr());
                } while (match(Type.COMMA));
            }
            Token close = expect(Type.RPAREN, "Missing closing ')' for function '" + name + "'");
            return new ExprNode.Call(name, args, nameLoc, nameLoc.to(tokenLoc(close)));
        }

        // Property read: context.path
        if (!check(Type.DOT)) {
            reporter.error(nameLoc, "Expected '.property' or '(' after '" + name + "'");
            throw new Bail();
        }
        StringBuilder path = new StringBuilder();
        Loc last = nameLoc;
        while (match(Type.DOT)) {
            Token segment = expect(Type.IDENT, "Expected a property name after '.'");
            if (path.length() > 0)
                path.append('.');
            path.append(segment.text());
            last = tokenLoc(segment);
        }
        return new ExprNode.PropertyRef(name, path.toString(), nameLoc, nameLoc.to(last));
    }

    private @NotNull ExprNode binary(@NotNull ExprNode.BinaryOp op, @NotNull ExprNode left, @NotNull ExprNode right) {
        return new ExprNode.Binary(op, left, right, left.loc().to(right.loc()));
    }

    private @NotNull Loc tokenLoc(@NotNull Token token) {
        int start = baseColumn + token.index();
        int end = start + Math.max(1, token.text().length());
        return new Loc(source, Span.of(line, start, end));
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
        reporter.error(tokenLoc(peek()), message);
        throw new Bail();
    }
}
