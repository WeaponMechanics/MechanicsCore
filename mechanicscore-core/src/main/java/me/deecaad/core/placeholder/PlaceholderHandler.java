package me.deecaad.core.placeholder;

import org.bukkit.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A PlaceholderHandler is a variable in a string. Instances of this class return the current value
 * of the variable, which can then be used in the string.
 */
public abstract class PlaceholderHandler implements Keyed {

    public PlaceholderHandler() {
    }

    /**
     * Returns the value for this placeholder, given the <code>data</code>.
     *
     * @param data The data used to generate placeholders from
     * @return the result for placeholder or null
     */
    public abstract @Nullable String onRequest(@NotNull PlaceholderData data);

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PlaceholderHandler that = (PlaceholderHandler) o;
        return getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
