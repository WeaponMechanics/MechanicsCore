package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;

/**
 * A column range within a single rendered config line. {@code start} and
 * {@code end} are 0-based column offsets into {@link SourceRef#rawLine()}.
 */
public record Span(int line, int start, int end) {

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
