package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.PlayerEffectMechanic;
import me.deecaad.core.mechanics.Targeters;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.mechanics.targeters.WorldTargeter;
import me.deecaad.core.utils.RandomUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class SoundMechanic extends PlayerEffectMechanic {

    private Sound sound;
    private float volume;
    private float pitch;
    private float noise;
    private SoundCategory category;
    private Targeter listeners;
    private List<Condition> listenerConditions;

    /**
     * Default constructor for serializer.
     */
    public SoundMechanic() {
    }

    public SoundMechanic(Sound sound, float volume, float pitch, float noise, SoundCategory category, Targeter listeners, List<Condition> listenerConditions) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.noise = noise;
        this.category = category;
        this.listeners = listeners;
        this.listenerConditions = listenerConditions;
    }

    public Sound getSound() {
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
    protected void use0(CastData cast) {
        if (listeners == null) {
            Location loc = cast.getTargetLocation();

            loc.getWorld().playSound(loc, sound, category, volume, pitch + RandomUtil.range(-noise, noise));
            return;
        }

        // Imagine an explosion. It has a target location. So the distance between
        // the source (the player who caused the explosion) and the target (the
        // explosion) will be constant. In reality, for listenerConditions, we
        // want the target to be the listener. So we must strip away the target location
        CastData center = cast;
        if (cast.hasTargetLocation()) {
            center = center.clone();
            center.setTargetLocation((Supplier<Location>) null);
        }

        // Cache to avoid overhead
        Location targetLocation = cast.getTargetLocation();
        float pitch = this.pitch + RandomUtil.range(-noise, noise);

        // When listeners != null, only targeted Players will be able to hear
        // this sound. In this case, we have to loop through every player and
        // manually play the sound packet for them.
        OUTER : for (Iterator<CastData> it = listeners.getTargets(center); it.hasNext();) {
            CastData target = it.next();
            if (!(target.getTarget() instanceof Player player))
                continue;

            for (Condition condition : listenerConditions)
                if (!condition.isAllowed(target))
                    continue OUTER;

            player.playSound(targetLocation, sound, category, volume, pitch);
        }
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "sound");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/sound";
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        Sound sound = data.of("Sound").assertExists().getBukkitRegistry(Sound.class).get();
        float volume = (float) data.of("Volume").assertRange(0, null).getDouble().orElse(1.0);
        float pitch = (float) data.of("Pitch").assertRange(0.5, 2.0).getDouble().orElse(1.0);
        float noise = (float) data.of("Noise").assertRange(0.0, 1.5).getDouble().orElse(0.0);
        SoundCategory category = data.of("Category").getEnum(SoundCategory.class).orElse(SoundCategory.PLAYERS);

        Targeter listeners = data.of("Listeners").serializeRegistry(Targeters.REGISTRY).orElse(null);
        List<Condition> listenerConditions = data.of("Listener_Conditions").getRegistryList(Conditions.REGISTRY);

        // If the user wants to use listener conditions, be sure to use a
        // targeter for listeners (Otherwise these conditions are ignored).
        if (!listenerConditions.isEmpty() && listeners == null)
            listeners = new WorldTargeter();

        return applyParentArgs(data, new SoundMechanic(sound, volume, pitch, noise, category, listeners, listenerConditions));
    }

    @Override
    public void playFor(CastData cast, List<Player> viewers) {

        // Cache to avoid overhead
        Location targetLocation = cast.getTargetLocation();
        float pitch = this.pitch + RandomUtil.range(-noise, noise);

        for (Player player : viewers) {
            player.playSound(targetLocation, sound, category, volume, pitch);
        }
    }

    @Override
    public @Nullable Targeter getViewerTargeter() {
        return listeners;
    }

    @Override
    public List<Condition> getViewerConditions() {
        return listenerConditions;
    }
}