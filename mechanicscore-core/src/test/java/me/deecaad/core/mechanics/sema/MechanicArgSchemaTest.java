package me.deecaad.core.mechanics.sema;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.conditions.CheckCondition;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.MutableRegistry;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the schema validator wired into the compiler catches hallucinated mechanic arguments
 * (which serialize() silently ignored) and recurses into registry-picked nested serializers. Uses
 * synthetic mechanic/targeter prototypes so it runs headlessly (no Bukkit registries needed).
 */
class MechanicArgSchemaTest {

    private static final MutableRegistry<Targeter> TARGETERS =
        new MutableRegistry.SimpleMutableRegistry<>(new HashMap<>());

    static {
        TARGETERS.add(new FakeTargeter());
    }

    /** A targeter with a one-key schema, to exercise nested recursion. */
    static final class FakeTargeter extends Targeter {
        @Override public boolean isEntity() { return true; }
        @Override public @NotNull Context target(@NotNull CastScope scope) { throw new UnsupportedOperationException(); }
        @Override public NamespacedKey getKey() { return new NamespacedKey("test", "faketargeter"); }

        @Override protected ConfigSchema.Builder schemaBuilder() {
            return ConfigSchema.builder().doubleKey("Range").required().contextKey("From");
        }

        @Override public Targeter serialize(SerializeData data) throws SerializerException {
            data.of("Range").assertExists().getDouble();
            return this;
        }
    }

    /** A mechanic requiring Volume and accepting a registry-picked Listeners targeter. */
    static final class FakeMechanic extends Mechanic {
        @Override public void use0(CastScope scope, Target subject) { }
        @Override public NamespacedKey getKey() { return new NamespacedKey("test", "fake"); }

        @Override protected ConfigSchema.Builder schemaBuilder() {
            return super.schemaBuilder()
                .doubleKey("Volume").required()
                .registrySerializerKey("Listeners", TARGETERS);
        }

        @Override public Mechanic serialize(SerializeData data) throws SerializerException {
            data.of("Volume").assertExists().getDouble();
            data.of("Listeners").serializeRegistry(TARGETERS);
            return this;
        }
    }

    private static final class StubSymbols implements SymbolSource {
        private final Mechanic fake = new FakeMechanic();
        @Override public @Nullable Mechanic mechanic(@NotNull String name) { return name.equalsIgnoreCase("fake") ? fake : null; }
        @Override public @Nullable Targeter targeter(@NotNull String name) { return null; }
        @Override public @Nullable Condition condition(@NotNull String name) { return name.equalsIgnoreCase("check") ? new CheckCondition() : null; }
        @Override public @NotNull Set<String> mechanicNames() { return Set.of("fake"); }
        @Override public @NotNull Set<String> targeterNames() { return Set.of(); }
        @Override public @NotNull Set<String> conditionNames() { return Set.of("check"); }
        @Override public @NotNull Set<String> providedVariables() { return Set.of("jumps"); }
    }

