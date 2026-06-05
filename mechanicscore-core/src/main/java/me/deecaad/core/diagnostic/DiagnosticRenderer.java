package me.deecaad.core.diagnostic;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Renders a {@link Diagnostic} into the same "located at ... ^^^" pretty style
 * used by {@code SerializerException}/{@code MapConfigLike}, and logs it via the
 * plugin Debugger. Diagnostics with no source position ({@link Span#NONE} or an
 * empty raw line) render as a header + location only, with no caret.
 */
public final class DiagnosticRenderer {

    private static final String INDENT = "    ";

    private DiagnosticRenderer() {
    }

    public static @NotNull List<String> render(@NotNull Diagnostic diagnostic) {
        List<String> lines = new ArrayList<>();
        lines.add(prefix(diagnostic.severity()) + diagnostic.message());
        lines.add(location(diagnostic.source()));

        boolean hasCaret = diagnostic.primary().line() >= 0 && !diagnostic.source().rawLine().isEmpty();
        if (hasCaret) {
            lines.add(INDENT + diagnostic.source().rawLine());
            lines.add(caret(diagnostic.primary()));
            for (Span secondary : diagnostic.secondary())
                lines.add(caret(secondary));
        }

        if (diagnostic.hint() != null)
            lines.add("Hint: " + diagnostic.hint());
        return lines;
    }

    public static void log(@NotNull MechanicsLogger debug, @NotNull Diagnostic diagnostic) {
        Level level = switch (diagnostic.severity()) {
            case ERROR -> Level.SEVERE;
            case WARNING -> Level.WARNING;
            case INFO -> Level.INFO;
        };
        List<String> lines = render(diagnostic);
        debug.log(level, lines.toArray(new String[0]));
    }

    private static @NotNull String prefix(@NotNull Severity severity) {
        return switch (severity) {
            case ERROR -> "Error: ";
            case WARNING -> "Warning: ";
            case INFO -> "Info: ";
        };
    }

    private static @NotNull String location(@NotNull SourceRef source) {
        String at = "Located in file '" + source.file() + "' at '" + source.configPath() + "'";
        if (source.listIndex() >= 0)
            at += " (the " + StringUtil.ordinal(source.listIndex() + 1) + " list item)";
        return at;
    }

    private static @NotNull String caret(@NotNull Span span) {
        return StringUtil.repeat(" ", INDENT.length() + span.start()) + StringUtil.repeat("^", span.width());
    }
}
