package me.deecaad.core.mechanics.program;

import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SnakeYamlConfig;
import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.sema.GlobalSymbolSource;
import me.deecaad.core.mechanics.sema.SemanticAnalyzer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves call-site provided-vocabulary declaration: a {@code Mechanics:} list referencing a provided
 * context/variable type-checks only when the serializer reading it declares that vocabulary via
 * {@link MechanicSerializer#builder()}. Runs headlessly through the production
 * {@link GlobalSymbolSource} (real registries populate via {@code getKey()} under MockBukkit, no live
 * plugin needed).
 */
class MechanicSerializerVocabTest {

    // Exercises a provided context (@Victim as targeter) and a provided variable ($damage in an
    // expression -- ?check's If is an expression key, unlike Damage's literal double).
    private static final String LINE = "Damage{Damage=5.0} @Victim ?check{If=$damage > 0}";

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private static DiagnosticReporter analyze(Set<String> contexts, Set<String> variables) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<StmtNode> statements = new ArrayList<>();
        statements.add(StatementParser.parse(LINE, 0, new File("test.yml"), "Mechanics", reporter));
        ProgramNode program = new ProgramNode(Map.of("Mechanics", new BlockNode("Mechanics", statements)), "Mechanics");
        new SemanticAnalyzer(new GlobalSymbolSource(contexts, variables)).analyze(program, new File("test.yml"), reporter);
        return reporter;
    }

    /** Serializes {@link #LINE} (nested at {@code AK.Damage.Mechanics}) with the given serializer. */
    private static void serializeWith(MechanicSerializer serializer) throws Exception {
        SnakeYamlConfig config = SnakeYamlConfig.ofText(
            "AK:\n  Damage:\n    Mechanics:\n      - '" + LINE + "'\n");
        SerializeData data = new SerializeData(new File("weapon.yml"), "AK.Damage.Mechanics", config);
        serializer.serialize(data);
    }

    // ---- The mechanism: provided contexts/variables decide whether the line type-checks. ----

    @Test
    void providedVocab_lineTypeChecks() {
        DiagnosticReporter reporter = analyze(Set.of("Victim"), Set.of("damage"));
        assertFalse(reporter.hasErrors(), () -> "@Victim/$damage should resolve when provided: " + reporter.all());
    }

    @Test
    void withoutProvidedVocab_lineErrors() {
        DiagnosticReporter reporter = analyze(Set.of(), Set.of());
        assertTrue(reporter.hasErrors(), "@Victim/$damage should be unknown when not provided");
    }

    // ---- The call site: a builder-declared serializer makes its vocabulary type-check. ----

    @Test
    void declaredVocab_compiles() {
        assertDoesNotThrow(() -> serializeWith(MechanicSerializer.builder()
            .context("Victim")
            .variable("damage")
            .build()));
    }

    @Test
    void undeclaredVocab_rejects() {
        // The plain serializer declares no vocabulary, so @Victim/$damage are unknown and it fails.
        assertThrows(SerializerException.class, () -> serializeWith(new MechanicSerializer()));
    }
}
