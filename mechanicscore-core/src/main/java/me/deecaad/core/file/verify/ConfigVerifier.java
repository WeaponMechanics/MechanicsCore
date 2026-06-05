package me.deecaad.core.file.verify;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SnakeYamlConfig;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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
        try {
            return verify(SnakeYamlConfig.ofFile(yamlFile), new File(yamlFile.getName()), yamlFile.getName());
        } catch (IOException | InvalidConfigurationException ex) {
            return parseError(new File(yamlFile.getName()), yamlFile.getName(), ex);
        }
    }

    public @NotNull VerificationResult verify(@NotNull String yaml, @NotNull String displayName) {
        File file = new File(displayName);
        try {
            return verify(SnakeYamlConfig.ofText(yaml), file, displayName);
        } catch (InvalidConfigurationException ex) {
            return parseError(file, displayName, ex);
        }
    }

    private VerificationResult verify(SnakeYamlConfig config, File file, String displayName) {
        List<Diagnostic> located = new ArrayList<>();
        for (Diagnostic diagnostic : collect(config, file))
            located.add(config.enrich(diagnostic));
        return new VerificationResult(displayName, located);
    }

    private static VerificationResult parseError(File file, String displayName, Exception ex) {
        Diagnostic diagnostic = Diagnostic.at(Severity.ERROR, DiagnosticKind.OTHER,
            SourceRef.ofConfig(file, ""), "Failed to parse YAML: " + ex.getMessage(), null);
        return new VerificationResult(displayName, List.of(diagnostic));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Diagnostic> collect(SnakeYamlConfig config, File file) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (String key : config.getKeys(null, false)) {
            Serializer<?> serializer = serializers.get(key.toLowerCase(Locale.ROOT));
            if (serializer == null)
                continue;

            SerializeData data = new SerializeData(file, key, config);
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
