package me.deecaad.core.file;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * A side index from a config path to its source position, recovered from the underlying SnakeYAML
 * node tree. Bukkit's {@code YamlConfiguration} composes the same tree but discards positions when
 * it copies values into its map, so file-section diagnostics had no line/column. This re-reads the
 * raw text, keeps the marks, and lets {@link #enrich} upgrade a path-only {@link Diagnostic} to one
 * with a real caret snippet, matching inline-mechanic diagnostics.
 *
 * <p>Best-effort and never throws: a missing file, parse error, or unknown path leaves the
 * diagnostic untouched.
 */
public final class YamlPositions {

    public static final YamlPositions EMPTY = new YamlPositions(new HashMap<>(), new String[0]);

    private final Map<String, Entry> byPath;
    private final String[] lines;

    private YamlPositions(Map<String, Entry> byPath, String[] lines) {
        this.byPath = byPath;
        this.lines = lines;
    }

    public static @NotNull YamlPositions ofFile(@Nullable File file) {
        if (file == null)
            return EMPTY;
        try {
            return ofText(Files.readString(file.toPath()));
        } catch (Exception ex) {
            return EMPTY;
        }
    }

    public static @NotNull YamlPositions ofText(@NotNull String yaml) {
        try {
            Node root = new Yaml(new LoaderOptions()).compose(new StringReader(yaml));
            Map<String, Entry> map = new HashMap<>();
            if (root instanceof MappingNode mapping)
                index(mapping, "", map);
            return new YamlPositions(map, yaml.split("\\R", -1));
        } catch (Exception ex) {
            return EMPTY;
        }
    }

    private static void index(MappingNode mapping, String prefix, Map<String, Entry> out) {
        for (NodeTuple tuple : mapping.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode keyNode))
                continue;
            String rawKey = keyNode.getValue();
            String path = prefix.isEmpty() ? norm(rawKey) : prefix + "." + norm(rawKey);

            Mark keyMark = keyNode.getStartMark();
            Node valueNode = tuple.getValueNode();
            Mark valueMark = valueNode == null ? null : valueNode.getStartMark();
            int valueLen = valueNode instanceof ScalarNode scalar ? scalar.getValue().length() : 0;

            out.put(path, new Entry(
                keyMark == null ? -1 : keyMark.getLine(),
                keyMark == null ? 0 : keyMark.getColumn(),
                rawKey.length(),
                valueMark == null ? -1 : valueMark.getLine(),
                valueMark == null ? 0 : valueMark.getColumn(),
                valueLen));

            if (valueNode instanceof MappingNode child)
                index(child, path, out);
        }
    }

    /**
     * Returns {@code diagnostic} with a real {@link Span} and raw line filled in from the YAML
     * position of its config path. Diagnostics that already carry a position (e.g. inline mechanic
     * args) or whose path is not in the file are returned unchanged.
     */
    public @NotNull Diagnostic enrich(@NotNull Diagnostic diagnostic) {
        if (diagnostic.primary().line() >= 0)
            return diagnostic;

        String path = norm(diagnostic.source().configPath());
        Entry entry = byPath.get(path);
        if (entry == null) {
            // A missing key isn't in the file; point at its parent section instead.
            int dot = path.lastIndexOf('.');
            entry = dot < 0 ? null : byPath.get(path.substring(0, dot));
            if (entry == null)
                return diagnostic;
        }

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

    private static boolean pointsAtValue(DiagnosticKind kind) {
        return switch (kind) {
            case INVALID_TYPE, OUT_OF_RANGE, INVALID_VALUE -> true;
            default -> false;
        };
    }

    private static String norm(String path) {
        return MapConfigLike.normalizeKey(path);
    }

    private record Entry(int keyLine, int keyCol, int keyLen, int valueLine, int valueCol, int valueLen) {
    }
}
