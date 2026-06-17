package me.deecaad.core.file.verify;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SnakeYamlConfig;
import me.deecaad.core.file.TemplateExpander;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Diffs a raw config section against a declared {@link ConfigSchema} for <em>key shape</em>: unknown
 * (misspelled/hallucinated) keys, inert keys, and missing required keys, recursing into nested
 * sections to do the same there. Leaf-value validation (type, range, enum, ...) is intentionally left
 * to {@link Serializer#serialize}, so each value problem is reported exactly once and through one
 * error system ({@link SerializerException#toDiagnostic}); the caller gates on a hard schema error
 * (missing required) so the serializer does not re-report it. The schema's declared types still drive
 * the JSON {@link SchemaExporter}; only the runtime value re-validation moved out. The same engine
 * runs over file sections ({@code SnakeYamlConfig}) and inline mechanic args ({@code MapConfigLike}).
 */
public final class SchemaValidator {

    private static final String UNIQUE_IDENTIFIER_NORM = normalize(InlineSerializer.UNIQUE_IDENTIFIER);
    private static final String REFERENCE_KEY_NORM = normalize(TemplateExpander.REFERENCE_KEY);

    private SchemaValidator() {
    }

    public static void validate(
        @NotNull ConfigSchema schema,
        @NotNull SerializeData data,
        @NotNull List<Diagnostic> out) {
        validate(schema, data, null, out);
    }

    /**
     * @param contexts The cast contexts in scope, used to check {@code CONTEXT} keys (From/To/...).
     *                 {@code null} outside the mechanics compiler, where such keys are plain strings.
     */
    public static void validate(
        @NotNull ConfigSchema schema,
        @NotNull SerializeData data,
        @Nullable Set<String> contexts,
        @NotNull List<Diagnostic> out) {

        String base = data.getKey() == null ? "" : data.getKey();

        // Path_To template reference (file configs only): the section's real content comes from the
        // referenced template, resolved before serialization, so required-key checks are relaxed here.
        boolean isReference = data.getConfig() instanceof SnakeYamlConfig && data.has(TemplateExpander.REFERENCE_KEY);

        // Inline scalar form: the value is a bare string, not a section. Section-key checks do not
        // apply (the serializer handles the scalar in serialize()).
        if (data.getKey() != null) {
            try {
                if (data.of().is(String.class))
                    return;
            } catch (RuntimeException ignored) {
                // Fall through to normal section validation.
            }
        }

        // Flag missing required keys and inert keys, and recurse into present nested sections so
        // unknown keys inside them are flagged too. Leaf-value validation (type, range, ...) is left
        // to the serializer; required-key presence is cheap and carries its own kind, so it stays here
        // (and the caller gates on it to avoid the serializer re-reporting it).
        for (KeySpec spec : schema.keys()) {
            String path = base.isEmpty() ? spec.name() : base + "." + spec.name();

            if (!isActive(data, spec.condition())) {
                if (data.has(spec.name()))
                    out.add(Diagnostic.at(Severity.INFO, DiagnosticKind.INACTIVE_KEY,
                        SourceRef.ofConfig(data.getFile(), path),
                        "'" + spec.name() + "' has no effect (" + spec.condition().siblingKey()
                            + " is not " + spec.condition().expected() + ")", null));
                continue;
            }

            if (!data.has(spec.name())) {
                if (spec.required() && !isReference)
                    out.add(Diagnostic.at(Severity.ERROR, DiagnosticKind.MISSING_REQUIRED,
                        SourceRef.ofConfig(data.getFile(), path),
                        "missing required key '" + spec.name() + "'", null));
                continue;
            }

            if (spec.type() == KeyType.CONTEXT) {
                checkContext(data, contexts, spec.name(), path, out);
                continue;
            }

            recurse(spec, data, contexts, out);
        }

        // Flag unknown (hallucinated) keys. Normalize both sides the same way MapConfigLike does
        // (lowercase, strip spaces and underscores) so inline args match declared keys.
        if (!schema.allowUnknown()) {
            List<String> declaredNames = new ArrayList<>();
            Set<String> declaredNorm = new HashSet<>();
            for (KeySpec spec : schema.keys()) {
                declaredNames.add(spec.name());
                declaredNorm.add(normalize(spec.name()));
            }

            Collection<String> presentKeys = data.getConfig().getKeys(base.isEmpty() ? null : base, false);
            for (String key : presentKeys) {
                String norm = normalize(key);
                if (norm.equals(UNIQUE_IDENTIFIER_NORM) || norm.equals(REFERENCE_KEY_NORM) || declaredNorm.contains(norm))
                    continue;

                String path = base.isEmpty() ? key : base + "." + key;
                String suggestion = declaredNames.isEmpty() ? null : StringUtil.didYouMean(key, declaredNames, key.length() + 2);
                String hint = suggestion == null ? null : "did you mean '" + suggestion + "'?";
                out.add(Diagnostic.at(Severity.WARNING, DiagnosticKind.UNKNOWN_KEY,
                    SourceRef.ofConfig(data.getFile(), path), "unknown key '" + key + "'", hint));
            }
        }
    }

    /**
     * Descends into the nested section(s) of a present declared key, for unknown-key detection only.
     * Leaf keys do nothing here. Construction and value errors are surfaced later by
     * {@link Serializer#serialize}, so this never reports them (that would double-report).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void recurse(KeySpec spec, SerializeData data, @Nullable Set<String> contexts, List<Diagnostic> out) {
        switch (spec.type()) {
            case NESTED -> {
                Class<? extends Serializer<?>> nestedClass = spec.nested();
                if (nestedClass == null)
                    return;
                Serializer<?> nested;
                try {
                    nested = nestedClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException ex) {
                    return;
                }
                ConfigSchema nestedSchema = nested.schema();
                if (nestedSchema != null)
                    validate(nestedSchema, data.move(spec.name()), contexts, out);
            }
            case REGISTRY_SERIALIZER -> recurseRegistry(spec, data, contexts, out, true);
            case REGISTRY_SERIALIZER_LIST -> recurseRegistry(spec, data, contexts, out, false);
            default -> {
                // Leaf key: the serializer validates its value.
            }
        }
    }

    private static void recurseRegistry(KeySpec spec, SerializeData data, @Nullable Set<String> contexts, List<Diagnostic> out, boolean single) {
        if (spec.registry() == null)
            return;

        // forEachRegistryEntry resolves the chosen serializer(s) and yields each child SerializeData
        // without constructing. We recurse a declared nested schema for hallucinated keys; a nested
        // serializer without a schema is left entirely to serialize().
        try {
            data.of(spec.name()).forEachRegistryEntry(spec.registry(), single, (serializer, child) -> {
                ConfigSchema nestedSchema = serializer.schema();
                if (nestedSchema != null)
                    validate(nestedSchema, child, contexts, out);
            });
        } catch (SerializerException ignored) {
            // Resolving the registry entry failed (e.g. unknown type). serialize() reports it.
        }
    }

    /**
     * Checks a {@code CONTEXT} key's value against the contexts in scope. A no-op when {@code contexts}
     * is null (outside the mechanics compiler), where the value is just a string the serializer reads.
     */
    private static void checkContext(SerializeData data, @Nullable Set<String> contexts, String name, String path, List<Diagnostic> out) {
        if (contexts == null)
            return;
        String value;
        try {
            value = data.of(name).get(String.class).orElse(null);
        } catch (RuntimeException ex) {
            return;
        }
        if (value == null || contexts.contains(value))
            return;
        String suggestion = StringUtil.didYouMean(value, contexts, value.length() + 2);
        String hint = suggestion == null ? null : "did you mean '" + suggestion + "'?";
        out.add(Diagnostic.at(Severity.ERROR, DiagnosticKind.INVALID_VALUE,
            SourceRef.ofConfig(data.getFile(), path), "unknown context '" + value + "'", hint));
    }

    private static boolean isActive(SerializeData data, Condition condition) {
        if (condition == null)
            return true;
        if (!data.has(condition.siblingKey()))
            return false;
        try {
            Object expected = condition.expected();
            if (expected instanceof Boolean) {
                return data.of(condition.siblingKey()).getBool().map(expected::equals).orElse(false);
            }
            return data.of(condition.siblingKey()).get(Object.class)
                .map(v -> v.toString().equalsIgnoreCase(expected.toString()))
                .orElse(false);
        } catch (SerializerException ex) {
            return false;
        }
    }

    static @NotNull String normalize(@NotNull String str) {
        return MapConfigLike.normalizeKey(str);
    }
}
