package me.deecaad.core.mechanics;

import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.sema.GlobalSymbolSource;
import me.deecaad.core.mechanics.sema.SemanticAnalyzer;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof that real, registered mechanics validate headlessly (no Minecraft server) through
 * the compiler, using the production {@link GlobalSymbolSource}. getKey() needs no live plugin, so the
 * registries populate and schemas drive validation. This is the closest thing to the external-harness
 * config-verification workflow runnable in-process today.
 */
class RealMechanicHeadlessValidationTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private static DiagnosticReporter analyze(String line) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<StmtNode> statements = new ArrayList<>();
        statements.add(StatementParser.parse(line, 0, new File("test.yml"), "Mechanics", reporter));
        ProgramNode program = new ProgramNode(Map.of("Mechanics", new BlockNode("Mechanics", statements)), "Mechanics");
        new SemanticAnalyzer(new GlobalSymbolSource()).analyze(program, new File("test.yml"), reporter);
        return reporter;
    }

    @Test
    void validDamageMechanic_noErrors() {
        DiagnosticReporter reporter = analyze("Damage{Damage=5.0} @target");
        assertFalse(reporter.hasErrors(), () -> reporter.all().toString());
    }

    @Test
    void hallucinatedArgOnRealMechanic_flagged() {
        DiagnosticReporter reporter = analyze("Damage{Damage=5.0, Damagee=9} @target");
        boolean flagged = reporter.all().stream()
            .anyMatch(d -> d.kind() == DiagnosticKind.UNKNOWN_KEY && d.message().toLowerCase().contains("damagee"));
        assertTrue(flagged, () -> "real mechanic should flag hallucinated 'Damagee': " + reporter.all());
    }
}
