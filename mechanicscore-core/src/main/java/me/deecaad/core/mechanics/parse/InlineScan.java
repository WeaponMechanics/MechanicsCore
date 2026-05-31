package me.deecaad.core.mechanics.parse;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.mechanics.ast.InlineCallNode;
import me.deecaad.core.mechanics.ast.Loc;
import me.deecaad.core.mechanics.ast.SourceRef;
import me.deecaad.core.mechanics.ast.Span;
import me.deecaad.core.mechanics.diagnostic.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Bridges the existing {@code InlineSerializer.inlineFormat} tokenizer into the
 * AST: parses a {@code Name{key=value}} string and maps the per-arg
 * {@code Holder.index()} back to rawLine columns so spans are precise. The arg
 * holders are kept verbatim so sema can rebuild a {@code SerializeData}.
 */
public final class InlineScan {

    private InlineScan() {
    }

    /**
     * @param innerRaw  the {@code Name{...}} text (may have leading/trailing spaces)
     * @param innerColRaw the column of {@code innerRaw}'s first char in the rawLine
     */
    public static @Nullable InlineCallNode scan(@NotNull String innerRaw, int innerColRaw, int line,
                                                @NotNull SourceRef source, @NotNull DiagnosticReporter reporter) {
        int leading = leadingSpaces(innerRaw);
        String inner = innerRaw.strip();
        int innerCol = innerColRaw + leading;

        Map<String, MapConfigLike.Holder> args;
        try {
            args = InlineSerializer.inlineFormat(inner);
        } catch (InlineSerializer.FormatException ex) {
            int col = innerCol + ex.getIndex();
            reporter.error(new Loc(source, Span.of(line, col, col + 1)),
                ex.getMessage() == null ? "Malformed inline syntax" : ex.getMessage());
            return null;
        }

        MapConfigLike.Holder nameHolder = args.get(InlineSerializer.UNIQUE_IDENTIFIER);
        String name = nameHolder != null ? String.valueOf(nameHolder.value()) : inner;
        Loc nameLoc = new Loc(source, Span.of(line, innerCol, innerCol + name.length()));
        Loc loc = new Loc(source, Span.of(line, innerCol, innerCol + inner.length()));
        return new InlineCallNode(name, nameLoc, args, loc);
    }

    /**
     * The rawLine column of an arg value, derived from the call's start column
     * plus the holder's index within the inline string.
     */
    public static int argColumn(@NotNull InlineCallNode call, @NotNull MapConfigLike.Holder holder) {
        return call.loc().span().start() + holder.index();
    }

    private static int leadingSpaces(@NotNull String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ')
            i++;
        return i;
    }
}
