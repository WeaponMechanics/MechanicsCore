package me.deecaad.core.diagnostic;

import org.jetbrains.annotations.NotNull;

/**
 * A column range within a single rendered config line. {@code start} and
 * {@code end} are 0-based column offsets into {@link SourceRef#rawLine()}.
 *
 * <p>{@link #NONE} is the sentinel for diagnostics with no line/column context
 * (e.g. file-section validation, where Bukkit YAML exposes no positions). The
 * renderer skips caret rendering when {@code line < 0}.
 */
public record Span(int line, int start, int end) {

    public static final Span NONE = new Span(-1, 0, 0);

    public static @NotNull Span of(int line, int start, int end) {
        return new Span(line, start, Math.max(start, end));
    }

    /**
     * A span covering from the start of this span to the end of {@code other}.
     */
    public @NotNull Span to(@NotNull Span other) {
        return new Span(line, start, other.end);
    }

    public int width() {
        return Math.max(1, end - start);
    }
}
