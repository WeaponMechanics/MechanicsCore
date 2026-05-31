package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.ChanceSerializer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.TargetKind;

/**
 * A Mechanic is a single action an admin writes in config. The behavior lives in
 * {@link #use0(CastScope, Target)}, which runs once per subject target. Targeting,
 * conditions, repeat, delay, and chance are handled by the executor, not here.
 */
public abstract class Mechanic implements InlineSerializer<Mechanic> {

    private int repeatAmount = 1;
    private int repeatInterval = 1;
    private int delayBeforePlay = 0;
    private double chance = 1.0;

    public Mechanic() {
    }

    public int getRepeatAmount() {
        return repeatAmount;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public int getDelayBeforePlay() {
        return delayBeforePlay;
    }

    public double getChance() {
        return chance;
    }

    /**
     * Runs this mechanic on a single subject target.
     *
     * @param scope the cast scope.
     * @param subject the subject target this mechanic acts on (may be null).
     */
    public abstract void use0(CastScope scope, Target subject);

    /**
     * The narrowest target kind this mechanic needs. Lets the optimizer pick a
     * cheaper query when every consumer is satisfied by players. Defaults to
     * {@code LIVING_ENTITY}; location-only mechanics should return {@code LOCATION}.
     */
    public TargetKind requiredTarget() {
        return TargetKind.LIVING_ENTITY;
    }

    /**
     * Whether this is a per-target effect with no scheduling that is safe to
     * group with siblings sharing the same subject query (e.g. particles/sounds).
     */
    public boolean isBatchablePlayerEffect() {
        return false;
    }

    public Mechanic applyParentArgs(SerializeData data, Mechanic mechanic) throws SerializerException {
        mechanic.repeatAmount = data.of("Repeat_Amount").assertRange(1, null).getInt().orElse(1);
        mechanic.repeatInterval = data.of("Repeat_Interval").assertRange(1, null).getInt().orElse(1);
        mechanic.delayBeforePlay = data.of("Delay_Before_Play").assertRange(0, null).getInt().orElse(0);
        mechanic.chance = data.of("Chance").serialize(ChanceSerializer.class).orElse(1.0);
        return mechanic;
    }
}
