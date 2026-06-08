package me.deecaad.core.file;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
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
import org.yaml.snakeyaml.nodes.SequenceNode;

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
 * {@link #enrich} upgrade a path-only {@link Diagnostic} with a real {@link Span} and raw source line,
 * so config-file errors point at the exact line just like inline-mechanic errors do. Bukkit's
 * {@code YamlConfiguration} discards those marks, which is why we read YAML here instead.
 *
 * <p>Key matching is case-sensitive and dotted-path.
 */
public final class SnakeYamlConfig implements ConfigLike {

    private static final Object MISSING = new Object();

    private final @NotNull Map<String, Object> root;
    private final @NotNull String[] lines;
    private final @NotNull Map<String, Entry> positions;
    private final @NotNull Map<String, List<int[]>> listPositions;

    private SnakeYamlConfig(@NotNull Map<String, Object> root, @NotNull String[] lines,
                           @NotNull Map<String, Entry> positions, @NotNull Map<String, List<int[]>> listPositions) {
        this.root = root;
        this.lines = lines;
        this.positions = positions;
        this.listPositions = listPositions;
    }

    /**
     * The source position of each item in the list at {@code path}, as {@code {line, column}} (both
     * 0-based) pointing at the item's value. Used to re-anchor inline-mechanic diagnostics, which the
     * parser produces relative to the mechanic string, back onto the real YAML line. Empty when the
     * path is not a list this backend indexed.
     */
    public @NotNull List<int[]> listItemPositions(@Nullable String path) {
        List<int[]> raw = listPositions.getOrDefault(norm(path), List.of());
        List<int[]> adjusted = new ArrayList<>(raw.size());
        for (int[] pos : raw) {
            int line = pos[0];
            int column = pos[1];
            // SnakeYAML marks a quoted scalar at its opening quote; step past it to the content.
            if (line >= 0 && line < lines.length && column < lines[line].length()) {
                char c = lines[line].charAt(column);
                if (c == '\'' || c == '"')
                    column++;
            }
            adjusted.add(new int[]{line, column});
        }
        return adjusted;
    }

    /**
     * The raw source line at the given 0-based line index, or empty when out of range.
     */
    public @NotNull String sourceLine(int line) {
        return line >= 0 && line < lines.length ? lines[line] : "";
    }

    /**
     * Up to two source lines immediately preceding {@code line} (oldest-first), shown as context
     * above an error.
     */
    public @NotNull List<String> contextBefore(int line) {
        List<String> out = new ArrayList<>();
        for (int i = Math.max(0, line - 2); i < line; i++)
            out.add(lines[i]);
        return out;
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
            Map<String, List<int[]>> listPositions = new LinkedHashMap<>();
            Node node = new Yaml(new LoaderOptions()).compose(new StringReader(yaml));
            if (node instanceof MappingNode mapping)
                index(mapping, "", positions, listPositions);

            return new SnakeYamlConfig(root, yaml.split("\\R", -1), positions, listPositions);
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
            diagnostic.source().listIndex(), raw, contextBefore(line));
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

    private static void index(@NotNull MappingNode mapping, @NotNull String prefix,
                              @NotNull Map<String, Entry> out, @NotNull Map<String, List<int[]>> listOut) {
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

            if (valueNode instanceof MappingNode child) {
                index(child, path, out, listOut);
            } else if (valueNode instanceof SequenceNode sequence) {
                List<int[]> items = new ArrayList<>(sequence.getValue().size());
                for (Node item : sequence.getValue()) {
                    Mark mark = item.getStartMark();
                    items.add(new int[]{mark == null ? -1 : mark.getLine(), mark == null ? 0 : mark.getColumn()});
                }
                listOut.put(path, items);
            }
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
