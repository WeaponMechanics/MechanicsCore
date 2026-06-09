package me.deecaad.core.mechanics.optimize;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.conditions.CheckCondition;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.TestExpressions;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.program.Subject;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.TargetKind;
import me.deecaad.core.mechanics.targeters.WorldTargeter;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimizerTest {

    static final class TestMechanic extends Mechanic {
        final TargetKind kind;
        final boolean batch;
        final double chance;

        TestMechanic(TargetKind kind, boolean batch, double chance) {
            this.kind = kind;
            this.batch = batch;
            this.chance = chance;
        }

        @Override public void use0(CastScope scope, Target subject) { }
        @Override public NamespacedKey getKey() { return new NamespacedKey("test", "m"); }
        @Override public Mechanic serialize(SerializeData data) { return this; }
        @Override public TargetKind requiredTarget() { return kind; }
        @Override public boolean isBatchablePlayerEffect() { return batch; }
        @Override public double getChance() { return chance; }
    }

    static final class TestCondition extends Condition {
        @Override protected boolean isAllowed0(CastScope scope, Target subject) { return true; }
        @Override public NamespacedKey getKey() { return new NamespacedKey("test", "c"); }
        @Override public Condition serialize(SerializeData data) { return this; }
    }

    private static Program program(Statement... statements) {
        return new Program(Map.of("Mechanics", new MechanicBlock("Mechanics", List.of(statements))), "Mechanics");
    }

    private static List<Statement> statements(Program program) {
        return program.getEntry().statements();
    }

    @Test
    void constantFoldingCollapsesPureExpressions() {
        Expression folded = ConstantFolder.fold(TestExpressions.compile("8 * (1 + 2)"));
        assertEquals(24.0, assertInstanceOf(Expression.NumberLiteral.class, folded).value());
    }

    @Test
    void constantFoldingSkipsRandomAndVariables() {
        assertInstanceOf(Expression.FunctionCall.class, ConstantFolder.fold(TestExpressions.compile("random(1, 2)")));
        assertInstanceOf(Expression.Binary.class, ConstantFolder.fold(TestExpressions.compile("$x + 1")));
    }

    @Test
    void deadCodeEliminationDropsZeroChanceAndFalseChecks() {
        Statement zeroChance = new Statement.BuiltinInvocation(
            new TestMechanic(TargetKind.LIVING_ENTITY, false, 0.0), new Subject.Reference("target"), List.of());
        Statement falseCheck = new Statement.BuiltinInvocation(
            new TestMechanic(TargetKind.LIVING_ENTITY, false, 1.0), new Subject.Reference("target"),
            List.of(new CheckCondition(TestExpressions.compile("0"))));
        Statement kept = new Statement.BuiltinInvocation(
            new TestMechanic(TargetKind.LIVING_ENTITY, false, 1.0), new Subject.Reference("target"), List.of());

        Program result = DeadCodeElimination.apply(program(zeroChance, falseCheck, kept));
        assertEquals(1, statements(result).size());
    }

    @Test
    void deadCodeEliminationDropsAlwaysTrueCheckFromGuards() {
        Statement stmt = new Statement.BuiltinInvocation(
            new TestMechanic(TargetKind.LIVING_ENTITY, false, 1.0), new Subject.Reference("target"),
            List.of(new CheckCondition(TestExpressions.compile("1")), new TestCondition()));

        Program result = DeadCodeElimination.apply(program(stmt));
        Statement.BuiltinInvocation bi = assertInstanceOf(Statement.BuiltinInvocation.class, statements(result).get(0));
        assertEquals(1, bi.conditions().size(), "always-true Check should be removed, unknown condition kept");
        assertInstanceOf(TestCondition.class, bi.conditions().get(0));
    }

    @Test
    void querySubstitutionNarrowsToPlayersWhenAllConsumersArePlayers() {
        Statement playerStmt = new Statement.BuiltinInvocation(
            new TestMechanic(TargetKind.PLAYER, false, 1.0), new Subject.Inline(new WorldTargeter()), List.of());
        Program result = QuerySubstitution.apply(program(playerStmt));

        Subject.Inline inline = (Subject.Inline) ((Statement.BuiltinInvocation) statements(result).get(0)).subject();
        assertTrue(String.valueOf(inline.targeter().groupKey()).contains("world:null:true:"),
            "WorldTargeter should be specialized to players: " + inline.targeter().groupKey());
    }

    @Test
    void querySubstitutionKeepsEntitiesWhenAConsumerNeedsNonPlayers() {
        Statement mobStmt = new Statement.BuiltinInvocation(
            new TestMechanic(TargetKind.LIVING_ENTITY, false, 1.0), new Subject.Inline(new WorldTargeter()), List.of());
        Program result = QuerySubstitution.apply(program(mobStmt));

        Subject.Inline inline = (Subject.Inline) ((Statement.BuiltinInvocation) statements(result).get(0)).subject();
        assertTrue(String.valueOf(inline.targeter().groupKey()).contains("world:null:false:"),
            "should not narrow to players: " + inline.targeter().groupKey());
    }

    @Test
    void groupingMergesBatchableEffectsSharingASubject() {
        Subject subject = new Subject.Reference("world");
        Statement a = new Statement.BuiltinInvocation(new TestMechanic(TargetKind.PLAYER, true, 1.0), subject, List.of());
        Statement b = new Statement.BuiltinInvocation(new TestMechanic(TargetKind.PLAYER, true, 1.0), subject, List.of());
        Statement c = new Statement.BuiltinInvocation(new TestMechanic(TargetKind.PLAYER, true, 1.0), subject, List.of());

        Program result = Grouping.apply(program(a, b, c));
        assertEquals(1, statements(result).size());
        Statement.GroupedInvoke grouped = assertInstanceOf(Statement.GroupedInvoke.class, statements(result).get(0));
        assertEquals(3, grouped.mechanics().size());
    }

    @Test
    void groupingDoesNotMergeNonBatchableEffects() {
        Subject subject = new Subject.Reference("world");
        Statement a = new Statement.BuiltinInvocation(new TestMechanic(TargetKind.LIVING_ENTITY, false, 1.0), subject, List.of());
        Statement b = new Statement.BuiltinInvocation(new TestMechanic(TargetKind.LIVING_ENTITY, false, 1.0), subject, List.of());

        Program result = Grouping.apply(program(a, b));
        assertEquals(2, statements(result).size(), "non-batchable mechanics must not be grouped");
    }
}
