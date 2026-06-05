package me.deecaad.core.file.verify;

import org.jetbrains.annotations.NotNull;

/**
 * Declares that a key is only "active" when a sibling key holds an expected value. Lets the
 * verifier know a key is valid regardless of the branch taken, and lets it flag keys that are set
 * but have no effect ({@link me.deecaad.core.diagnostic.DiagnosticKind#INACTIVE_KEY}).
 *
 * @param siblingKey The sibling key this depends on.
 * @param expected The value {@code siblingKey} must equal for this key to be active.
 */
public record Condition(@NotNull String siblingKey, @NotNull Object expected) {

    public static Condition activeWhen(@NotNull String siblingKey, @NotNull Object expected) {
        return new Condition(siblingKey, expected);
    }
}
