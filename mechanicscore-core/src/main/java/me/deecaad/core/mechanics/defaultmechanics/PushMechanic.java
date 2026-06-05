package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.utils.VectorUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pushes the subject away from a named origin context ({@code From}, defaults to
 * source).
 */
public class PushMechanic extends Mechanic {

    private double speed;
    private double verticalMultiplier;
    private @NotNull String from = CastScope.SOURCE;

    public PushMechanic() {
    }

    public PushMechanic(double speed, double verticalMultiplier, @NotNull String from) {
        this.speed = speed;
        this.verticalMultiplier = verticalMultiplier;
        this.from = from;
    }

    public double getSpeed() {
        return speed;
    }

    public double getVerticalMultiplier() {
        return verticalMultiplier;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || subject.entity() == null)
            return;

        Context fromContext = scope.getContext(from);
        Target fromTarget = fromContext == null ? null : fromContext.first();
        if (fromTarget == null)
            return;
        Location fromLocation = fromTarget.location();

        Vector velocity = subject.location().subtract(fromLocation).toVector();
        if (VectorUtil.isZero(velocity))
            return;

        velocity.setY(velocity.getY() * verticalMultiplier);
        velocity.normalize().multiply(speed);
        subject.entity().setVelocity(velocity);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "push");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/push";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().doubleKey("Speed").required().doubleKey("Vertical_Multiplier").stringKey("From");
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        double speed = data.of("Speed").assertExists().getDouble().getAsDouble();
        double verticalMultiplier = data.of("Vertical_Multiplier").getDouble().orElse(1.0);
        String from = data.of("From").get(String.class).orElse(CastScope.SOURCE);
        return applyParentArgs(data, new PushMechanic(speed, verticalMultiplier, from));
    }
}
