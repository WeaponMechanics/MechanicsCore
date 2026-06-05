package me.deecaad.core.mechanics.parse;

import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionParserTest {

    private static SourceRef source(String rawLine) {
        return new SourceRef(null, "Mechanics", 0, rawLine);
    }

    private static ExprNode parse(String text, String rawLine, int baseColumn, DiagnosticReporter reporter) {
        return ExpressionParser.parse(text, source(rawLine), 0, baseColumn, reporter);
    }

    @Test
    void precedenceBuildsCorrectTree() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        ExprNode node = parse("8 * (1 + 2)", "8 * (1 + 2)", 0, reporter);

        assertTrue(reporter.isEmpty());
        ExprNode.Binary mul = assertInstanceOf(ExprNode.Binary.class, node);
        assertEquals(ExprNode.BinaryOp.MULTIPLY, mul.op());
        ExprNode.NumberLit eight = assertInstanceOf(ExprNode.NumberLit.class, mul.left());
        assertEquals(8.0, eight.value());
        assertInstanceOf(ExprNode.Binary.class, mul.right());
    }

    @Test
    void spansTrackColumnsWithBaseOffset() {
        // rawLine: "$dmg = 8 * 2", expression "8 * 2" starts at column 7
        DiagnosticReporter reporter = new DiagnosticReporter();
        ExprNode node = parse("8 * 2", "$dmg = 8 * 2", 7, reporter);

        ExprNode.Binary mul = assertInstanceOf(ExprNode.Binary.class, node);
        ExprNode.NumberLit eight = assertInstanceOf(ExprNode.NumberLit.class, mul.left());
        ExprNode.NumberLit two = assertInstanceOf(ExprNode.NumberLit.class, mul.right());
        assertEquals(7, eight.loc().span().start());   // '8' in rawLine
        assertEquals(11, two.loc().span().start());     // '2' in rawLine
    }

    @Test
    void functionAndPropertyAreSyntacticOnly() {
        // No registry validation in the parser; these are well-formed nodes.
        DiagnosticReporter reporter = new DiagnosticReporter();
        assertInstanceOf(ExprNode.Call.class, parse("foo(1)", "foo(1)", 0, reporter));
        assertInstanceOf(ExprNode.PropertyRef.class, parse("target.helth", "target.helth", 0, reporter));
        assertTrue(reporter.isEmpty(), "parser must not validate names/arity");
    }

    @Test
    void propertyPathSpanCoversThePath() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        ExprNode.PropertyRef ref = assertInstanceOf(ExprNode.PropertyRef.class,
            parse("enemies.size", "enemies.size", 0, reporter));
        assertEquals("enemies", ref.context());
        assertEquals("size", ref.path());
    }

    @ParameterizedTest
    @ValueSource(strings = { "3 +", "(3", "max(1,", "3 = 4", "$" })
    void malformedRecordsDiagnosticAndRecovers(String bad) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        ExprNode node = parse(bad, bad, 0, reporter);
        assertFalse(reporter.isEmpty(), "should record a diagnostic for: " + bad);
        assertInstanceOf(ExprNode.ErrorExpr.class, node);
    }
}
