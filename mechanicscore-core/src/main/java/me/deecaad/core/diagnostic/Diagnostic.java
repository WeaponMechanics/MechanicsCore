package me.deecaad.core.diagnostic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single finding produced while loading or verifying a config, with a precise
 * source location when available. Multiple diagnostics are collected per pass so
 * the user (or harness) sees every problem at once.
 *
 * <p>Inline mechanic diagnostics carry a real {@link Span} (column caret).
 * File-section diagnostics have no positions, so they use {@link Span#NONE} and a
 * {@link SourceRef#ofConfig} source; the renderer degrades to a path-only message.
 */
public record Diagnostic(@NotNull Severity severity, @NotNull DiagnosticKind kind, @NotNull String message,
                         @NotNull SourceRef source, @NotNull Span primary, @NotNull List<Span> secondary,
                         @Nullable String hint) {

    /**
     * Builds a file-context diagnostic (no line/column). {@code source} should be a
     * {@link SourceRef#ofConfig} whose {@code configPath} is the dotted config path.
     */
    public static @NotNull Diagnostic at(@NotNull Severity severity, @NotNull DiagnosticKind kind,
                                         @NotNull SourceRef source, @NotNull String message, @Nullable String hint) {
        return new Diagnostic(severity, kind, message, source, Span.NONE, List.of(), hint);
    }
}
