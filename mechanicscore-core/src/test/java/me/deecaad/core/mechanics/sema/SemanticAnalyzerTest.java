package me.deecaad.core.mechanics.sema;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticAnalyzerTest {

    // A mechanic that requires an "Amount" arg, to exercise the catch-convert path.
    static final class FakeMechanic extends Mechanic {
        @Override
        public void use0(CastScope scope, Target subject) {
        }

        @Override
        public NamespacedKey getKey() {
            return new NamespacedKey("test", "fake");
        }

        @Override
        protected ConfigSchema.Builder schemaBuilder() {
            return super.schemaBuilder().intKey("Amount");
        }

        @Override
        public Mechanic serialize(SerializeData data) throws SerializerException {
            data.of("Amount").assertExists().getInt();
            return this;
        }
    }

    /** A map-based SymbolSource that avoids loading the Bukkit Registry type. */
    private static final class StubSymbols implements SymbolSource {
        private final Mechanic fake = new FakeMechanic();

        @Override
        public @Nullable Mechanic mechanic(@NotNull String name) {
            return name.equalsIgnoreCase("fake") ? fake : null;
        }

        @Override
        public @Nullable Targeter targeter(@NotNull String name) {
            return null;
        }

        @Override
        public @Nullable Condition condition(@NotNull String name) {
            return null;
        }

        @Override
        public @NotNull Set<String> mechanicNames() {
            return Set.of("fake");
        }

        @Override
        public @NotNull Set<String> targeterNames() {
            return Set.of();
        }

        @Override
        public @NotNull Set<String> conditionNames() {
            return Set.of();
        }

        @Override
        public @NotNull Set<String> providedContexts() {
            return Set.of("enemies");
        }
    }

    private static SemanticAnalyzer analyzer() {
        return new SemanticAnalyzer(new StubSymbols());
    }

    private static ProgramNode program(DiagnosticReporter reporter, String... lines) {
        List<StmtNode> statements = new ArrayList<>();
        for (int i = 0; i < lines.length; i++)
            statements.add(StatementParser.parse(lines[i], i, new File("test.yml"), "Mechanics", reporter));
        return new ProgramNode(Map.of("Mechanics", new BlockNode("Mechanics", statements)), "Mechanics");
    }

    private static long errors(DiagnosticReporter reporter) {
        return reporter.all().stream().filter(d -> d.severity() == Severity.ERROR).count();
    }

    @Test
    void collectsMultipleErrorsNotFailFast() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter,
            "$x = foo(1)",         // unknown function
            "$y = target.helth",   // unknown property
            "Bogus{} @target"      // unknown mechanic/block
        ), new File("test.yml"), reporter);

        assertEquals(3, errors(reporter), "each bad line should yield its own error");
    }

    @Test
    void unknownFunctionAndArity() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter, "$a = foo(1)", "$b = clamp(1, 2)"), new File("test.yml"), reporter);

        assertEquals(2, errors(reporter));
        assertTrue(reporter.all().stream().anyMatch(d -> d.message().contains("Unknown function 'foo'")));
        assertTrue(reporter.all().stream().anyMatch(d -> d.message().contains("clamp") && d.message().contains("expects 3")));
    }

    @Test
    void unknownPropertyIsErrorUnknownContextIsAllowed() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter, "$a = target.helth", "$b = ghost.size"), new File("test.yml"), reporter);

        // target.helth: unknown property -> ERROR. ghost.size: 'size' is valid and an unknown
        // context is no longer flagged (it may be seeded by a caller/trigger).
        assertEquals(1, errors(reporter));
        assertTrue(reporter.all().stream().anyMatch(d -> d.severity() == Severity.ERROR && d.message().contains("helth")));
        assertFalse(reporter.all().stream().anyMatch(d -> d.message().contains("ghost")));
    }

    @Test
    void serializeErrorOnOneLineDoesNotAbortSiblings() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter,
            "Fake{} @target",       // serialize throws (missing Amount) -> converted error
            "$y = target.helth"     // separate property error still collected
        ), new File("test.yml"), reporter);

        assertEquals(2, errors(reporter));
    }

    @Test
    void externallyProvidedVariablesAndContextsAreNotFlagged() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter,
            "$dmg = $external * 2",       // $external set by a caller/trigger -> no warning
            "Fake{Amount=5} @enemies"     // @enemies declared via providedContexts() -> no warning
        ), new File("test.yml"), reporter);

        assertTrue(reporter.isEmpty(), () -> "reads of caller-provided state must not warn: " + reporter.all());
    }

    @Test
    void unknownSubjectContextIsError() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter, "Fake{Amount=5} @nope"), new File("test.yml"), reporter);

        assertEquals(1, errors(reporter));
        assertTrue(reporter.all().stream().anyMatch(d ->
            d.message().contains("Unknown context") && d.message().contains("nope")));
    }

    @Test
    void builtinSourceAndTargetContextsAreInScope() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter,
            "Fake{Amount=5} @source",
            "Fake{Amount=5} @target"
        ), new File("test.yml"), reporter);

        assertEquals(0, errors(reporter), () -> reporter.all().toString());
    }

    @Test
    void boundContextIsInScopeForLaterLines() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter,
            "@ring = Nearby{}",         // unknown targeter -> 1 error, but still registers @ring
            "Fake{Amount=5} @ring"      // @ring now in scope -> no 'unknown context' error
        ), new File("test.yml"), reporter);

        assertEquals(1, errors(reporter), () -> reporter.all().toString());
        assertTrue(reporter.all().stream().anyMatch(d -> d.message().contains("targeter")));
        assertFalse(reporter.all().stream().anyMatch(d -> d.message().contains("Unknown context")));
    }

    @Test
    void validProgramProducesNoErrors() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        analyzer().analyze(program(reporter,
            "$dmg = 8 * (1 + 2)",
            "Fake{Amount=5} @target"
        ), new File("test.yml"), reporter);

        assertEquals(0, errors(reporter), () -> reporter.all().toString());
    }
}
