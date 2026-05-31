package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageMechanic extends Mechanic {

    public static final String METADATA_KEY = "mechanicscore-damagemechanic";

    private double damage;
    private boolean resetHitCooldown;
    private boolean requiresEvent;
    private boolean ignoreArmor;

    public DamageMechanic() {
    }

    public DamageMechanic(double damage, boolean ignoreArmor, boolean resetHitCooldown) {
        this.damage = damage;
        this.resetHitCooldown = resetHitCooldown;
        this.requiresEvent = ignoreArmor;
        this.ignoreArmor = ignoreArmor;
    }

    public double getDamage() {
        return damage;
    }

    public boolean isRequireEvent() {
        return requiresEvent;
    }

    public boolean isIgnoreArmor() {
        return ignoreArmor;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "damage");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/damage";
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || subject.entity() == null)
            return;

        LivingEntity target = subject.entity();
        if (requiresEvent)
            target.setMetadata(METADATA_KEY, new FixedMetadataValue(MechanicsCore.getInstance(), this));

        target.damage(damage);
        if (resetHitCooldown)
            target.setNoDamageTicks(0);
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        double damage = data.of("Damage").getDouble().orElse(1.0);
        boolean ignoreArmor = data.of("Ignore_Armor").getBool().orElse(false);
        boolean resetHitCooldown = data.of("Reset_Cooldown").getBool().orElse(false);
        return applyParentArgs(data, new DamageMechanic(damage, ignoreArmor, resetHitCooldown));
    }
}
