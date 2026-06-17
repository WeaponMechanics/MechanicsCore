package me.deecaad.core.utils

import org.bukkit.Keyed
import org.bukkit.Registry

/**
 * Utility functions for working with the Bukkit [Registry].
 */
object RegistryUtil {

    // Keys are matched ignoring case and word separators, so 'EntityType', 'Entity_Type',
    // and 'entity_type' all resolve to the registered key 'entity_type'.
    private fun normalize(key: String): String = key.lowercase().replace("_", "")

    @JvmStatic
    fun matches(key: String, value: Keyed): Boolean {
        if (key.isEmpty()) throw IllegalArgumentException("Key cannot be empty")

        var namespace: String? = null
        var keyToCheck = key
        if (key.contains(":")) {
            namespace = key.split(":")[0].lowercase()
            keyToCheck = key.split(":")[1]
        }

        if (namespace != null && value.key.namespace != namespace) return false
        return normalize(value.key.key) == normalize(keyToCheck)
    }

    /**
     * Loops through the registry and returns the first registry value whose
     * key matches the given string.
     */
    @JvmStatic
    fun <T : Keyed> matchAny(registry: Registry<T>, key: String): T? {
        if (key.isEmpty()) return null

        var namespace: String? = null
        var keyToCheck = key
        if (key.contains(":")) {
            namespace = key.split(":")[0]
            keyToCheck = key.split(":")[1]
        }

        val normalized = normalize(keyToCheck)
        for (value in registry) {
            if (namespace != null && value.key.namespace != namespace) continue
            if (normalize(value.key.key) == normalized) return value
        }

        return null
    }
}