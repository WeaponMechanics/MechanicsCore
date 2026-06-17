package me.deecaad.core.file;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.diagnostic.SourceRef;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnakeYamlConfigTest {

    private final File file = new File("test.yml");

    private SnakeYamlConfig config(String yaml) throws Exception {
        return SnakeYamlConfig.ofText(yaml);
    }

    @Test
    void listItemPositions_pointAtEachMechanicValue() throws Exception {
        SnakeYamlConfig config = config(
            "ChainLightning:\n  Mechanics:\n    - 'Damage{Damage=5} @target'\n    - 'Bad{} @target'\n");

        List<int[]> positions = config.listItemPositions("ChainLightning.Mechanics");
        assertEquals(2, positions.size());

        // Item 0 is on line index 2, its quoted value starts after "    - '" (column 7).
        assertEquals(2, positions.get(0)[0]);
        assertEquals(3, positions.get(1)[0]);
        // The recorded column lands on the first char of the mechanic content.
        String raw = config.sourceLine(positions.get(0)[0]);
        assertEquals('D', raw.charAt(positions.get(0)[1]), raw);
    }

    @Test
    void dottedPathRead_caseSensitive() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Shoot:\n    Damage: 5\n");

        assertEquals(5, config.get("Weapon.Shoot.Damage"));
        assertTrue(config.contains("Weapon.Shoot.Damage"));
        assertFalse(config.contains("Weapon.Shoot.damage")); // case-sensitive
        assertNull(config.get("Weapon.Missing", null));
    }

    @Test
    void getKeys_deepIsDottedParentFirst() throws Exception {
        SnakeYamlConfig config = config("A:\n  B:\n    C: 1\n  D: 2\n");

        assertEquals(List.of("A", "A.B", "A.B.C", "A.D"), config.getKeys(null, true));
        assertEquals(List.of("A"), config.getKeys(null, false));
        // keys under a sub-section are relative to that section
        assertEquals(List.of("B", "D"), config.getKeys("A", false));
    }

    @Test
    void enrich_missingKeyFallsBackToParentSection() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Shoot:\n    Damage: 5\n");

        // 'Recoil' does not exist; we still want a span pointing at the parent section line.
        Diagnostic raw = Diagnostic.at(Severity.ERROR, DiagnosticKind.MISSING_REQUIRED,
            SourceRef.ofConfig(file, "Weapon.Shoot.Recoil"), "Missing required key", null);
        Diagnostic enriched = config.enrich(raw);

        assertTrue(enriched.primary().line() >= 0, "fell back to a real line");
        assertTrue(enriched.source().rawLine().contains("Shoot"), enriched.source().rawLine());
    }

    @Test
    void enrich_fillsSpanForTypeError() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Damage: oops\n");

        Diagnostic raw = Diagnostic.at(Severity.ERROR, DiagnosticKind.INVALID_TYPE,
            SourceRef.ofConfig(file, "Weapon.Damage"), "Expected a number", null);
        Diagnostic enriched = config.enrich(raw);

        assertTrue(enriched.primary().line() >= 0);
        assertTrue(enriched.source().rawLine().contains("Damage: oops"));
    }

    @Test
    void enrich_leavesAlreadyPositionedDiagnostic() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Damage: oops\n");

        Diagnostic raw = Diagnostic.at(Severity.ERROR, DiagnosticKind.INVALID_TYPE,
            SourceRef.ofConfig(file, "Weapon.Damage"), "Expected a number", null);
        Diagnostic positioned = config.enrich(raw); // enriched with a real line

        assertSame(positioned, config.enrich(positioned));
    }
}
