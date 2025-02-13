package me.deecaad.core.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NumericPlaceholderHandler extends PlaceholderHandler {

    public NumericPlaceholderHandler() {
    }

    @Override
    public final @Nullable String onRequest(@NotNull PlaceholderData data) {
        Number value = requestValue(data);
        return value == null ? null : value.toString();
    }

    public abstract @Nullable Number requestValue(@NotNull PlaceholderData data);
}
