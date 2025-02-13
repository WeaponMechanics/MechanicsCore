package me.deecaad.core.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class EnumPlaceholderHandler extends PlaceholderHandler {

    public EnumPlaceholderHandler() {
    }

    public abstract @NotNull List<String> getOptions();
}
