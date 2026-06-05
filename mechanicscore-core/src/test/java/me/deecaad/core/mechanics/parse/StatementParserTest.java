package me.deecaad.core.mechanics.parse;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.mechanics.ast.InlineCallNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.ast.SubjectNode;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementParserTest {

    private static StmtNode parse(String line, DiagnosticReporter reporter) {
        return StatementParser.parse(line, 0, null, "Mechanics", reporter);
    }

    @Test
    void mechanicWithSubjectReference() {
        String line = "Damage{Amount=5} @enemies";
        DiagnosticReporter reporter = new DiagnosticReporter();
        StmtNode.Invoke invoke = assertInstanceOf(StmtNode.Invoke.class, parse(line, reporter));
        assertTrue(reporter.isEmpty());

        assertEquals("Damage", invoke.call().name());
        assertEquals(0, invoke.call().nameLoc().span().start());
        SubjectNode.Ref ref = assertInstanceOf(SubjectNode.Ref.class, invoke.subject());
        assertEquals("enemies", ref.contextName());

        // Arg span maps back to the '5' in the raw line.
        MapConfigLike.Holder amount = invoke.call().args().get("Amount");
        int col = InlineScan.argColumn(invoke.call(), amount);
        assertEquals('5', line.charAt(col));
    }

    @Test
    void bindingTargeterNameSpan() {
        String line = "@enemies = NearbyEntities{From=source, Radius=10}";
        DiagnosticReporter reporter = new DiagnosticReporter();
        StmtNode.Bind bind = assertInstanceOf(StmtNode.Bind.class, parse(line, reporter));
        assertTrue(reporter.isEmpty());

        assertEquals("enemies", bind.contextName());
        assertEquals("NearbyEntities", bind.targeter().name());
        assertEquals(line.indexOf("NearbyEntities"), bind.targeter().nameLoc().span().start());
    }

    @Test
    void assignmentExpressionBaseColumn() {
        String line = "$dmg = 8 * 2";
        DiagnosticReporter reporter = new DiagnosticReporter();
        StmtNode.Assign assign = assertInstanceOf(StmtNode.Assign.class, parse(line, reporter));
        assertTrue(reporter.isEmpty());

        assertEquals("dmg", assign.var());
        ExprNode.Binary mul = assertInstanceOf(ExprNode.Binary.class, assign.value());
        ExprNode.NumberLit eight = assertInstanceOf(ExprNode.NumberLit.class, mul.left());
        assertEquals(line.indexOf('8'), eight.loc().span().start());
    }

    @Test
    void conditionsAndInlineSubject() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        StmtNode.Invoke withCond = assertInstanceOf(StmtNode.Invoke.class,
            parse("Damage{Amount=5} @target ?Range{To=source, Max=10}", reporter));
        assertEquals(1, withCond.conditions().size());
        assertEquals("Range", withCond.conditions().get(0).name());

        StmtNode.Invoke inlineSubject = assertInstanceOf(StmtNode.Invoke.class,
            parse("Damage{} @NearbyEntities{Radius=5}", reporter));
        SubjectNode.Inline inline = assertInstanceOf(SubjectNode.Inline.class, inlineSubject.subject());
        assertEquals("NearbyEntities", inline.targeter().name());
        assertTrue(reporter.isEmpty());
    }

    @Test
    void blockCallKwargArgColumnMapsToExpression() {
        // For a block call the kwarg value is an expression; its column must map back.
        String line = "ChainLightning{jumps=5} @target";
        DiagnosticReporter reporter = new DiagnosticReporter();
        StmtNode.Invoke invoke = assertInstanceOf(StmtNode.Invoke.class, parse(line, reporter));
        MapConfigLike.Holder jumps = invoke.call().args().get("jumps");
        int col = InlineScan.argColumn(invoke.call(), jumps);
        assertEquals('5', line.charAt(col));
    }
}
