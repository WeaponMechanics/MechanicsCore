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
import me.deecaad.core.file.SimpleSerializer;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The shared engine that validates a raw config section against a declared {@link ConfigSchema},
 * appending any {@link Diagnostic}s found. It validates only (presence, type, range, enum, unknown
 * keys, nested recursion); construction always goes through {@link Serializer#serialize}. The same
 * engine runs over file sections ({@code BukkitConfig}) and inline mechanic args ({@code MapConfigLike}).
 */
public final class SchemaValidator {

    private static final String UNIQUE_IDENTIFIER_NORM = normalize(InlineSerializer.UNIQUE_IDENTIFIER);

    private SchemaValidator() {
    }

    public static void validate(
        @NotNull ConfigSchema schema,
        @NotNull SerializeData data,
        @NotNull List<Diagnostic> out) {

        String base = data.getKey() == null ? "" : data.getKey();

        // Inline / path-to scalar form: the value is a bare string, not a section. Section-key
        // validation does not apply (the serializer handles the scalar in serialize()).
        if (data.getKey() != null) {
            try {
                if (data.of().is(String.class))
                    return;
            } catch (RuntimeException ignored) {
                // Fall through to normal section validation.
            }
        }

        // Validate each declared key.
        for (KeySpec spec : schema.keys()) {
            String path = base.isEmpty() ? spec.name() : base + "." + spec.name();
            boolean present = data.has(spec.name());
            boolean active = isActive(data, spec.condition());

            if (!active) {
                if (present)
                    out.add(Diagnostic.at(Severity.INFO, DiagnosticKind.INACTIVE_KEY,
                        SourceRef.ofConfig(data.getFile(), path),
                        "'" + spec.name() + "' has no effect (" + spec.condition().siblingKey()
                            + " is not " + spec.condition().expected() + ")", null));
                continue;
            }

            if (!present) {
                if (spec.required())
                    out.add(Diagnostic.at(Severity.ERROR, DiagnosticKind.MISSING_REQUIRED,
                        SourceRef.ofConfig(data.getFile(), path),
                        "missing required key '" + spec.name() + "'", null));
                continue;
            }

            try {
                coerce(spec, data, path, out);
            } catch (SerializerException ex) {
                out.add(toDiagnostic(ex, data, path));
            }
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
                if (norm.equals(UNIQUE_IDENTIFIER_NORM) || declaredNorm.contains(norm))
                    continue;

                String path = base.isEmpty() ? key : base + "." + key;
                String hint = declaredNames.isEmpty() ? null : StringUtil.didYouMean(key, declaredNames);
                out.add(Diagnostic.at(Severity.WARNING, DiagnosticKind.UNKNOWN_KEY,
                    SourceRef.ofConfig(data.getFile(), path), "unknown key '" + key + "'", hint));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void coerce(
        KeySpec spec,
        SerializeData data,
        String path,
        List<Diagnostic> out) throws SerializerException {

        SerializeData.ConfigAccessor acc = data.of(spec.name());
        switch (spec.type()) {
            case INT -> {
                applyRange(acc, spec);
                acc.getInt();
            }
            case DOUBLE -> {
                applyRange(acc, spec);
                acc.getDouble();
            }
            case BOOL -> acc.getBool();
            case STRING -> acc.get(String.class);
            case COLOR -> acc.getAdventure();
            case MATERIAL -> acc.getMaterial();
            case ENTITY -> acc.getEntityType();
            case ENUM -> acc.getEnum((Class) spec.enumType());
            case NESTED -> coerceNested(spec, data, path, out);
            case REGISTRY -> {
                Class clazz = spec.registryClass();
                if (clazz != null)
                    acc.getBukkitRegistry(clazz);
            }
            case REGISTRY_SERIALIZER -> coerceRegistrySerializer(spec, data, out, true);
            case REGISTRY_SERIALIZER_LIST -> coerceRegistrySerializer(spec, data, out, false);
            case LIST -> coerceList(spec, data);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void coerceList(KeySpec spec, SerializeData data) throws SerializerException {
        List<SimpleSerializer<?>> args = spec.listArgs();

        // Raw list (no element parsing): only assert it is a list.
        if (args == null || args.isEmpty()) {
            data.of(spec.name()).get(List.class);
            return;
        }

        SerializeData.ConfigListAccessor list = data.ofList(spec.name());
        for (int i = 0; i < args.size(); i++) {
            list.addArgument((SimpleSerializer) args.get(i));
            if (i + 1 == spec.requiredArgs())
                list.requireAllPreviousArgs();
        }
        if (spec.required())
            list.assertExists();

        list.assertList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void coerceRegistrySerializer(KeySpec spec, SerializeData data, List<Diagnostic> out, boolean single)
        throws SerializerException {

        if (spec.registry() == null)
            return;

        // forEachRegistryEntry resolves the chosen serializer(s) and yields each child SerializeData
        // without constructing. We recurse the schema (catching hallucinated nested keys), or fall
        // back to serialize() for not-yet-migrated nested serializers.
        data.of(spec.name()).forEachRegistryEntry(spec.registry(), single, (serializer, child) -> {
            ConfigSchema nestedSchema = serializer.schema();
            if (nestedSchema != null) {
                validate(nestedSchema, child, out);
            } else {
                try {
                    ((Serializer) serializer).serialize(child);
                } catch (SerializerException ex) {
                    out.add(toDiagnostic(ex, child, child.getKey() == null ? spec.name() : child.getKey()));
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void coerceNested(
        KeySpec spec,
        SerializeData data,
        String path,
        List<Diagnostic> out) throws SerializerException {

        Class<? extends Serializer<?>> nestedClass = spec.nested();
        if (nestedClass == null)
            return;

        Serializer<?> nested;
        try {
            nested = nestedClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            out.add(Diagnostic.at(Severity.ERROR, DiagnosticKind.OTHER,
                SourceRef.ofConfig(data.getFile(), path),
                "could not instantiate nested serializer " + nestedClass.getSimpleName(), null));
            return;
        }

        ConfigSchema nestedSchema = nested.schema();
        if (nestedSchema != null) {
            validate(nestedSchema, data.move(spec.name()), out);
        } else {
            data.of(spec.name()).serialize((Serializer) nested);
        }
    }

    private static void applyRange(SerializeData.ConfigAccessor acc, KeySpec spec) throws SerializerException {
        Range range = spec.range();
        if (range == null || (range.min() == null && range.max() == null))
            return;
        acc.assertRange(range.min(), range.max());
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

    private static Diagnostic toDiagnostic(SerializerException ex, SerializeData data, String path) {
        String message = String.join("; ", ex.getMessages());
        if (message.isEmpty())
            message = "invalid value";
        return Diagnostic.at(Severity.ERROR, classify(message), SourceRef.ofConfig(data.getFile(), path), message, null);
    }

    private static DiagnosticKind classify(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("missing required"))
            return DiagnosticKind.MISSING_REQUIRED;
        if (lower.contains("range") || lower.contains("between") || lower.contains("must be"))
            return DiagnosticKind.OUT_OF_RANGE;
        if (lower.contains("expected") || lower.contains("type"))
            return DiagnosticKind.INVALID_TYPE;
        return DiagnosticKind.INVALID_VALUE;
    }

    static @NotNull String normalize(@NotNull String str) {
        return MapConfigLike.normalizeKey(str);
    }
}
