package me.deecaad.core.file.verify;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.DiagnosticRenderer;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigVerifierTest {

    private ConfigVerifier verifier() {
        return new ConfigVerifier(List.of(new SquareSerializer()), null);
    }

    private long count(VerificationResult result, DiagnosticKind kind) {
        return result.diagnostics().stream().filter(i -> i.kind() == kind).count();
    }

    @Test
    void goodConfig_isOk_noUnknownKeys() {
        String yaml = """
            Square:
              Offset:
                X: 1.0
                Y: 2.0
              Length: 5
              R: 0.5
            """;
        VerificationResult result = verifier().verify(yaml, "good.yml");
        assertTrue(result.ok(), result.toJson());
        assertEquals(0, count(result, DiagnosticKind.UNKNOWN_KEY), result.toJson());
    }

    @Test
    void hallucinatedKey_warnsWithHint() {
        String yaml = """
            Square:
              Length: 5
              Red: 0.5
            """;
        VerificationResult result = verifier().verify(yaml, "typo.yml");
        Diagnostic unknown = result.diagnostics().stream()
            .filter(i -> i.kind() == DiagnosticKind.UNKNOWN_KEY)
            .findFirst().orElse(null);
        assertNotNull(unknown, result.toJson());
        assertEquals("Square.Red", unknown.source().configPath());
        assertNotNull(unknown.hint(), "expected a did-you-mean hint");
    }

    @Test
    void outOfRange_isError() {
        String yaml = """
            Square:
              Length: -5
            """;
        VerificationResult result = verifier().verify(yaml, "range.yml");
        assertFalse(result.ok(), result.toJson());
    }

    @Test
    void missingRequired_isError() {
        String yaml = """
            Square:
              R: 0.5
            """;
        VerificationResult result = verifier().verify(yaml, "missing.yml");
        assertEquals(1, count(result, DiagnosticKind.MISSING_REQUIRED), result.toJson());
        assertFalse(result.ok());
    }

    @Test
    void inactiveKey_notFlaggedUnknown_provesControlFlowFix() {
        String yaml = """
            Square:
              Length: 5
              Explosion: false
              Radius: 99
            """;
        VerificationResult result = verifier().verify(yaml, "inactive.yml");
        assertEquals(0, count(result, DiagnosticKind.UNKNOWN_KEY), "Radius is declared, not hallucinated");
        assertEquals(1, count(result, DiagnosticKind.INACTIVE_KEY), result.toJson());
    }

    @Test
    void nestedHallucinatedKey_flagged() {
        String yaml = """
            Square:
              Offset:
                X: 1.0
                Y: 2.0
                Z: 3.0
              Length: 5
            """;
        VerificationResult result = verifier().verify(yaml, "nested.yml");
        Diagnostic unknown = result.diagnostics().stream()
            .filter(i -> i.kind() == DiagnosticKind.UNKNOWN_KEY)
            .findFirst().orElse(null);
        assertNotNull(unknown, result.toJson());
        assertEquals("Square.Offset.Z", unknown.source().configPath());
    }

    @Test
    void schemaExport_isComplete() {
        String json = new SchemaExporter(List.of(new SquareSerializer())).toJsonSchema();
        assertTrue(json.contains("\"Square\""), json);
        assertTrue(json.contains("\"Length\""), json);
        assertTrue(json.contains("\"minimum\":0"), json);
        assertTrue(json.contains("\"required\":[\"Length\"]"), json);
    }

    private Diagnostic firstOfKind(VerificationResult result, DiagnosticKind kind) {
        return result.diagnostics().stream().filter(i -> i.kind() == kind).findFirst().orElse(null);
    }

    @Test
    void unknownKey_carriesYamlPosition() {
        String yaml = """
            Square:
              Length: 5
              Red: 0.5
            """;
        Diagnostic unknown = firstOfKind(verifier().verify(yaml, "typo.yml"), DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown);
        assertEquals(2, unknown.primary().line(), "Red is on the 3rd line (0-based 2)");
        assertEquals(2, unknown.primary().start(), "key starts after 2-space indent");
        assertEquals(5, unknown.primary().end());
        assertEquals("  Red: 0.5", unknown.source().rawLine());
    }

    @Test
    void outOfRange_pointsAtTheValueNotTheKey() {
        String yaml = """
            Square:
              Length: -5
            """;
        Diagnostic error = firstOfKind(verifier().verify(yaml, "range.yml"), DiagnosticKind.OUT_OF_RANGE);
        assertNotNull(error);
        assertEquals(1, error.primary().line());
        assertEquals("  Length: -5", error.source().rawLine());
        assertEquals("-5", error.source().rawLine().substring(error.primary().start()), "caret sits on the value");
    }

    @Test
    void nestedKey_carriesYamlPosition() {
        String yaml = """
            Square:
              Offset:
                X: 1.0
                Y: 2.0
                Z: 3.0
              Length: 5
            """;
        Diagnostic unknown = firstOfKind(verifier().verify(yaml, "nested.yml"), DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown);
        assertEquals(4, unknown.primary().line());
        assertEquals(4, unknown.primary().start(), "nested key under 4-space indent");
        assertEquals("    Z: 3.0", unknown.source().rawLine());
    }

    @Test
    void missingRequired_pointsAtParentSection() {
        String yaml = """
            Square:
              R: 0.5
            """;
        Diagnostic missing = firstOfKind(verifier().verify(yaml, "missing.yml"), DiagnosticKind.MISSING_REQUIRED);
        assertNotNull(missing);
        assertEquals(0, missing.primary().line(), "Length is absent, so we point at the Square section");
        assertEquals("Square:", missing.source().rawLine());
    }

    @Test
    void renderedSnippet_caretAlignsUnderKey() {
        String yaml = """
            Square:
              Length: 5
              Red: 0.5
            """;
        Diagnostic unknown = firstOfKind(verifier().verify(yaml, "typo.yml"), DiagnosticKind.UNKNOWN_KEY);
        List<String> out = DiagnosticRenderer.render(unknown);

        String source = out.stream().filter(l -> l.contains("Red:")).findFirst().orElseThrow();
        String caret = out.stream().filter(l -> l.matches(" *\\^+")).findFirst().orElseThrow();
        assertEquals(source.indexOf("Red"), caret.indexOf('^'), "caret column must line up under the key");
        assertEquals(3, caret.chars().filter(c -> c == '^').count(), "caret width matches 'Red'");
    }

    // --- test serializers ---

    public record Vec2(double x, double y) {
    }

    public record Square(Vec2 offset, int length, double r, double g, double b) {
    }

    public static final class Vec2Serializer implements Serializer<Vec2> {
        @Override
        public ConfigSchema schema() {
            return ConfigSchema.builder()
                .doubleKey("X").required()
                .doubleKey("Y").required()
                .build();
        }

        @Override
        public @NotNull Vec2 serialize(@NotNull SerializeData data) throws SerializerException {
            throw new UnsupportedOperationException();
        }
    }

    public static final class SquareSerializer implements Serializer<Square> {
        @Override
        public String getKeyword() {
            return "Square";
        }

        @Override
        public ConfigSchema schema() {
            return ConfigSchema.builder()
                .nested("Offset", Vec2Serializer.class)
                .intKey("Length").required().range(0, null)
                .doubleKey("R").range(0.0, 1.0)
                .doubleKey("G").range(0.0, 1.0)
                .doubleKey("B").range(0.0, 1.0)
                .boolKey("Explosion")
                .intKey("Radius").activeWhen("Explosion", true)
                .build();
        }

        @Override
        public @NotNull Square serialize(@NotNull SerializeData data) throws SerializerException {
            throw new UnsupportedOperationException();
        }
    }
}
