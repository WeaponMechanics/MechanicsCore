package me.deecaad.core.file;

import me.deecaad.core.MechanicsLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expands {@code Path_To} template references in the raw value tree BEFORE serialization. A section
 * containing a {@code Path_To} string is replaced by a deep copy of the referenced template, with the
 * section's other keys deep-merged on top (local overrides template). Templates may themselves
 * reference other templates (chaining), with cycle detection. After expansion the value tree is
 * equivalent to hand-typed inline config, so serializers need no knowledge of templates.
 */
public final class TemplateExpander {

    public static final String REFERENCE_KEY = "Path_To";

    private TemplateExpander() {
    }

    /**
     * Expands every template reference in {@code root}, mutating it in place.
     */
    public static void expand(@NotNull Map<String, Object> root, @NotNull ConfigTemplates templates,
                              @NotNull File file, @NotNull MechanicsLogger debug) {
        if (templates.isEmpty())
            return;
        expandNode(root, templates, file, debug, new ArrayDeque<>());
    }

    /**
     * Fully expands {@code node} in place: resolves its own {@code Path_To} (if any), then recurses
     * into every child section.
     */
    @SuppressWarnings("unchecked")
    private static void expandNode(@NotNull Map<String, Object> node, @NotNull ConfigTemplates templates,
                                   @NotNull File file, @NotNull MechanicsLogger debug, @NotNull Deque<String> visiting) {
        if (node.get(REFERENCE_KEY) instanceof String)
            resolveReference(node, templates, file, debug, visiting);

        for (Object value : new ArrayList<>(node.values())) {
            if (value instanceof Map<?, ?> child)
                expandNode((Map<String, Object>) child, templates, file, debug, visiting);
        }
    }

    /**
     * Resolves the {@code Path_To} reference declared directly on {@code node}, rewriting {@code node}
     * in place to be the referenced template with {@code node}'s other keys merged on top.
     */
    private static void resolveReference(@NotNull Map<String, Object> node, @NotNull ConfigTemplates templates,
                                         @NotNull File file, @NotNull MechanicsLogger debug, @NotNull Deque<String> visiting) {
        String name = (String) node.get(REFERENCE_KEY);

        if (visiting.contains(name)) {
            debug.severe("Cyclic config template reference '" + name + "'",
                "Reference chain: " + String.join(" -> ", visiting) + " -> " + name,
                "A template cannot (directly or indirectly) reference itself. Found in " + file.getName() + ".");
            node.remove(REFERENCE_KEY);
            return;
        }

        Map<String, Object> template = templates.get(name);
        if (template == null) {
            debug.severe("Unknown config template '" + name + "'",
                "Referenced via " + REFERENCE_KEY + " in " + file.getName() + ", but no template with that name exists.",
                "Available templates: " + String.join(", ", templates.names()));
            node.remove(REFERENCE_KEY);
            return;
        }

        visiting.push(name);
        expandNode(template, templates, file, debug, visiting);
        visiting.pop();

        deepMerge(template, node);
        node.clear();
        node.putAll(template);
    }

    /**
     * Merges {@code overlay} onto {@code base} (overlay wins). Nested sections merge recursively;
     * scalars and lists are replaced wholesale. The {@link #REFERENCE_KEY} is never copied.
     */
    @SuppressWarnings("unchecked")
    private static void deepMerge(@NotNull Map<String, Object> base, @NotNull Map<String, Object> overlay) {
        for (Map.Entry<String, Object> entry : overlay.entrySet()) {
            String key = entry.getKey();
            if (key.equals(REFERENCE_KEY))
                continue;

            Object overlayValue = entry.getValue();
            Object baseValue = base.get(key);
            if (overlayValue instanceof Map<?, ?> oMap && baseValue instanceof Map<?, ?> bMap) {
                deepMerge((Map<String, Object>) bMap, (Map<String, Object>) oMap);
            } else {
                base.put(key, deepCopy(overlayValue));
            }
        }
    }

    /**
     * Returns a deep copy of a value: maps and lists are copied recursively so callers may freely
     * mutate (expand/merge) without corrupting another instance; scalars are returned as-is.
     */
    @SuppressWarnings("unchecked")
    static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet())
                copy.put(entry.getKey(), deepCopy(entry.getValue()));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list)
                copy.add(deepCopy(element));
            return copy;
        }
        return value;
    }
}
