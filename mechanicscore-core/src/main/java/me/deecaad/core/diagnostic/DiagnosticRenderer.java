package me.deecaad.core.diagnostic;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * The single renderer for every config/mechanic error: turns a {@link Diagnostic} into a
 * compiler-style gutter snippet and logs it as one block via the plugin Debugger. The whole snippet
 * is one log call so the {@code [time LEVEL]: [Plugin]} prefix shows once, not per line.
 *
 * <pre>
 * unknown key 'Hi'
 *     AK_47.yml:5:7 (AK_47->Info->Weapon_Item->Hi)
 *   3 |     Weapon_Item:
 *   4 |       Type: "FEATHER"
 *   5 |       Hi: "FEATHER"
 *     |       ^^ did you mean 'Type'?
 * </pre>
 *
 * Severity is conveyed by the log level and ANSI color, so the message carries no "Error:"/"Warning:"
 * prefix. Color is emitted for the console; the server's log file appender strips the codes. Diagnostics
 * with no source position render as a bare message; those with a file/path but no caret add the location
 * line only.
 */
public final class DiagnosticRenderer {

    /**
     * Whether to emit ANSI color. The console renders it; the platform's file appender strips it. Flip
     * off for environments with no color-aware console.
     */
    public static boolean colorEnabled = true;

    private static final String RESET = "\u001b[0m";
    private static final String BOLD = "\u001b[1m";
    private static final String DIM = "\u001b[90m";
    private static final String RED = "\u001b[31m";
    private static final String YELLOW = "\u001b[33m";
    private static final String CYAN = "\u001b[36m";

    private DiagnosticRenderer() {
    }

    /**
     * Renders the snippet as plain text (no color). Used by tests and any caller that wants raw lines.
     */
    public static @NotNull List<String> render(@NotNull Diagnostic diagnostic) {
        return render(diagnostic, false);
    }

    public static @NotNull List<String> render(@NotNull Diagnostic diagnostic, boolean color) {
        List<String> lines = new ArrayList<>();
        String sev = severityCode(diagnostic.severity());

        lines.add(wrap(color, sev, diagnostic.message()));

        SourceRef source = diagnostic.source();
        boolean hasCaret = diagnostic.primary().line() >= 0 && !source.rawLine().isEmpty();
        boolean hasLocation = source.file() != null || !source.configPath().isBlank();
        if (hasLocation)
            lines.add(locationLine(diagnostic, color));

        if (hasCaret) {
            int errorLine = diagnostic.primary().line() + 1;
            List<String> context = source.contextBefore();
            int width = Integer.toString(errorLine).length();

            int firstContextLine = errorLine - context.size();
            for (int i = 0; i < context.size(); i++)
                lines.add(gutter(firstContextLine + i, width, color) + wrap(color, DIM, context.get(i)));

            lines.add(gutter(errorLine, width, color) + source.rawLine());

            String caretLine = blankGutter(width, color) + wrap(color, sev, caret(diagnostic.primary()));
            if (diagnostic.hint() != null && !diagnostic.hint().isEmpty())
                caretLine += "  " + wrap(color, CYAN, diagnostic.hint());
            lines.add(caretLine);

            for (Span secondary : diagnostic.secondary())
                lines.add(blankGutter(width, color) + wrap(color, sev, caret(secondary)));
        } else if (diagnostic.hint() != null && !diagnostic.hint().isEmpty()) {
            lines.add("  hint: " + wrap(color, CYAN, diagnostic.hint()));
        }
        return lines;
    }

    public static void log(@NotNull MechanicsLogger debug, @NotNull Diagnostic diagnostic) {
        Level level = switch (diagnostic.severity()) {
            case ERROR -> Level.SEVERE;
            case WARNING -> Level.WARNING;
            case INFO -> Level.INFO;
        };
        List<String> lines = render(diagnostic, colorEnabled);
        debug.log(level, String.join("\n", lines));
    }

    private static @NotNull String locationLine(@NotNull Diagnostic diagnostic, boolean color) {
        SourceRef source = diagnostic.source();
        String fileName = source.file() == null ? null : source.file().getName();
        boolean caret = diagnostic.primary().line() >= 0;
        String coord = fileName == null ? null
            : caret ? fileName + ":" + (diagnostic.primary().line() + 1) + ":" + (diagnostic.primary().start() + 1)
            : fileName;

        StringBuilder sb = new StringBuilder("    ");
        if (coord != null)
            sb.append(wrap(color, BOLD, coord));

        String crumb = breadcrumb(source.configPath());
        if (!crumb.isEmpty()) {
            if (coord != null)
                sb.append(' ');
            sb.append(wrap(color, DIM, "(" + crumb + ")"));
        }
        if (source.listIndex() >= 0)
            sb.append(' ').append(wrap(color, DIM, "[" + StringUtil.ordinal(source.listIndex() + 1) + " list item]"));
        return sb.toString();
    }

    private static @NotNull String breadcrumb(@NotNull String configPath) {
        return configPath.isEmpty() ? "" : configPath.replace(".", "->");
    }

    private static @NotNull String gutter(int lineNumber, int width, boolean color) {
        String number = Integer.toString(lineNumber);
        return wrap(color, DIM, "  " + StringUtil.repeat(" ", width - number.length()) + number + " | ");
    }

    private static @NotNull String blankGutter(int width, boolean color) {
        return wrap(color, DIM, "  " + StringUtil.repeat(" ", width) + " | ");
    }

    private static @NotNull String caret(@NotNull Span span) {
        return StringUtil.repeat(" ", span.start()) + StringUtil.repeat("^", span.width());
    }

    private static @NotNull String severityCode(@NotNull Severity severity) {
        return switch (severity) {
            case ERROR -> RED;
            case WARNING -> YELLOW;
            case INFO -> "";
        };
    }

    private static @NotNull String wrap(boolean color, @NotNull String code, @NotNull String text) {
        return !color || code.isEmpty() ? text : code + text + RESET;
    }
}
