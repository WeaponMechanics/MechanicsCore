package me.deecaad.core.mechanics;

import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.parse.ExpressionParser;
import me.deecaad.core.mechanics.sema.ExprLower;

import java.io.File;

/**
 * Test helper: compiles a standalone expression string through the production pipeline (the new
 * parser + lowering), so tests build {@link Expression} fixtures without a second parser. Throws
 * IllegalArgumentException on any compile diagnostic, so a malformed-expression test can assertThrows.
 */
public final class TestExpressions {

    private TestExpressions() {
    }

    public static Expression compile(String text) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        ExprNode node = ExpressionParser.parse(text, new SourceRef(new File("test"), "", -1, text), 0, 0, reporter);
        Expression expression = ExprLower.lower(node, null, null, reporter);
        if (reporter.hasErrors())
            throw new IllegalArgumentException("expression did not compile: " + reporter.all());
        return expression;
    }
}
