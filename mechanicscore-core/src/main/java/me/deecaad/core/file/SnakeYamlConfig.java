package me.deecaad.core.file;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
import me.deecaad.core.utils.SerializerUtil;
import me.deecaad.core.utils.StringUtil;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ConfigLike} backed directly by SnakeYAML. The value tree is a plain nested
 * {@code Map<String, Object>} (sections are maps, lists are lists, scalars are typed), and a parallel
 * index maps each config path to its source line/column recovered from the SnakeYAML marks. This lets
 * {@link #getLocation} render a "located at ... ^^^" caret snippet and {@link #enrich} upgrade a
 * path-only {@link Diagnostic}, so config-file errors point at the exact line just like inline-mechanic
 * errors do. Bukkit's {@code YamlConfiguration} discards those marks, which is why we read YAML here
 * instead.
 *
 * <p>Key matching is case-sensitive and dotted-path, matching the previous Bukkit behavior.
 */
public final class SnakeYamlConfig implements ConfigLike {

    private static final Object MISSING = new Object();
    private static final String INDENT = "    ";

    private final @NotNull Map<String, Object> root;
    private final @NotNull String[] lines;
    private final @NotNull Map<String, Entry> positions;

    private SnakeYamlConfig(@NotNull Map<String, Object> root, @NotNull String[] lines, @NotNull Map<String, Entry> positions) {
        this.root = root;
        this.lines = lines;
        this.positions = positions;
    }

    public static @NotNull SnakeYamlConfig ofFile(@NotNull File file) throws IOException, InvalidConfigurationException {
        return ofText(Files.readString(file.toPath()));
    }

    @SuppressWarnings("unchecked")
    public static @NotNull SnakeYamlConfig ofText(@NotNull String yaml) throws InvalidConfigurationException {
        try {
            Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
            Map<String, Object> root = loaded instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();

            Map<String, Entry> positions = new LinkedHashMap<>();
            Node node = new Yaml(new LoaderOptions()).compose(new StringReader(yaml));
            if (node instanceof MappingNode mapping)
                index(mapping, "", positions);

            return new SnakeYamlConfig(root, yaml.split("\\R", -1), positions);
        } catch (Exception ex) {
            throw new InvalidConfigurationException(ex);
        }
    }

    /**
     * The raw nested value tree. Mutable, so {@link TemplateExpander} can resolve {@code Path_To}
     * references in place before serialization.
     */
    public @NotNull Map<String, Object> root() {
        return root;
    }

    @SuppressWarnings("unchecked")
    private @NotNull Object resolve(@Nullable String path) {
        if (path == null || path.isEmpty())
            return root;

        Map<String, Object> current = root;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map<?, ?> map))
                return MISSING;
            current = (Map<String, Object>) map;
        }
        String last = parts[parts.length - 1];
        return current.containsKey(last) ? current.get(last) : MISSING;
    }

    @Override
    public boolean contains(String key) {
        return resolve(key) != MISSING;
    }

    @Override
    public Object get(String key, Object def) {
        Object value = resolve(key);
        return value == MISSING ? def : value;
    }

    @Override
    public boolean isString(String key) {
        return resolve(key) instanceof String;
    }

    @Override
    public List<?> getList(String key) {
        Object value = resolve(key);
        return value instanceof List<?> list ? list : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getKeys(String path, boolean deep) {
        Object section = resolve(path);
        if (!(section instanceof Map<?, ?> map))
            return List.of();

        List<String> keys = new ArrayList<>();
        collectKeys((Map<String, Object>) map, "", deep, keys);
        return keys;
    }

    private static void collectKeys(@NotNull Map<String, Object> section, @NotNull String prefix, boolean deep, @NotNull List<String> out) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            out.add(path);
            if (deep && entry.getValue() instanceof Map<?, ?> child)
                //noinspection unchecked
                collectKeys((Map<String, Object>) child, path, true, out);
        }
    }

    @Override
    public String getLocation(File localFile, String localPath) {
        String header = SerializerUtil.foundAt(localFile, localPath);
        Entry entry = lookup(localPath);
        if (entry == null)
            return header;

        boolean useValue = entry.valueLine() >= 0;
        int line = useValue ? entry.valueLine() : entry.keyLine();
        int col = useValue ? entry.valueCol() : entry.keyCol();
        int len = useValue ? entry.valueLen() : entry.keyLen();
        if (line < 0 || line >= lines.length)
            return header;

        return header + "\n"
            + INDENT + lines[line] + "\n"
            + StringUtil.repeat(" ", INDENT.length() + col) + StringUtil.repeat("^", Math.max(1, len));
    }

    /**
     * Fills a path-only {@link Diagnostic} with a real {@link Span} and raw source line from the YAML
     * position of its config path. Diagnostics that already carry a position, or whose path is not in
     * the file, are returned unchanged.
     */
    @Override
    public @NotNull Diagnostic enrich(@NotNull Diagnostic diagnostic) {
        if (diagnostic.primary().line() >= 0)
            return diagnostic;

        Entry entry = lookup(diagnostic.source().configPath());
        if (entry == null)
            return diagnostic;

        boolean useValue = pointsAtValue(diagnostic.kind()) && entry.valueLine() >= 0;
        int line = useValue ? entry.valueLine() : entry.keyLine();
        int col = useValue ? entry.valueCol() : entry.keyCol();
        int len = useValue ? entry.valueLen() : entry.keyLen();
        if (line < 0)
            return diagnostic;

        String raw = line < lines.length ? lines[line] : "";
        Span span = Span.of(line, col, col + Math.max(1, len));
        SourceRef source = new SourceRef(diagnostic.source().file(), diagnostic.source().configPath(),
            diagnostic.source().listIndex(), raw);
        return new Diagnostic(diagnostic.severity(), diagnostic.kind(), diagnostic.message(),
            source, span, diagnostic.secondary(), diagnostic.hint());
    }

    private @Nullable Entry lookup(@Nullable String path) {
        String norm = norm(path);
        Entry entry = positions.get(norm);
        if (entry != null)
            return entry;

        // A missing key isn't in the file; point at its parent section instead.
        int dot = norm.lastIndexOf('.');
        return dot < 0 ? null : positions.get(norm.substring(0, dot));
    }

    private static void index(@NotNull MappingNode mapping, @NotNull String prefix, @NotNull Map<String, Entry> out) {
        for (NodeTuple tuple : mapping.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode keyNode))
                continue;
            String path = prefix.isEmpty() ? norm(keyNode.getValue()) : prefix + "." + norm(keyNode.getValue());

            Mark keyMark = keyNode.getStartMark();
            Node valueNode = tuple.getValueNode();
            Mark valueMark = valueNode == null ? null : valueNode.getStartMark();
            int valueLen = valueNode instanceof ScalarNode scalar ? scalar.getValue().length() : 0;

            out.put(path, new Entry(
                keyMark == null ? -1 : keyMark.getLine(),
                keyMark == null ? 0 : keyMark.getColumn(),
                keyNode.getValue().length(),
                valueMark == null ? -1 : valueMark.getLine(),
                valueMark == null ? 0 : valueMark.getColumn(),
                valueLen));

            if (valueNode instanceof MappingNode child)
                index(child, path, out);
        }
    }

    private static boolean pointsAtValue(@NotNull DiagnosticKind kind) {
        return switch (kind) {
            case INVALID_TYPE, OUT_OF_RANGE, INVALID_VALUE -> true;
            default -> false;
        };
    }

    private static @NotNull String norm(@Nullable String path) {
        return path == null ? "" : MapConfigLike.normalizeKey(path);
    }

    private record Entry(int keyLine, int keyCol, int keyLen, int valueLine, int valueCol, int valueLen) {
    }
}
