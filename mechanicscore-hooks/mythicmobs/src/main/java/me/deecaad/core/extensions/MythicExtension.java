package me.deecaad.core.extensions;

import me.deecaad.core.extension.Extension;
import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.mechanics.conditions.MythicMobsEntityCondition;
import me.deecaad.core.mechanics.conditions.MythicMobsFactionCondition;
import me.deecaad.core.mechanics.defaultmechanics.MythicSkillMechanic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class MythicExtension implements Extension {
    @Override
    public void register(@NotNull Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (!pm.isPluginEnabled("MythicMobs")) {
            return;
        }

        Conditions.REGISTRY.add(new MythicMobsEntityCondition());
        Conditions.REGISTRY.add(new MythicMobsFactionCondition());

        Mechanics.REGISTRY.add(new MythicSkillMechanic());
    }
}
