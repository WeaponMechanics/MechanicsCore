package me.deecaad.core.extensions;

import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.conditions.GeyserCondition;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class GeyserExtension implements Extension {

    @Override
    public void register(@NotNull Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (!pm.isPluginEnabled("Geyser-Spigot")) {
            return;
        }

        Conditions.REGISTRY.add(new GeyserCondition());
    }
}
