package me.deecaad.core.file;

import me.deecaad.core.MechanicsLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Expands {@code Path_To} template references in raw config BEFORE serialization. A section containing
 * a {@code Path_To} string is replaced by a deep copy of the referenced template, with the section's
 * other keys deep-merged on top (local overrides template). Templates may themselves reference other
 * templates (chaining), with cycle detection. After expansion the config is equivalent to hand-typed
 * inline config, so serializers need no knowledge of templates.
 */
public final class TemplateExpander {

    public static final String REFERENCE_KEY = "Path_To";

    private TemplateExpander() {
    }

    /**
     * Expands every template reference in {@code root}, mutating it in place.
     */
    public static void expand(@NotNull YamlConfiguration root, @NotNull ConfigTemplates templates,
                              @NotNull File file, @NotNull MechanicsLogger debug) {
        if (templates.isEmpty())
            return;
        expandNode(root, templates, file, debug, new ArrayDeque<>());
    }

    /**
     * Fully expands {@code node} in place: resolves its own {@code Path_To} (if any), then recurses
     * into every child section.
     */
    private static void expandNode(@NotNull ConfigurationSection node, @NotNull ConfigTemplates templates,
                                   @NotNull File file, @NotNull MechanicsLogger debug, @NotNull Deque<String> visiting) {
        if (node.isString(REFERENCE_KEY))
            resolveReference(node, templates, file, debug, visiting);

        for (String key : new ArrayList<>(node.getKeys(false))) {
            if (node.isConfigurationSection(key))
                expandNode(node.getConfigurationSection(key), templates, file, debug, visiting);
        }
    }

    /**
     * Resolves the {@code Path_To} reference declared directly on {@code node}, rewriting {@code node}
     * in place to be the referenced template with {@code node}'s other keys merged on top.
     */
    private static void resolveReference(@NotNull ConfigurationSection node, @NotNull ConfigTemplates templates,
                                         @NotNull File file, @NotNull MechanicsLogger debug, @NotNull Deque<String> visiting) {
        String name = node.getString(REFERENCE_KEY);

        if (visiting.contains(name)) {
            debug.severe("Cyclic config template reference '" + name + "'",
                "Reference chain: " + String.join(" -> ", visiting) + " -> " + name,
                "A template cannot (directly or indirectly) reference itself. Found in " + file.getName() + ".");
            node.set(REFERENCE_KEY, null);
            return;
        }

        ConfigurationSection template = templates.get(name);
        if (template == null) {
            debug.severe("Unknown config template '" + name + "'",
                "Referenced via " + REFERENCE_KEY + " in " + file.getName() + ", but no template with that name exists.",
                "Available templates: " + String.join(", ", templates.names()));
            node.set(REFERENCE_KEY, null);
            return;
        }

        visiting.push(name);
        expandNode(template, templates, file, debug, visiting);
        visiting.pop();

        deepMerge(template, node);

        for (String key : new ArrayList<>(node.getKeys(false)))
            node.set(key, null);
        copyInto(template, node);
    }

    /**
     * Merges {@code overlay} onto {@code base} (overlay wins). Nested sections merge recursively;
     * scalars and lists are replaced wholesale. The {@link #REFERENCE_KEY} is never copied.
     */
    private static void deepMerge(@NotNull ConfigurationSection base, @NotNull ConfigurationSection overlay) {
        for (String key : overlay.getKeys(false)) {
            if (key.equals(REFERENCE_KEY))
                continue;

            if (overlay.isConfigurationSection(key) && base.isConfigurationSection(key)) {
                deepMerge(base.getConfigurationSection(key), overlay.getConfigurationSection(key));
            } else if (overlay.isConfigurationSection(key)) {
                copyInto(overlay.getConfigurationSection(key), base.createSection(key));
            } else {
                base.set(key, copyValue(overlay.get(key)));
            }
        }
    }

    /**
     * Returns a deep copy of a section, detached into a fresh {@link MemoryConfiguration}.
     */
    static @NotNull ConfigurationSection deepCopy(@NotNull ConfigurationSection src) {
        MemoryConfiguration copy = new MemoryConfiguration();
        copyInto(src, copy);
        return copy;
    }

    private static void copyInto(@NotNull ConfigurationSection src, @NotNull ConfigurationSection dest) {
        for (String key : src.getKeys(false)) {
            if (src.isConfigurationSection(key)) {
                copyInto(src.getConfigurationSection(key), dest.createSection(key));
            } else {
                dest.set(key, copyValue(src.get(key)));
            }
        }
    }

    private static Object copyValue(Object value) {
        if (value instanceof List<?> list)
            return new ArrayList<>(list);
        return value;
    }
}
