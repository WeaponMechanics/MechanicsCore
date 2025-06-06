package me.deecaad.core.utils

import org.bukkit.Keyed
import org.bukkit.Registry

/**
 * Utility functions for working with the Bukkit [Registry].
 */
object RegistryUtil {

    @JvmStatic
    fun matches(key: String, value: Keyed): Boolean {
        if (key.isEmpty()) throw IllegalArgumentException("Key cannot be empty")

        // Some keys may contain a namespace, so we should enforce that namespace
        var namespace: String? = null
        var keyToCheck = key
        if (key.contains(":")) {
            namespace = key.split(":")[0].lowercase()
            keyToCheck = key.split(":")[1].lowercase()
        }

        if (namespace != null && value.key.namespace != namespace) return false
        return value.key.key == keyToCheck
    }

    /**
     * Loops through the registry and returns the first registry value whose
     * key matches the given string.
     */
    @JvmStatic
    fun <T : Keyed> matchAny(registry: Registry<T>, key: String): T? {
        if (key.isEmpty()) return null

        // Some keys may contain a namespace, so we should enforce that namespace
        var namespace: String? = null
        var keyToCheck = key.lowercase()
        if (key.contains(":")) {
            namespace = key.split(":")[0]
            keyToCheck = key.split(":")[1].lowercase()
        }

        for (value in registry) {
            if (namespace != null && value.key.namespace != namespace) continue
            if (value.key.key == keyToCheck) return value
        }

        return null
    }
}