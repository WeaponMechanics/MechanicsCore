package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Identifies the config origin of a parsed line: the file, the config path (e.g.
 * {@code Mechanics}), the list-item index, and the raw rendered line text used
 * for caret diagnostics.
 */
public record SourceRef(@Nullable File file, @NotNull String configPath, int listIndex, @NotNull String rawLine) {
}
