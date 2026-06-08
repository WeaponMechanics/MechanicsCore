package me.deecaad.core.diagnostic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Identifies the config origin of a diagnostic: the file, the config path (e.g.
 * {@code Mechanics} or {@code Item.Durability.Max_Damage}), the list-item index,
 * the raw rendered line text used for caret diagnostics, and the source lines
 * immediately preceding it (oldest-first) shown as context above the error.
 */
public record SourceRef(@Nullable File file, @NotNull String configPath, int listIndex, @NotNull String rawLine,
                        @NotNull List<String> contextBefore) {

    /**
     * Backward-compatible constructor for sources with no preceding context.
     */
    public SourceRef(@Nullable File file, @NotNull String configPath, int listIndex, @NotNull String rawLine) {
        this(file, configPath, listIndex, rawLine, List.of());
    }

    /**
     * A file-context source with no line/column info (no raw line, not a list item).
     * Used by section validation where Bukkit YAML exposes no positions.
     */
    public static @NotNull SourceRef ofConfig(@Nullable File file, @NotNull String configPath) {
        return new SourceRef(file, configPath, -1, "", List.of());
    }
}
