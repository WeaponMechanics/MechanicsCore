package me.deecaad.core.file.verify;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.file.BukkitConfig;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.YamlPositions;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Verifies a YAML config against the registered serializers' declared schemas, producing a
 * structured {@link VerificationResult}. Built for an external LLM harness: feed it generated
 * config (file or string), get back every error plus hallucinated-key warnings.
 */
public final class ConfigVerifier {

    private final Map<String, Serializer<?>> serializers = new HashMap<>();
    private final List<IValidator> validators;

    public ConfigVerifier(@NotNull List<Serializer<?>> serializers, @Nullable List<IValidator> validators) {
        for (Serializer<?> serializer : serializers) {
            String keyword = serializer.getKeyword();
            if (keyword != null)
                this.serializers.put(keyword.toLowerCase(Locale.ROOT), serializer);
        }
        this.validators = validators == null ? List.of() : validators;
    }

    public @NotNull VerificationResult verify(@NotNull File yamlFile) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(yamlFile);
        return locate(YamlPositions.ofFile(yamlFile), collect(config, yamlFile), yamlFile.getName());
    }

    public @NotNull VerificationResult verify(@NotNull String yaml, @NotNull String displayName) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        return locate(YamlPositions.ofText(yaml), collect(config, new File(displayName)), displayName);
    }

    private static VerificationResult locate(YamlPositions positions, List<Diagnostic> raw, String displayName) {
        List<Diagnostic> located = new ArrayList<>(raw.size());
        for (Diagnostic diagnostic : raw)
            located.add(positions.enrich(diagnostic));
        return new VerificationResult(displayName, located);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Diagnostic> collect(YamlConfiguration config, File file) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (String key : config.getKeys(false)) {
            Serializer<?> serializer = serializers.get(key.toLowerCase(Locale.ROOT));
            if (serializer == null)
                continue;

            SerializeData data = new SerializeData(file, key, new BukkitConfig(config));
            ConfigSchema schema = serializer.schema();

            if (schema != null) {
                // Schema-backed: validate only. We never construct, so NMS-dependent serializers
                // (e.g. items) can be verified headlessly.
                SchemaValidator.validate(schema, data, diagnostics);
            } else {
                // Legacy serializer with no schema: the only way to validate is to run it.
                try {
                    data.of().serialize((Serializer) serializer);
                } catch (SerializerException ex) {
                    diagnostics.add(Diagnostic.at(Severity.ERROR, DiagnosticKind.OTHER,
                        SourceRef.ofConfig(file, key), joinOr(ex, "invalid value"), null));
                }
            }
        }

        return diagnostics;
    }

    private static String joinOr(SerializerException ex, String fallback) {
        String message = String.join("; ", ex.getMessages());
        return message.isEmpty() ? fallback : message;
    }
}
