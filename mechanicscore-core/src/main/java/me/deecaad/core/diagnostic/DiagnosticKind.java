package me.deecaad.core.diagnostic;

/**
 * Categorizes a {@link Diagnostic} for machine consumption (e.g. an LLM config
 * harness). The {@link #code()} strings are stable and short for compact JSON.
 */
public enum DiagnosticKind {
    UNKNOWN_KEY("unknown"),
    INVALID_TYPE("type"),
    OUT_OF_RANGE("range"),
    MISSING_REQUIRED("missing"),
    INVALID_VALUE("value"),
    INACTIVE_KEY("inactive"),
    OTHER("other");

    private final String code;

    DiagnosticKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
