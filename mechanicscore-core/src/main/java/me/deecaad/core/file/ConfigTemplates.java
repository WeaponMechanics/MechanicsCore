package me.deecaad.core.file;

import me.deecaad.core.MechanicsLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the raw (un-serialized) value trees declared in {@code templates/} folders for one reader
 * scope. {@link TemplateExpander} resolves {@code Path_To} references against this registry before
 * serialization, so templates never need to be serialized themselves.
 */
public final class ConfigTemplates {

    private final Map<String, Map<String, Object>> templates = new HashMap<>();
    private final MechanicsLogger debug;

    public ConfigTemplates(@NotNull MechanicsLogger debug) {
        this.debug = debug;
    }

    /**
     * Registers every top-level section in a {@code templates/} file by its section name. On a
     * duplicate name the first registration wins and an error is logged.
     */
    @SuppressWarnings("unchecked")
    public void registerFile(@NotNull File file, @NotNull Map<String, Object> root) {
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> section))
                continue;

            String name = entry.getKey();
            if (templates.containsKey(name)) {
                debug.severe("Duplicate config template '" + name + "'",
                    "Found a second template with this name in " + file.getName() + ".",
                    "Template names must be unique. Keeping the first one and ignoring this copy.");
                continue;
            }

            templates.put(name, (Map<String, Object>) TemplateExpander.deepCopy(section));
        }
    }

    /**
     * Returns a fresh deep copy of the named template, or null if no such template exists. A copy is
     * returned so callers may freely mutate (expand/merge) without corrupting the registry.
     */
    @SuppressWarnings("unchecked")
    public @Nullable Map<String, Object> get(@NotNull String name) {
        Map<String, Object> template = templates.get(name);
        return template == null ? null : (Map<String, Object>) TemplateExpander.deepCopy(template);
    }

    public @NotNull java.util.Set<String> names() {
        return templates.keySet();
    }

    public boolean isEmpty() {
        return templates.isEmpty();
    }
}
