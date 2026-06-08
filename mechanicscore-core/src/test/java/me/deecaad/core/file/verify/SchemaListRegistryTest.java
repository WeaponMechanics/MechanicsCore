package me.deecaad.core.file.verify;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.simple.IntSerializer;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaListRegistryTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private ConfigVerifier verifier() {
        return new ConfigVerifier(List.of(new GearSerializer()), null);
    }

    private long errors(VerificationResult result) {
        return result.diagnostics().stream().filter(i -> i.severity() == Severity.ERROR).count();
    }

    @Test
    void validList_parses() {
        String yaml = """
            Gear:
              Enchantments:
                - sharpness-5
                - unbreaking-3
            """;
        VerificationResult result = verifier().verify(yaml, "gear.yml");
        assertTrue(result.ok(), result.toJson());
        assertEquals(0, errors(result), result.toJson());
    }

    @Test
    void listElement_outOfRange_isError() {
        String yaml = """
            Gear:
              Enchantments:
                - sharpness-0
            """;
        VerificationResult result = verifier().verify(yaml, "gear.yml");
        assertFalse(result.ok(), result.toJson());
    }

    @Test
    void listElement_invalidRegistry_isError() {
        String yaml = """
            Gear:
              Enchantments:
                - notarealenchant-5
            """;
        VerificationResult result = verifier().verify(yaml, "gear.yml");
        assertFalse(result.ok(), result.toJson());
    }

    @Test
    void registrySingle_parses() {
        String yaml = """
            Gear:
              Catalyst: sharpness
            """;
        VerificationResult result = verifier().verify(yaml, "gear.yml");
        assertEquals(0, errors(result), result.toJson());
    }

    @Test
    void registrySingle_invalid_isError() {
        String yaml = """
            Gear:
              Catalyst: notreal
            """;
        VerificationResult result = verifier().verify(yaml, "gear.yml");
        assertFalse(result.ok(), result.toJson());
    }

    @Test
    void hallucinatedKey_flaggedAlongsideList() {
        String yaml = """
            Gear:
              Enchantments:
                - sharpness-5
              Enchantmentss:
                - sharpness-5
            """;
        VerificationResult result = verifier().verify(yaml, "gear.yml");
        Diagnostic unknown = result.diagnostics().stream()
            .filter(i -> i.kind() == DiagnosticKind.UNKNOWN_KEY)
            .findFirst().orElse(null);
        assertTrue(unknown != null && unknown.source().configPath().equals("Gear.Enchantmentss"), result.toJson());
    }

    // --- test serializer ---

    public record Gear(List<List<Optional<Object>>> enchantments, Enchantment catalyst) {
    }

    public static final class GearSerializer implements Serializer<Gear> {
        @Override
        public String getKeyword() {
            return "Gear";
        }

        @Override
        public ConfigSchema schema() {
            return ConfigSchema.builder()
                .listKey("Enchantments", 1,
                    new RegistryValueSerializer<>(Enchantment.class, true),
                    new IntSerializer(1))
                .registryKey("Catalyst", Enchantment.class)
                .build();
        }

        @Override
        public @NotNull Gear serialize(@NotNull SerializeData data) throws SerializerException {
            SerializeData.ConfigListAccessor list = data.ofList("Enchantments");
            list.addArgument(new RegistryValueSerializer<>(Enchantment.class, true));
            list.requireAllPreviousArgs();
            list.addArgument(new IntSerializer(1));
            List<List<Optional<Object>>> enchantments = list.assertList();

            Enchantment catalyst = data.of("Catalyst").getBukkitRegistry(Enchantment.class).orElse(null);
            return new Gear(enchantments, catalyst);
        }
    }
}
