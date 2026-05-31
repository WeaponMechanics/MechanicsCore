package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PotionMechanic extends Mechanic {

    private PotionEffect potion;

    public PotionMechanic() {
    }

    public PotionMechanic(PotionEffect potion) {
        this.potion = potion;
    }

    public PotionEffect getPotion() {
        return potion;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || subject.entity() == null)
            return;
        subject.entity().addPotionEffect(potion);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "potion");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/potion";
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        RegistryValueSerializer<PotionEffectType> potionSerializer = new RegistryValueSerializer<>(PotionEffectType.class, true);
        PotionEffectType potion = data.of("Potion").assertExists().serialize(potionSerializer).get().getFirst();
        int time = data.of("Time").assertRange(0, null).getInt().orElse(100);
        int amplifier = data.of("Level").assertRange(0, null).getInt().orElse(1) - 1;
        ParticleMode particleMode = data.of("Particles").getEnum(ParticleMode.class).orElse(ParticleMode.NORMAL);
        boolean ambient = particleMode == ParticleMode.AMBIENT;
        boolean showParticles = particleMode != ParticleMode.HIDE;
        boolean showIcon = !data.of("Hide_Icon").getBool().orElse(false);

        PotionEffect effect = new PotionEffect(potion, time, amplifier, ambient, showParticles, showIcon);
        return applyParentArgs(data, new PotionMechanic(effect));
    }

    public enum ParticleMode {
        HIDE,
        NORMAL,
        AMBIENT
    }
}
