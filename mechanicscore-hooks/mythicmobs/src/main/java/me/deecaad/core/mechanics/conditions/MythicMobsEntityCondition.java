package me.deecaad.core.mechanics.conditions;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MythicMobsEntityCondition extends Condition {

    private String name;

    public MythicMobsEntityCondition() {
    }

    public MythicMobsEntityCondition(String name) {
        this.name = name;
    }

    @Override
    public boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        if (subject == null || subject.entity() == null)
            return false;

        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(subject.entity().getUniqueId()).orElse(null);
        return mythicMob != null && mythicMob.getName().equals(name);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "mythicmobs_entity");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/integrations/mythicmobs#mythic-mobs-entity-condition";
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        String type = data.of("Entity").assertExists().get(String.class).get();
        return applyParentArgs(data, new MythicMobsEntityCondition(type));
    }
}
