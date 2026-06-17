package me.deecaad.core.file.verify;

import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.Severity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The outcome of verifying one config: every structured {@link Diagnostic} found.
 * {@link #toJson()} emits a compact, token-efficient shape for an LLM harness to consume.
 */
public record VerificationResult(@NotNull String file, @NotNull List<Diagnostic> diagnostics) {

    /**
     * @return true when no {@link Severity#ERROR} diagnostic was found. Unknown keys are warnings,
     *         so they do not by themselves make a result not-ok.
     */
    public boolean ok() {
        for (Diagnostic diagnostic : diagnostics)
            if (diagnostic.severity() == Severity.ERROR)
                return false;
        return true;
    }

    public @NotNull String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"file\":").append(quote(file)).append(",\"ok\":").append(ok()).append(",\"issues\":[");
        for (int i = 0; i < diagnostics.size(); i++) {
            Diagnostic diagnostic = diagnostics.get(i);
            if (i > 0)
                sb.append(',');
            sb.append("{\"sev\":").append(quote(diagnostic.severity().code()))
                .append(",\"kind\":").append(quote(diagnostic.kind().code()))
                .append(",\"path\":").append(quote(diagnostic.source().configPath()))
                .append(",\"msg\":").append(quote(diagnostic.message()));
            if (diagnostic.hint() != null)
                sb.append(",\"hint\":").append(quote(diagnostic.hint()));
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20)
                        sb.append(String.format("\\u%04x", (int) c));
                    else
                        sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
