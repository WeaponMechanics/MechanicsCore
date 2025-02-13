package me.deecaad.core.mechanics;

import me.deecaad.core.mechanics.defaultmechanics.ActionBarMechanic;
import me.deecaad.core.mechanics.defaultmechanics.BossBarMechanic;
import me.deecaad.core.mechanics.defaultmechanics.CommandMechanic;
import me.deecaad.core.mechanics.defaultmechanics.CustomSoundMechanic;
import me.deecaad.core.mechanics.defaultmechanics.DamageMechanic;
import me.deecaad.core.mechanics.defaultmechanics.DropItemMechanic;
import me.deecaad.core.mechanics.defaultmechanics.FireworkMechanic;
import me.deecaad.core.mechanics.defaultmechanics.IgniteMechanic;
import me.deecaad.core.mechanics.defaultmechanics.LeapMechanic;
import me.deecaad.core.mechanics.defaultmechanics.LightningMechanic;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.defaultmechanics.MessageMechanic;
import me.deecaad.core.mechanics.defaultmechanics.PotionMechanic;
import me.deecaad.core.mechanics.defaultmechanics.PushMechanic;
import me.deecaad.core.mechanics.defaultmechanics.SculkBloomMechanic;
import me.deecaad.core.mechanics.defaultmechanics.SculkShriekMechanic;
import me.deecaad.core.mechanics.defaultmechanics.SoundMechanic;
import me.deecaad.core.mechanics.defaultmechanics.TitleMechanic;
import me.deecaad.core.utils.MutableRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Utility class holding all built-in mechanics, and the registry for all
 * mechanics. This class is not meant to be instantiated.
 *
 * <p>The registry {@link Mechanics#REGISTRY} is a mutable registry, meaning
 * that you can external sources may add/overwrite mechanics.
 */
public class Mechanics {

    /**
     * The registry for all globally registered mechanics.
     */
    public static final @NotNull MutableRegistry<Mechanic> REGISTRY
            = new MutableRegistry.SimpleMutableRegistry<>(new HashMap<>());

    public static final @NotNull Mechanic ACTION_BAR = register(new ActionBarMechanic());
    public static final @NotNull Mechanic BOSS_BAR = register(new BossBarMechanic());
    public static final @NotNull Mechanic COMMAND = register(new CommandMechanic());
    public static final @NotNull Mechanic CUSTOM_SOUND = register(new CustomSoundMechanic());
    public static final @NotNull Mechanic DAMAGE = register(new DamageMechanic());
    public static final @NotNull Mechanic DROP_ITEM = register(new DropItemMechanic());
    public static final @NotNull Mechanic FIREWORK = register(new FireworkMechanic());
    public static final @NotNull Mechanic IGNITE = register(new IgniteMechanic());
    public static final @NotNull Mechanic LEAP = register(new LeapMechanic());
    public static final @NotNull Mechanic LIGHTNING = register(new LightningMechanic());
    public static final @NotNull Mechanic MESSAGE = register(new MessageMechanic());
    public static final @NotNull Mechanic POTION = register(new PotionMechanic());
    public static final @NotNull Mechanic PUSH = register(new PushMechanic());
    public static final @NotNull Mechanic SCULK_BLOOM = register(new SculkBloomMechanic());
    public static final @NotNull Mechanic SCULK_SHRIEK = register(new SculkShriekMechanic());
    public static final @NotNull Mechanic SOUND = register(new SoundMechanic());
    public static final @NotNull Mechanic TITLE = register(new TitleMechanic());


    private Mechanics() {
    }

    private static @NotNull Mechanic register(@NotNull Mechanic mechanic) {
        REGISTRY.add(mechanic);
        return mechanic;
    }
}
