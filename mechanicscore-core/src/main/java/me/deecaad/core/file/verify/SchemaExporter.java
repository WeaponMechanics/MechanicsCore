package me.deecaad.core.file.verify;

import me.deecaad.core.file.Serializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exports the declared schemas of a serializer set as a standard JSON Schema document, for an LLM
 * harness to consume before generating config. Serializers with a non-null {@link Serializer#schema()}
 * are exported completely and accurately; serializers without one are skipped.
 */
public final class SchemaExporter {

    private final List<Serializer<?>> serializers;

    public SchemaExporter(@NotNull List<Serializer<?>> serializers) {
        this.serializers = serializers;
    }

    public @NotNull String toJsonSchema() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"properties\":{");

        boolean first = true;
        for (Serializer<?> serializer : serializers) {
            ConfigSchema schema = serializer.schema();
            String keyword = serializer.getKeyword();
            if (schema == null || keyword == null)
                continue;

            if (!first)
                sb.append(',');
            first = false;
            sb.append(quote(keyword)).append(':');
            writeObject(sb, schema);
        }

        sb.append("}}");
        return sb.toString();
    }

    private void writeObject(StringBuilder sb, ConfigSchema schema) {
        sb.append("{\"type\":\"object\",\"properties\":{");
        boolean first = true;
        StringBuilder required = new StringBuilder();
        boolean firstRequired = true;

        for (KeySpec spec : schema.keys()) {
            if (!first)
                sb.append(',');
            first = false;
            sb.append(quote(spec.name())).append(':');
            writeProperty(sb, spec);

            if (spec.required()) {
                if (!firstRequired)
                    required.append(',');
                firstRequired = false;
                required.append(quote(spec.name()));
            }
        }

        sb.append("},\"required\":[").append(required).append(']');
        sb.append(",\"additionalProperties\":").append(schema.allowUnknown());
        sb.append('}');
    }

    private void writeProperty(StringBuilder sb, KeySpec spec) {
        switch (spec.type()) {
            case INT -> writeNumber(sb, "integer", spec.range());
            case DOUBLE -> writeNumber(sb, "number", spec.range());
            case BOOL -> sb.append("{\"type\":\"boolean\"}");
            case STRING, COLOR, MATERIAL, ENTITY, SOUND, REGISTRY -> sb.append("{\"type\":\"string\"}");
            case ENUM -> {
                sb.append("{\"type\":\"string\",\"enum\":[");
                List<String> options = spec.enumValues();
                for (int i = 0; i < options.size(); i++) {
                    if (i > 0)
                        sb.append(',');
                    sb.append(quote(options.get(i)));
                }
                sb.append("]}");
            }
            case NESTED -> writeNested(sb, spec);
            case REGISTRY_SERIALIZER -> sb.append("{\"type\":\"object\"}");
            case LIST, REGISTRY_SERIALIZER_LIST -> sb.append("{\"type\":\"array\"}");
        }
    }

    private void writeNested(StringBuilder sb, KeySpec spec) {
        Class<? extends Serializer<?>> nestedClass = spec.nested();
        if (nestedClass != null) {
            try {
                ConfigSchema nestedSchema = nestedClass.getDeclaredConstructor().newInstance().schema();
                if (nestedSchema != null) {
                    writeObject(sb, nestedSchema);
                    return;
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall through to a generic object.
            }
        }
        sb.append("{\"type\":\"object\"}");
    }

    private void writeNumber(StringBuilder sb, String type, Range range) {
        sb.append("{\"type\":\"").append(type).append('"');
        if (range != null) {
            if (range.min() != null)
                sb.append(",\"minimum\":").append(trim(range.min()));
            if (range.max() != null)
                sb.append(",\"maximum\":").append(trim(range.max()));
        }
        sb.append('}');
    }

    private static String trim(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value))
            return Long.toString((long) value);
        return Double.toString(value);
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
