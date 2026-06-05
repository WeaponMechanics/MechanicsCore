package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.utils.VectorUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeapMechanic extends Mechanic {

    private double speed;
    private double verticalMultiplier;

    public LeapMechanic() {
    }

    public LeapMechanic(double speed, double verticalMultiplier) {
        this.speed = speed;
        this.verticalMultiplier = verticalMultiplier;
    }

    public double getSpeed() {
        return speed;
    }

    public double getVerticalMultiplier() {
        return verticalMultiplier;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        LivingEntity source = scope.sourceEntity();
        if (source == null || subject == null)
            return;

        Vector velocity = subject.location().subtract(source.getLocation()).toVector();
        if (VectorUtil.isZero(velocity))
            return;

        velocity.setY(velocity.getY() * verticalMultiplier);
        velocity.normalize().multiply(speed);
        source.setVelocity(velocity);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "leap");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/leap";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().doubleKey("Speed").required().doubleKey("Vertical_Multiplier");
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        double speed = data.of("Speed").assertExists().getDouble().getAsDouble();
        double verticalMultiplier = data.of("Vertical_Multiplier").getDouble().orElse(1.0);
        return applyParentArgs(data, new LeapMechanic(speed, verticalMultiplier));
    }
}