    private static DiagnosticReporter analyze(String line) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<StmtNode> statements = new ArrayList<>();
        statements.add(StatementParser.parse(line, 0, new File("test.yml"), "Mechanics", reporter));
        ProgramNode program = new ProgramNode(Map.of("Mechanics", new BlockNode("Mechanics", statements)), "Mechanics");
        new SemanticAnalyzer(new StubSymbols()).analyze(program, new File("test.yml"), reporter);
        return reporter;
    }

    private static @Nullable Diagnostic firstOf(DiagnosticReporter reporter, DiagnosticKind kind) {
        return reporter.all().stream().filter(d -> d.kind() == kind).findFirst().orElse(null);
    }

    @Test
    void validArgs_noDiagnostics() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5} @target");
        assertTrue(reporter.isEmpty(), () -> "valid args should not warn: " + reporter.all());
    }

    @Test
    void parentArgs_notFlaggedUnknown() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5, Repeat_Amount=3, Chance=50%} @target");
        assertTrue(reporter.isEmpty(), () -> "inherited parent args must be accepted: " + reporter.all());
    }

    @Test
    void hallucinatedArg_warnsWithHintAndSpan() {
        String line = "Fake{Volume=5, Volumee=2} @target";
        DiagnosticReporter reporter = analyze(line);
        Diagnostic unknown = firstOf(reporter, DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown, () -> "Volumee should be flagged: " + reporter.all());
        assertEquals(Severity.WARNING, unknown.severity());
        assertTrue(unknown.message().toLowerCase().contains("volumee"), unknown.message());
        assertEquals("did you mean 'Volume'?", unknown.hint());
        // The caret must land on the key, not its value (regression: it pointed at '2').
        assertEquals(line.indexOf("Volumee"), unknown.primary().start(), "caret should be on the key");
        assertEquals(line.indexOf("Volumee") + "Volumee".length(), unknown.primary().end());
    }

    @Test
    void hallucinatedNestedArg_flaggedViaRegistryRecursion() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5, Listeners=faketargeter{Range=5, Rang=9}} @target");
        Diagnostic unknown = firstOf(reporter, DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown, () -> "nested 'Rang' typo should be flagged: " + reporter.all());
        assertTrue(unknown.message().toLowerCase().contains("rang"), unknown.message());
    }

    @Test
    void unknownRegistryId_isError() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5, Listeners=notreal{}} @target");
        assertTrue(reporter.hasErrors(), () -> "unknown targeter id should error: " + reporter.all());
    }

    @Test
    void duplicateKey_warnsItIsOverridden() {
        String line = "Fake{Volume=5, Volume=3} @target";
        DiagnosticReporter reporter = analyze(line);
        Diagnostic dup = reporter.all().stream()
            .filter(d -> d.message().contains("Duplicate key")).findFirst().orElse(null);
        assertNotNull(dup, () -> "a repeated key should warn: " + reporter.all());
        assertEquals(Severity.WARNING, dup.severity());
        assertTrue(dup.message().contains("Volume"), dup.message());
        // The caret must land on the overriding key, not its value (regression: it pointed at '3').
        assertEquals(line.lastIndexOf("Volume"), dup.primary().start(), "caret should be on the duplicate key");
        assertEquals(line.lastIndexOf("Volume") + "Volume".length(), dup.primary().end());
    }

    @Test
    void duplicateKey_caseInsensitive_stillWarns() {
        // 'Volume' and 'volume' collapse during normalization; the override must still be flagged.
        DiagnosticReporter reporter = analyze("Fake{Volume=5, volume=3} @target");
        assertTrue(reporter.all().stream().anyMatch(d -> d.message().contains("Duplicate key")),
            () -> "normalized duplicate should warn: " + reporter.all());
    }

    @Test
    void contextKey_validReference_noDiagnostics() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5, Listeners=faketargeter{Range=5, From=source}} @target");
        assertTrue(reporter.isEmpty(), () -> "From=source should be accepted: " + reporter.all());
    }

    @Test
    void contextKey_unknownReference_isErrorWithHint() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5, Listeners=faketargeter{Range=5, From=sourcee}} @target");
        Diagnostic bad = firstOf(reporter, DiagnosticKind.INVALID_VALUE);
        assertNotNull(bad, () -> "unknown context should be flagged: " + reporter.all());
        assertEquals(Severity.ERROR, bad.severity());
        assertTrue(bad.message().contains("sourcee"), bad.message());
        assertEquals("did you mean 'source'?", bad.hint());
    }

    @Test
    void exprKey_conditionExpression_compiledThroughPipelineWithSpan() {
        // The If= expression compiles through the AST pipeline, so an unknown function inside a
        // condition gets the same diagnostic + real span as '$x = foo(1)' would.
        DiagnosticReporter reporter = analyze("Fake{Volume=5} @target ?check{If=foo(1)}");
        Diagnostic bad = reporter.all().stream()
            .filter(d -> d.message().contains("Unknown function") && d.message().contains("foo"))
            .findFirst().orElse(null);
        assertNotNull(bad, () -> "condition expression should be checked: " + reporter.all());
        assertTrue(bad.primary().line() >= 0, "diagnostic should carry a real source span");
    }

    @Test
    void exprKey_validConditionExpression_noDiagnostics() {
        DiagnosticReporter reporter = analyze("Fake{Volume=5} @target ?check{If=$jumps > 0}");
        assertTrue(reporter.isEmpty(), () -> "valid condition expression should not warn: " + reporter.all());
    }

    @Test
    void exprKey_conditionExpression_checksPropertyRefContext() {
        // Property-ref context checking reaches inside the condition expression, since If= routes
        // through ExprLower like any '$x = <expr>'.
        DiagnosticReporter reporter = analyze("Fake{Volume=5} @target ?check{If=ghost.size > 0}");
        assertTrue(reporter.all().stream().anyMatch(d ->
                d.message().contains("Unknown context") && d.message().contains("ghost")),
            () -> reporter.all().toString());
    }
}
