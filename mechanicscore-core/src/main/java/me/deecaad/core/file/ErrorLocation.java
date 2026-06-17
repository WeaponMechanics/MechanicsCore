package me.deecaad.core.file;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * A structured pointer to where a config mistake occurred: the file, the dotted config path, and an
 * optional 0-based list index ({@code -1} when not a list item). Replaces the old pre-rendered
 * location string so a {@link SerializerException} can be enriched into a precise caret diagnostic.
 */
public record ErrorLocation(@Nullable File file, @Nullable String path, int index) {

    public static final ErrorLocation UNKNOWN = new ErrorLocation(null, null, -1);

    public ErrorLocation(@Nullable File file, @Nullable String path) {
        this(file, path, -1);
    }
}
