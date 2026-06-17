package me.deecaad.core.file;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapConfigLike implements ConfigLike {

    private final Map<String, Holder> config;
    private File file;
    private String path;
    private String fullLine;

    public MapConfigLike(Map<String, Holder> config) {
        this.config = new HashMap<>();
        for (String key : config.keySet()) {
            String noCase = normalizeString(key);

            this.config.put(noCase, config.get(key));
        }
    }

    public MapConfigLike setDebugInfo(File file, String path, String fullLine) {
        this.file = file;
        this.path = path;
        this.fullLine = fullLine;
        return this;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return path;
    }

    public String getFullLine() {
        return fullLine;
    }

    @Override
    public boolean contains(String key) {
        return config.containsKey(normalizeString(key));
    }

    @Override
    public Object get(String key, Object def) {
        return config.getOrDefault(normalizeString(key), new Holder(def, 0, -1)).value;
    }

    @Override
    public boolean isString(String key) {
        return get(normalizeString(key), null) instanceof String;
    }

    @Override
    public List<?> getList(String key) {
        Object temp = get(normalizeString(key), null);

        if (temp instanceof List<?> list)
            return list;

        return List.of();
    }

    @Override
    public Collection<String> getKeys(String path, boolean deep) {
        return List.copyOf(config.keySet());
    }

    public @NotNull String normalizeString(@NotNull String str) {
        return normalizeKey(str);
    }

    /**
     * The canonical config-key normalization (lowercase, strip spaces and underscores). The
     * unknown-key diff in SchemaValidator must use this exact rule so inline keys match how they
     * are stored here.
     */
    public static @NotNull String normalizeKey(@NotNull String str) {
        return str.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    /** {@code index} is the value's column; {@code keyIndex} is the key's column (-1 if none, e.g. a list element). */
    public record Holder(Object value, int index, int keyIndex) {
    }
}
