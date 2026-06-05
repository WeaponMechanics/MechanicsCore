package me.deecaad.core.mechanics;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.conditions.CheckCondition;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.optimize.Optimizer;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.program.Subject;
import me.deecaad.core.mechanics.scope.CastBudget;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.TargetKind;
import me.deecaad.core.mechanics.scope.Value;
import me.deecaad.core.mechanics.sema.SemanticAnalyzer;
import me.deecaad.core.mechanics.sema.SymbolSource;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Full lifecycle: config lines to AST (parse) to instance IR (sema) to optimized
 * IR (optimize) to execution against a mocked scope. This is the single place to
 * set a breakpoint in {@link #compile} and step through the whole pipeline.
 */
class MechanicsLifecycleTest {

    /** A mechanic that records how many times it ran and carries capability metadata. */
    static final class RecordingMechanic extends Mechanic {
        final String id;
        final TargetKind kind;
        final boolean batch;
        int count = 0;

        RecordingMechanic(String id, TargetKind kind, boolean batch) {
            this.id = id;
            this.kind = kind;
            this.batch = batch;
        }

        @Override public void use0(CastScope scope, Target subject) { count++; }
        @Override public NamespacedKey getKey() { return new NamespacedKey("test", id); }
        @Override public Mechanic serialize(SerializeData data) { return this; }
        @Override public TargetKind requiredTarget() { return kind; }
        @Override public boolean isBatchablePlayerEffect() { return batch; }
        @Override public double getChance() { return 1.0; }
    }

    /** A targeter that resolves to the cast source, so the program is runnable without a world. */
    static final class SelfTargeter extends Targeter {
        @Override public boolean isEntity() { return true; }
        @Override public Context target(@NotNull CastScope scope) { return scope.source(); }
        @Override public NamespacedKey getKey() { return new NamespacedKey("test", "self"); }
        @Override public Targeter serialize(SerializeData data) { return this; }
        @Override public Object groupKey() { return "self"; }
    }

    /** A test SymbolSource: a couple of batchable player effects, a non-batchable one, and ?check. */
    private static final class Symbols implements SymbolSource {
        final RecordingMechanic particle = new RecordingMechanic("particle", TargetKind.PLAYER, true);
        final RecordingMechanic sound = new RecordingMechanic("sound", TargetKind.PLAYER, true);
        final RecordingMechanic damage = new RecordingMechanic("damage", TargetKind.LIVING_ENTITY, false);
        final SelfTargeter self = new SelfTargeter();
        final CheckCondition check = new CheckCondition();

        @Override public @Nullable Mechanic mechanic(@NotNull String name) {
            return switch (name.toLowerCase()) {
                case "particle" -> particle;
                case "sound" -> sound;
                case "damage" -> damage;
                default -> null;
            };
        }

        @Override public @Nullable Targeter targeter(@NotNull String name) {
            return name.equalsIgnoreCase("self") ? self : null;
        }

        @Override public @Nullable Condition condition(@NotNull String name) {
            return name.equalsIgnoreCase("check") ? check : null;
        }

        @Override public @NotNull Set<String> mechanicNames() { return Set.of("particle", "sound", "damage"); }
        @Override public @NotNull Set<String> targeterNames() { return Set.of("self"); }
        @Override public @NotNull Set<String> conditionNames() { return Set.of("check"); }
    }

    /** Walks the full compiler: parse to sema to optimize. Break here to step through everything. */
    private static Program compile(SymbolSource symbols, String... lines) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        File file = new File("test.yml");

        List<StmtNode> nodes = new ArrayList<>();
        for (int i = 0; i < lines.length; i++)
            nodes.add(StatementParser.parse(lines[i], i, file, "Mechanics", reporter));

        ProgramNode programNode = new ProgramNode(Map.of("Mechanics", new BlockNode("Mechanics", nodes)), "Mechanics");
        Program program = new SemanticAnalyzer(symbols).analyze(programNode, file, reporter);
        assertFalse(reporter.hasErrors(), () -> "unexpected diagnostics: " + reporter.all());

        return Optimizer.optimize(program);
    }

    private static CastScope scope() {
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        Mockito.when(entity.getName()).thenReturn("Bob");
        Location location = new Location(null, 0, 0, 0);
        Mockito.when(entity.getLocation()).thenReturn(location);
        Mockito.when(entity.getEyeLocation()).thenReturn(location);
        return CastScope.builder(entity).budget(CastBudget.defaults()).build();
    }

    private static List<Statement> statements(Program program) {
        return program.getEntry().statements();
    }

    @Test
    void compilesFoldsGroupsAndRunsEndToEnd() {
        Symbols symbols = new Symbols();
        Program program = compile(symbols,
            "$dmg = 8 * (1 + 2)",                 // constant folding -> 24
            "@hit = self{}",                       // binding
            "particle{} @hit",                     // batchable player effect ->
            "sound{} @hit",                        //   grouped with the line above
            "damage{} @hit ?check{If=$dmg > 0}"    // conditioned + non-batchable -> stays separate
        );

        // --- Optimized IR shape ---
        List<Statement> stmts = statements(program);
        assertEquals(4, stmts.size(), () -> "expected fold + bind + grouped + damage, got " + stmts);

        Statement.Assignment assign = assertInstanceOf(Statement.Assignment.class, stmts.get(0));
        assertEquals(24.0, assertInstanceOf(Expression.NumberLiteral.class, assign.expression()).value(),
            "8 * (1 + 2) should fold to a literal 24");

        assertInstanceOf(Statement.Binding.class, stmts.get(1));

        Statement.GroupedInvoke grouped = assertInstanceOf(Statement.GroupedInvoke.class, stmts.get(2));
        assertEquals(2, grouped.mechanics().size(), "particle + sound should batch into one grouped query");
        assertInstanceOf(Subject.Reference.class, grouped.subject());

        Statement.BuiltinInvocation damage = assertInstanceOf(Statement.BuiltinInvocation.class, stmts.get(3));
        assertEquals(1, damage.conditions().size(), "?check should stay on the damage line");

        // --- Execution against a mocked scope ---
        CastScope scope = scope();
        program.run(scope);

        assertEquals(1, symbols.particle.count, "particle should run once (one target)");
        assertEquals(1, symbols.sound.count, "sound should run once via the grouped query");
        assertEquals(1, symbols.damage.count, "damage should pass ?check{If=$dmg > 0} and run once");
        assertEquals(24.0, assertInstanceOf(Value.NumberValue.class, scope.getVariable("dmg")).value(),
            "$dmg should resolve to the folded 24 at runtime");
    }

    @Test
    void deadCheckDropsTheStatementBeforeItCanRun() {
        Symbols symbols = new Symbols();
        Program program = compile(symbols,
            "damage{} @hit ?check{If=0}",   // statically false -> eliminated
            "@hit = self{}",
            "particle{} @hit"
        );

        // The damage line is gone; only the binding and the (lone) particle remain.
        long damageStatements = statements(program).stream()
            .filter(s -> s instanceof Statement.BuiltinInvocation bi && bi.mechanic() == symbols.damage)
            .count();
        assertEquals(0, damageStatements, "statically-false ?check should remove the damage statement");

        program.run(scope());
        assertEquals(0, symbols.damage.count, "eliminated statement must never execute");
        assertEquals(1, symbols.particle.count);
    }
}
