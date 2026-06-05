package me.deecaad.core.file;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.file.verify.SchemaValidator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateExpansionTest {

    private final File file = new File("test.yml");
    private final MechanicsLogger debug = Mockito.mock(MechanicsLogger.class);

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private YamlConfiguration yaml(String text) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(text);
        } catch (InvalidConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        return config;
    }

    private ConfigTemplates templates(String text) {
        ConfigTemplates templates = new ConfigTemplates(debug);
        templates.registerFile(file, yaml(text));
        return templates;
    }

    private ConfigurationSection expand(ConfigTemplates templates, String rootText, String path) {
        YamlConfiguration root = yaml(rootText);
        TemplateExpander.expand(root, templates, file, debug);
        return root.getConfigurationSection(path);
    }

    @Test
    void reference_resolvesTemplate() {
        ConfigTemplates templates = templates("standard:\n  A: 1\n  B: 2\n");
        ConfigurationSection block = expand(templates, "Weapon:\n  Block:\n    Path_To: standard\n", "Weapon.Block");

        assertEquals(1, block.getInt("A"));
        assertEquals(2, block.getInt("B"));
        assertFalse(block.contains("Path_To"));
    }

    @Test
    void localKeyOverridesTemplate() {
        ConfigTemplates templates = templates("standard:\n  Radius: 5\n  Power: 9\n");
        ConfigurationSection e = expand(templates, "W:\n  E:\n    Path_To: standard\n    Radius: 10\n", "W.E");

        assertEquals(10, e.getInt("Radius"));
        assertEquals(9, e.getInt("Power"));
    }

    @Test
    void nestedMapsMergeRecursively() {
        ConfigTemplates templates = templates("standard:\n  A:\n    B: 1\n    C: 2\n");
        ConfigurationSection x = expand(templates, "W:\n  X:\n    Path_To: standard\n    A:\n      C: 99\n", "W.X");

        assertEquals(1, x.getInt("A.B"));
        assertEquals(99, x.getInt("A.C"));
    }

    @Test
    void listsAreReplacedNotAppended() {
        ConfigTemplates templates = templates("standard:\n  L:\n  - x\n");
        ConfigurationSection x = expand(templates, "W:\n  X:\n    Path_To: standard\n    L:\n    - y\n", "W.X");

        assertEquals(List.of("y"), x.getStringList("L"));
    }

    @Test
    void chainedTemplatesResolve() {
        ConfigTemplates templates = templates("t1:\n  Base: 1\nt2:\n  Path_To: t1\n  Extra: 2\n");
        ConfigurationSection x = expand(templates, "W:\n  X:\n    Path_To: t2\n", "W.X");

        assertEquals(1, x.getInt("Base"));
        assertEquals(2, x.getInt("Extra"));
        assertFalse(x.contains("Path_To"));
    }

    @Test
    void cycleIsDetectedWithoutOverflow() {
        ConfigTemplates templates = templates("t1:\n  Path_To: t2\nt2:\n  Path_To: t1\n");

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            ConfigurationSection x = expand(templates, "W:\n  X:\n    Path_To: t1\n", "W.X");
            assertFalse(x.contains("Path_To"));
        });
        Mockito.verify(debug, Mockito.atLeastOnce()).severe(Mockito.any(String[].class));
    }

    @Test
    void missingTemplateReportsAndDropsReference() {
        ConfigTemplates templates = templates("standard:\n  A: 1\n");
        ConfigurationSection x = expand(templates, "W:\n  X:\n    Path_To: nonexistent\n    Local: 7\n", "W.X");

        assertFalse(x.contains("Path_To"));
        assertEquals(7, x.getInt("Local"));
        assertFalse(x.contains("A"));
        Mockito.verify(debug, Mockito.atLeastOnce()).severe(Mockito.any(String[].class));
    }

    @Test
    void duplicateTemplateNameKeepsFirst() {
        ConfigTemplates templates = new ConfigTemplates(debug);
        templates.registerFile(file, yaml("standard:\n  Value: 1\n"));
        templates.registerFile(file, yaml("standard:\n  Value: 2\n"));

        ConfigurationSection x = expand(templates, "W:\n  X:\n    Path_To: standard\n", "W.X");
        assertEquals(1, x.getInt("Value"));
    }

    @Test
    void noTemplatesIsNoOp() {
        ConfigTemplates templates = new ConfigTemplates(debug);
        YamlConfiguration root = yaml("W:\n  X:\n    A: 1\n    B: 2\n");
        TemplateExpander.expand(root, templates, file, debug);

        assertEquals(1, root.getInt("W.X.A"));
        assertEquals(2, root.getInt("W.X.B"));
    }

    @Test
    void templatesFolderIsNotInstantiatedAsEntry(@TempDir Path dir) throws Exception {
        Path weapons = Files.createDirectories(dir.resolve("weapons"));
        Path templatesDir = Files.createDirectories(weapons.resolve("templates"));
        Files.writeString(templatesDir.resolve("shared.yml"), "shared_block:\n  Value: 5\n");
        Files.writeString(weapons.resolve("bar.yml"), "bar:\n  Path_To: shared_block\n");

        RootFileReader<Marker, MarkerSerializer> reader = new RootFileReader<>(
            dir.toFile(), debug, getClass().getClassLoader(), MarkerSerializer.class, "weapons");

        Configuration result = reader.read();

        assertTrue(result.contains("bar"), "entry should be instantiated");
        assertFalse(result.contains("shared_block"), "template should NOT be instantiated as an entry");
    }

    private List<Diagnostic> validateSection(String rootText, String sectionKey) {
        YamlConfiguration cfg = yaml(rootText);
        SerializeData data = new SerializeData(file, sectionKey, new BukkitConfig(cfg));
        List<Diagnostic> out = new ArrayList<>();
        SchemaValidator.validate(new BoomSerializer().schema(), data, out);
        return out;
    }

    @Test
    void referenceSkipsRequiredButValidatesOverrides() {
        List<Diagnostic> diags = validateSection("Boom:\n  Path_To: standard\n  Radius: 5\n", "Boom");
        assertTrue(diags.isEmpty(), diags::toString);
    }

    @Test
    void referenceFlagsUnknownOverrideKey() {
        List<Diagnostic> diags = validateSection("Boom:\n  Path_To: standard\n  Bogus: 5\n", "Boom");
        assertEquals(1, diags.size(), diags::toString);
        assertEquals(DiagnosticKind.UNKNOWN_KEY, diags.get(0).kind());
    }

    @Test
    void nonReferenceStillEnforcesRequired() {
        List<Diagnostic> diags = validateSection("Boom:\n  Radius: 5\n", "Boom");
        assertTrue(diags.stream().anyMatch(d -> d.kind() == DiagnosticKind.MISSING_REQUIRED), diags::toString);
    }

    public record Marker(String name, int value) {
    }

    public record Boom() {
    }

    public static final class BoomSerializer implements Serializer<Boom> {
        @Override
        public @Nullable String getKeyword() {
            return "Boom";
        }

        @Override
        public @Nullable ConfigSchema schema() {
            return ConfigSchema.builder().intKey("Damage").required().intKey("Radius").build();
        }

        @Override
        public @NotNull Boom serialize(@NotNull SerializeData data) {
            return new Boom();
        }
    }

    public static final class MarkerSerializer implements Serializer<Marker> {
        @Override
        public @NotNull Marker serialize(@NotNull SerializeData data) throws SerializerException {
            return new Marker(data.getKey(), data.of("Value").getInt().orElse(0));
        }
    }
}
