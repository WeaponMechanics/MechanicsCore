package me.deecaad.core.mechanics.defaultmechanics;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class MythicSkillMechanic extends Mechanic {

    private String skillName;
    private float power;

    public MythicSkillMechanic() {
    }

    public MythicSkillMechanic(String skillName, float power) {
        this.skillName = skillName;
        this.power = power;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "mythicskill");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/integrations/mythicmobs#mythic-skill-mechanic";
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null)
            return;

        LivingEntity source = scope.sourceEntity();
        Collection<Entity> eTargets = new ArrayList<>();
        Collection<Location> lTargets = new ArrayList<>();
        if (subject.entity() != null)
            eTargets.add(subject.entity());
        else
            lTargets.add(subject.location());

        Location origin = source != null ? source.getLocation() : subject.location();
        MythicBukkit.inst().getAPIHelper().castSkill(source, skillName, origin, eTargets, lTargets, power);
    }

    @Override
    public @NotNull Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        String skill = data.of("Skill").assertExists().get(String.class).get();
        float power = (float) data.of("Power").getDouble().orElse(1.0);
        return applyParentArgs(data, new MythicSkillMechanic(skill, power));
    }
}
