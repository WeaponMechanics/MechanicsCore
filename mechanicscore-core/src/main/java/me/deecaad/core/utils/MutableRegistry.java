package me.deecaad.core.utils;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A mutable {@link Registry} that allows adding and overwriting entries.
 *
 * @param <T> the type of the entries in the registry
 */
public interface MutableRegistry<T extends Keyed> extends Registry<T> {

    /**
     * Tries to add the given entry to the registry. If the entry already exists,
     * a {@link DuplicateRegistryValueException} is thrown.
     *
     * @param entry the entry to add
     * @throws DuplicateRegistryValueException if the entry already exists
     */
    void add(@NotNull T entry) throws DuplicateRegistryValueException;

    /**
     * Adds (or, if it already exists, overwrites) the given entry to the registry.
     *
     * @param entry the entry to add
     */
    void overwrite(@NotNull T entry);


    /**
     * A simple implementation of {@link MutableRegistry}, backed by a hash map.
     *
     * @param <T> the type of the entries in the registry
     */
    final class SimpleMutableRegistry<T extends Keyed> implements MutableRegistry<T> {

        private final @NotNull Map<NamespacedKey, T> entries;

        public SimpleMutableRegistry(@NotNull Map<NamespacedKey, T> entries) {
            this.entries = new LinkedHashMap<>(entries);
        }

        @Override
        public @Nullable T get(@NotNull NamespacedKey namespacedKey) {
            return entries.get(namespacedKey);
        }

        @Override
        public @NotNull T getOrThrow(@NotNull NamespacedKey namespacedKey) {
            T entry = get(namespacedKey);
            if (entry == null) {
                throw new IllegalArgumentException("No entry found for key: " + namespacedKey);
            }
            return entry;
        }

        @Override
        public @NotNull Stream<T> stream() {
            return entries.values().stream();
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return entries.values().iterator();
        }

        @Override
        public void add(@NotNull T entry) throws DuplicateRegistryValueException {
            if (entries.containsKey(entry.getKey())) {
                throw new DuplicateRegistryValueException(entry);
            }
            entries.put(entry.getKey(), entry);
        }

        @Override
        public void overwrite(@NotNull T entry) {
            entries.put(entry.getKey(), entry);
        }
    }
}
