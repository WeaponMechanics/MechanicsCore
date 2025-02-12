package me.deecaad.core.utils;

import org.bukkit.Keyed;

/**
 * Thrown when someone tries to add a duplicate key to a {@link MutableRegistry}.
 */
public class DuplicateRegistryValueException extends RuntimeException {
    public DuplicateRegistryValueException(Keyed entry) {
        super("Duplicate registry value: " + entry.getKey());
    }
}
