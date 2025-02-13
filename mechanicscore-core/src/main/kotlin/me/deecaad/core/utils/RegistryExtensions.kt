package me.deecaad.core.utils

import org.bukkit.Keyed
import org.bukkit.Registry


inline fun <T : Keyed> Registry<T>.matchAny(key: String): T? {
    return RegistryUtil.matchAny(this, key)
}