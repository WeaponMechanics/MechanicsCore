package me.deecaad.core.mechanics.program;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticRenderer;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.file.SnakeYamlConfig;
import me.deecaad.core.mechanics.sema.GlobalSymbolSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves an inline-mechanic diagnostic is re-anchored onto the real YAML line: the rendered snippet
 * shows the actual source line (with its '- ' and quotes) and a real line number, and the caret lands
 * under the offending token rather than relative to the bare mechanic string.
 */
class MechanicLineRenderTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void inlineMechanicError_anchorsToRealYamlLine() throws Exception {
        String yaml = "ChainLightning:\n"
            + "  Mechanics:\n"
            + "    - 'Damage{Damage=5} @target'\n"
            + "    - 'Damage{Damage=5} @NotARealTargeter{}'\n";
        SnakeYamlConfig config = SnakeYamlConfig.ofText(yaml);
        List<int[]> origins = config.listItemPositions("ChainLightning.Mechanics");

        DiagnosticReporter reporter = new DiagnosticReporter();
        MechanicCompiler.compile("ChainLightning", "ChainLightning.Mechanics",
            List.of("Damage{Damage=5} @target", "Damage{Damage=5} @NotARealTargeter{}"),
            new File("weapon.yml"), new GlobalSymbolSource(), reporter);

        Diagnostic raw = reporter.all().stream()
            .filter(d -> d.message().toLowerCase().contains("targeter"))
            .findFirst().orElseThrow();
        Diagnostic anchored = MechanicSerializer.reanchor(raw, origins, config);

        // The bad targeter is the 2nd list item, on line index 3.
        assertEquals(3, anchored.primary().line(), anchored.toString());

        List<String> out = DiagnosticRenderer.render(anchored);
        String sourceLine = out.stream().filter(l -> l.contains("@NotARealTargeter")).findFirst().orElseThrow();
        assertTrue(sourceLine.contains("- 'Damage"), "renders the real YAML line with its dash/quote: " + sourceLine);
        assertTrue(sourceLine.contains(" 4 |"), "shows the real (1-based) line number: " + sourceLine);

        String caret = out.stream().filter(l -> l.contains("^")).findFirst().orElseThrow();
        assertEquals(sourceLine.indexOf("NotARealTargeter"), caret.indexOf('^'),
            "caret lands under the bad targeter:\n" + sourceLine + "\n" + caret);
    }
}
