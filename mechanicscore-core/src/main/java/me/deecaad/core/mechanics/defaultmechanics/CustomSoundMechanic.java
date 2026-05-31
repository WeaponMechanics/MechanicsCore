package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.Targeters;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.mechanics.targeters.WorldTargeter;
import me.deecaad.core.utils.RandomUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomSoundMechanic extends Mechanic {

    private String sound;
    private float volume;
    private float pitch;
    private float noise;
    private SoundCategory category;
    private Targeter listeners;
    private List<Condition> listenerConditions;

    public CustomSoundMechanic() {
    }

    public CustomSoundMechanic(String sound, float volume, float pitch, float noise, SoundCategory category, Targeter listeners, List<Condition> listenerConditions) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.noise = noise;
        this.category = category;
        this.listeners = listeners;
        this.listenerConditions = listenerConditions;
    }

    public String getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public float getNoise() {
        return noise;
    }

    public Object getCategory() {
        return category;
    }

    public Targeter getListeners() {
        return listeners;
    }

    public List<Condition> getListenerConditions() {
        return listenerConditions;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null)
            return;
        Location loc = subject.location();
        float pitch = this.pitch + RandomUtil.range(-noise, noise);

        if (listeners == null) {
            if (loc.getWorld() != null)
                loc.getWorld().playSound(loc, sound, category, volume, pitch);
            return;
        }

        Context listenerContext = listeners.target(scope);
        OUTER : for (Target listener : listenerContext) {
            if (!(listener.entity() instanceof Player player))
                continue;
            for (Condition condition : listenerConditions)
                if (!condition.isAllowed(scope, listener))
                    continue OUTER;
            player.playSound(loc, sound, category, volume, pitch);
        }
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "customsound");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/custom-sound";
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        String sound = data.of("Sound").assertExists().get(String.class).get();
        float volume = (float) data.of("Volume").assertRange(0, null).getDouble().orElse(1.0);
        float pitch = (float) data.of("Pitch").assertRange(0.5, 2.0).getDouble().orElse(1.0);
        float noise = (float) data.of("Noise").assertRange(0.0, 1.5).getDouble().orElse(0.0);
        SoundCategory category = data.of("Category").getEnum(SoundCategory.class).orElse(SoundCategory.PLAYERS);

        Targeter listeners = data.of("Listeners").serializeRegistry(Targeters.REGISTRY).orElse(null);
        List<Condition> listenerConditions = data.of("Listener_Conditions").getRegistryList(Conditions.REGISTRY);
        if (!listenerConditions.isEmpty() && listeners == null)
            listeners = new WorldTargeter();

        return applyParentArgs(data, new CustomSoundMechanic(sound, volume, pitch, noise, category, listeners, listenerConditions));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }

    @Override
    public boolean isBatchablePlayerEffect() {
        return true;
    }
}
