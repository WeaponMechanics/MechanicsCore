package me.deecaad.core.mechanics.diagnostic;

import me.deecaad.core.mechanics.ast.Loc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates {@link Diagnostic}s across a compile. Front-end stages record and
 * continue rather than throwing, so a single compile reports every problem.
 */
public final class DiagnosticReporter {

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public void error(@NotNull Loc loc, @NotNull String message) {
        error(loc, message, null);
    }

    public void error(@NotNull Loc loc, @NotNull String message, @Nullable String hint) {
        diagnostics.add(new Diagnostic(Severity.ERROR, message, loc.source(), loc.span(), List.of(), hint));
    }

    public void warning(@NotNull Loc loc, @NotNull String message) {
        warning(loc, message, null);
    }

    public void warning(@NotNull Loc loc, @NotNull String message, @Nullable String hint) {
        diagnostics.add(new Diagnostic(Severity.WARNING, message, loc.source(), loc.span(), List.of(), hint));
    }

    public void report(@NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public boolean hasErrors() {
        for (Diagnostic d : diagnostics)
            if (d.severity() == Severity.ERROR)
                return true;
        return false;
    }

    public int errorCount() {
        int count = 0;
        for (Diagnostic d : diagnostics)
            if (d.severity() == Severity.ERROR)
                count++;
        return count;
    }

    public boolean isEmpty() {
        return diagnostics.isEmpty();
    }

    public @NotNull List<Diagnostic> all() {
        return Collections.unmodifiableList(diagnostics);
    }
}
