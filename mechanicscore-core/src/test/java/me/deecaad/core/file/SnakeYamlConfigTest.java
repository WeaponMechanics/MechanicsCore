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
    void dottedPathRead_caseSensitive() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Shoot:\n    Damage: 5\n");

        assertEquals(5, config.get("Weapon.Shoot.Damage"));
        assertTrue(config.contains("Weapon.Shoot.Damage"));
        assertFalse(config.contains("Weapon.Shoot.damage")); // case-sensitive, like the old Bukkit backend
        assertNull(config.get("Weapon.Missing", null));
    }

    @Test
    void getKeys_deepIsDottedParentFirst() throws Exception {
        SnakeYamlConfig config = config("A:\n  B:\n    C: 1\n  D: 2\n");

        assertEquals(List.of("A", "A.B", "A.B.C", "A.D"), config.getKeys(null, true));
        assertEquals(List.of("A"), config.getKeys(null, false));
        // keys under a sub-section are relative to that section (matches the old Bukkit backend)
        assertEquals(List.of("B", "D"), config.getKeys("A", false));
    }

    @Test
    void getLocation_rendersSourceLineAndCaret() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Damage: oops\n");

        String location = config.getLocation(file, "Weapon.Damage");
        String[] lines = location.split("\n");

        // header + source line + caret line
        assertEquals(3, lines.length, location);
        assertTrue(lines[1].contains("Damage: oops"), location);
        assertTrue(lines[2].trim().chars().allMatch(c -> c == '^'), location);
        // the caret should sit under the value 'oops', not at column 0
        assertTrue(lines[2].indexOf('^') > lines[1].indexOf("oops") - 2, location);
    }

    @Test
    void getLocation_missingKeyFallsBackToParentSection() throws Exception {
        SnakeYamlConfig config = config("Weapon:\n  Shoot:\n    Damage: 5\n");

        // 'Recoil' does not exist; we still want a caret pointing at the parent section line.
        String location = config.getLocation(file, "Weapon.Shoot.Recoil");
        assertTrue(location.contains("Shoot"), location);
        assertTrue(location.contains("^"), location);
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
        Diagnostic positioned = config.enrich(raw); // now has a real line

        assertSame(positioned, config.enrich(positioned));
    }
}
