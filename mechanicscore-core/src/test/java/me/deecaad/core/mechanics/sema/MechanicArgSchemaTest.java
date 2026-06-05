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
            return ConfigSchema.builder().doubleKey("Range").required();
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
        @Override public @Nullable Condition condition(@NotNull String name) { return null; }
        @Override public @NotNull Set<String> mechanicNames() { return Set.of("fake"); }
        @Override public @NotNull Set<String> targeterNames() { return Set.of(); }
        @Override public @NotNull Set<String> conditionNames() { return Set.of(); }
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
        DiagnosticReporter reporter = analyze("Fake{Volume=5, Volumee=2} @target");
        Diagnostic unknown = firstOf(reporter, DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown, () -> "Volumee should be flagged: " + reporter.all());
        assertEquals(Severity.WARNING, unknown.severity());
        assertTrue(unknown.message().toLowerCase().contains("volumee"), unknown.message());
        assertEquals("Volume", unknown.hint());
        assertTrue(unknown.primary().line() >= 0, "diagnostic should be re-anchored to a source span");
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
}
