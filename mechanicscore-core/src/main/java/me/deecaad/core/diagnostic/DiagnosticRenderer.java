package me.deecaad.core.diagnostic;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * The single renderer for every config/mechanic error: turns a {@link Diagnostic} into a
 * compiler-style gutter snippet and logs it via the plugin Debugger. Diagnostics with no source
 * position ({@link Span#NONE} or an empty raw line) render as a header + location only, with no caret.
 */
public final class DiagnosticRenderer {

    private DiagnosticRenderer() {
    }

    /**
     * Renders into a compiler-style gutter snippet:
     * <pre>
     * Error: Unknown key 'Hello'
     *   AK_47.yml  at AK_47.Info.Weapon_Item.Hello
     *    42 |   Hello: 7
     *       |   ^^^^^  did you mean 'Lore'?
     * </pre>
     * Diagnostics with no source position render as header + location only. The hint is shown inline
     * after the caret when there is one, otherwise on its own line.
     */
    public static @NotNull List<String> render(@NotNull Diagnostic diagnostic) {
        List<String> lines = new ArrayList<>();
        lines.add(prefix(diagnostic.severity()) + diagnostic.message());
        lines.add(location(diagnostic.source()));

        boolean hasCaret = diagnostic.primary().line() >= 0 && !diagnostic.source().rawLine().isEmpty();
        if (hasCaret) {
            int errorLine = diagnostic.primary().line() + 1;
            List<String> context = diagnostic.source().contextBefore();
            int width = Integer.toString(errorLine).length();

            // Context lines above the error, numbered up to the error line.
            int firstContextLine = errorLine - context.size();
            for (int i = 0; i < context.size(); i++)
                lines.add(gutter(firstContextLine + i, width) + context.get(i));

            lines.add(gutter(errorLine, width) + diagnostic.source().rawLine());

            String caretLine = blankGutter(width) + caret(diagnostic.primary());
            if (diagnostic.hint() != null && !diagnostic.hint().isEmpty())
                caretLine += "  " + diagnostic.hint();
            lines.add(caretLine);

            for (Span secondary : diagnostic.secondary())
                lines.add(blankGutter(width) + caret(secondary));
        } else if (diagnostic.hint() != null && !diagnostic.hint().isEmpty()) {
            lines.add("  hint: " + diagnostic.hint());
        }
        return lines;
    }

    private static @NotNull String gutter(int lineNumber, int width) {
        String number = Integer.toString(lineNumber);
        return "  " + StringUtil.repeat(" ", width - number.length()) + number + " | ";
    }

    private static @NotNull String blankGutter(int width) {
        return "  " + StringUtil.repeat(" ", width) + " | ";
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
        String at = source.file() == null
            ? "  at " + source.configPath()
            : "  " + source.file().getName() + "  at " + source.configPath();
        if (source.listIndex() >= 0)
            at += " (" + StringUtil.ordinal(source.listIndex() + 1) + " list item)";
        return at;
    }

    private static @NotNull String caret(@NotNull Span span) {
        return StringUtil.repeat(" ", span.start()) + StringUtil.repeat("^", span.width());
    }
}
