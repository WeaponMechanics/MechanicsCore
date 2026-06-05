package me.deecaad.core.file;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.file.verify.SchemaValidator;
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
import java.util.Map;

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

    private static Map<String, Object> tree(String text) throws Exception {
        return SnakeYamlConfig.ofText(text).root();
    }

    private ConfigTemplates templates(String text) throws Exception {
        ConfigTemplates templates = new ConfigTemplates(debug);
        templates.registerFile(file, tree(text));
        return templates;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> expand(ConfigTemplates templates, String rootText, String path) throws Exception {
        Map<String, Object> root = tree(rootText);
        TemplateExpander.expand(root, templates, file, debug);

        Map<String, Object> current = root;
        for (String part : path.split("\\."))
            current = (Map<String, Object>) current.get(part);
        return current;
    }

    private static Object get(Map<String, Object> map, String dotted) {
        Object current = map;
        for (String part : dotted.split("\\.")) {
            if (!(current instanceof Map<?, ?> section))
                return null;
            current = section.get(part);
        }
        return current;
    }

    private static int asInt(Map<String, Object> map, String dotted) {
        return ((Number) get(map, dotted)).intValue();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Map<String, Object> map, String dotted) {
        return (List<String>) get(map, dotted);
    }

    private static boolean has(Map<String, Object> map, String dotted) {
        Object current = map;
        String[] parts = dotted.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = ((Map<?, ?>) current).get(parts[i]);
            if (!(next instanceof Map<?, ?>))
                return false;
            current = next;
        }
        return ((Map<?, ?>) current).containsKey(parts[parts.length - 1]);
    }

    @Test
    void reference_resolvesTemplate() throws Exception {
        ConfigTemplates templates = templates("standard:\n  A: 1\n  B: 2\n");
        Map<String, Object> block = expand(templates, "Weapon:\n  Block:\n    Path_To: standard\n", "Weapon.Block");

        assertEquals(1, asInt(block, "A"));
        assertEquals(2, asInt(block, "B"));
        assertFalse(has(block, "Path_To"));
    }

    @Test
    void localKeyOverridesTemplate() throws Exception {
        ConfigTemplates templates = templates("standard:\n  Radius: 5\n  Power: 9\n");
        Map<String, Object> e = expand(templates, "W:\n  E:\n    Path_To: standard\n    Radius: 10\n", "W.E");

        assertEquals(10, asInt(e, "Radius"));
        assertEquals(9, asInt(e, "Power"));
    }

    @Test
    void nestedMapsMergeRecursively() throws Exception {
        ConfigTemplates templates = templates("standard:\n  A:\n    B: 1\n    C: 2\n");
        Map<String, Object> x = expand(templates, "W:\n  X:\n    Path_To: standard\n    A:\n      C: 99\n", "W.X");

        assertEquals(1, asInt(x, "A.B"));
        assertEquals(99, asInt(x, "A.C"));
    }

    @Test
    void listsAreReplacedNotAppended() throws Exception {
        ConfigTemplates templates = templates("standard:\n  L:\n  - x\n");
        Map<String, Object> x = expand(templates, "W:\n  X:\n    Path_To: standard\n    L:\n    - y\n", "W.X");

        assertEquals(List.of("y"), asStringList(x, "L"));
    }

    @Test
    void chainedTemplatesResolve() throws Exception {
        ConfigTemplates templates = templates("t1:\n  Base: 1\nt2:\n  Path_To: t1\n  Extra: 2\n");
        Map<String, Object> x = expand(templates, "W:\n  X:\n    Path_To: t2\n", "W.X");

        assertEquals(1, asInt(x, "Base"));
        assertEquals(2, asInt(x, "Extra"));
        assertFalse(has(x, "Path_To"));
    }

    @Test
    void cycleIsDetectedWithoutOverflow() {
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            ConfigTemplates templates = templates("t1:\n  Path_To: t2\nt2:\n  Path_To: t1\n");
            Map<String, Object> x = expand(templates, "W:\n  X:\n    Path_To: t1\n", "W.X");
            assertFalse(has(x, "Path_To"));
        });
        Mockito.verify(debug, Mockito.atLeastOnce()).severe(Mockito.any(String[].class));
    }

    @Test
    void missingTemplateReportsAndDropsReference() throws Exception {
        ConfigTemplates templates = templates("standard:\n  A: 1\n");
        Map<String, Object> x = expand(templates, "W:\n  X:\n    Path_To: nonexistent\n    Local: 7\n", "W.X");

        assertFalse(has(x, "Path_To"));
        assertEquals(7, asInt(x, "Local"));
        assertFalse(has(x, "A"));
        Mockito.verify(debug, Mockito.atLeastOnce()).severe(Mockito.any(String[].class));
    }

    @Test
    void duplicateTemplateNameKeepsFirst() throws Exception {
        ConfigTemplates templates = new ConfigTemplates(debug);
        templates.registerFile(file, tree("standard:\n  Value: 1\n"));
        templates.registerFile(file, tree("standard:\n  Value: 2\n"));

        Map<String, Object> x = expand(templates, "W:\n  X:\n    Path_To: standard\n", "W.X");
        assertEquals(1, asInt(x, "Value"));
    }

    @Test
    void noTemplatesIsNoOp() throws Exception {
        ConfigTemplates templates = new ConfigTemplates(debug);
        Map<String, Object> root = tree("W:\n  X:\n    A: 1\n    B: 2\n");
        TemplateExpander.expand(root, templates, file, debug);

        assertEquals(1, asInt(root, "W.X.A"));
        assertEquals(2, asInt(root, "W.X.B"));
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

    private List<Diagnostic> validateSection(String rootText, String sectionKey) throws Exception {
        SnakeYamlConfig cfg = SnakeYamlConfig.ofText(rootText);
        SerializeData data = new SerializeData(file, sectionKey, cfg);
        List<Diagnostic> out = new ArrayList<>();
        SchemaValidator.validate(new BoomSerializer().schema(), data, out);
        return out;
    }

    @Test
    void referenceSkipsRequiredButValidatesOverrides() throws Exception {
        List<Diagnostic> diags = validateSection("Boom:\n  Path_To: standard\n  Radius: 5\n", "Boom");
        assertTrue(diags.isEmpty(), diags::toString);
    }

    @Test
    void referenceFlagsUnknownOverrideKey() throws Exception {
        List<Diagnostic> diags = validateSection("Boom:\n  Path_To: standard\n  Bogus: 5\n", "Boom");
        assertEquals(1, diags.size(), diags::toString);
        assertEquals(DiagnosticKind.UNKNOWN_KEY, diags.get(0).kind());
    }

    @Test
    void nonReferenceStillEnforcesRequired() throws Exception {
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
