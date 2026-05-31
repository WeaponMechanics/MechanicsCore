package me.deecaad.core.mechanics.diagnostic;

import me.deecaad.core.mechanics.ast.SourceRef;
import me.deecaad.core.mechanics.ast.Span;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single compile-time message with a precise source location. Multiple
 * diagnostics are collected per compile so the user sees every problem at once.
 */
public record Diagnostic(@NotNull Severity severity, @NotNull String message, @NotNull SourceRef source,
                         @NotNull Span primary, @NotNull List<Span> secondary, @Nullable String hint) {
}
