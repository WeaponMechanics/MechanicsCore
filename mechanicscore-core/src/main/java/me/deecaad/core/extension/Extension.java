package me.deecaad.core.extension;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Used by external jars to register mechanics, targeters, and conditions.
 */
public interface Extension {

    /**
     * Registers this addon's mechanics, targeters, and conditions.
     *
     * @param plugin the plugin that is registering everything
     */
    void register(@NotNull Plugin plugin);
}
