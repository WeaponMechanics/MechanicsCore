package me.deecaad.core.file.serializers;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.file.BukkitConfig;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.verify.SchemaValidator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies ItemSerializer's declared schema: hallucinated-key detection at top level and inside
 * nested sections, range checks, and the inline-string short circuit. Drives SchemaValidator
 * directly since ItemSerializer has no root keyword (it is used as a nested serializer).
 */
class ItemSchemaVerificationTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private List<Diagnostic> validate(String yaml) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        SerializeData data = new SerializeData(new File("item.yml"), "Item", new BukkitConfig(config));
        List<Diagnostic> issues = new ArrayList<>();
        SchemaValidator.validate(new ItemSerializer().schema(), data, issues);
        return issues;
    }

    private Diagnostic firstOf(List<Diagnostic> issues, DiagnosticKind kind) {
        return issues.stream().filter(i -> i.kind() == kind).findFirst().orElse(null);
    }

    private long count(List<Diagnostic> issues, DiagnosticKind kind) {
        return issues.stream().filter(i -> i.kind() == kind).count();
    }

    private boolean hasErrors(List<Diagnostic> issues) {
        return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
    }

    @Test
    void goodItem_noIssues() {
        List<Diagnostic> issues = validate("""
            Item:
              Type: DIAMOND_SWORD
              Name: "<red>Sword"
              Enchantments:
                - sharpness-5
            """);
        assertEquals(0, count(issues, DiagnosticKind.UNKNOWN_KEY), issues.toString());
        assertTrue(!hasErrors(issues), issues.toString());
    }

    @Test
    void hallucinatedTopLevelKey_warnsWithHint() {
        List<Diagnostic> issues = validate("""
            Item:
              Type: DIAMOND_SWORD
              Namee: "oops"
            """);
        Diagnostic unknown = firstOf(issues, DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown, issues.toString());
        assertEquals("Item.Namee", unknown.source().configPath());
        assertEquals("Name", unknown.hint());
    }

    @Test
    void hallucinatedNestedKey_flagged() {
        List<Diagnostic> issues = validate("""
            Item:
              Type: DIAMOND_SWORD
              Durability:
                Max_Damage: 100
                MaxDamag: 50
            """);
        Diagnostic unknown = firstOf(issues, DiagnosticKind.UNKNOWN_KEY);
        assertNotNull(unknown, issues.toString());
        assertEquals("Item.Durability.MaxDamag", unknown.source().configPath());
    }

    @Test
    void missingRequiredNested_isError() {
        List<Diagnostic> issues = validate("""
            Item:
              Type: DIAMOND_SWORD
              Durability:
                Damage: 10
            """);
        assertEquals(1, count(issues, DiagnosticKind.MISSING_REQUIRED), issues.toString());
    }

    @Test
    void maxStackOutOfRange_isError() {
        List<Diagnostic> issues = validate("""
            Item:
              Type: DIAMOND_SWORD
              Max_Stack_Size: 200
            """);
        assertTrue(hasErrors(issues), issues.toString());
    }

    @Test
    void inlineStringItem_noFalsePositives() {
        List<Diagnostic> issues = validate("Item: DIAMOND_SWORD\n");
        assertEquals(0, issues.size(), issues.toString());
    }
}
