package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

/**
 * Checks the distance between two named points. {@code From} defaults to the
 * subject being tested, {@code To} defaults to {@code source}. This makes the
 * old "always from source" behavior explicit and lets you measure from any
 * named context instead.
 */
public class RangeCondition extends Condition {

    private @Nullable String fromName;
    private @NotNull String toName = CastScope.SOURCE;
    private OptionalDouble minSquared;
    private OptionalDouble maxSquared;

    public RangeCondition() {
    }

    public RangeCondition(@Nullable String fromName, @NotNull String toName, OptionalDouble minSquared, OptionalDouble maxSquared) {
        this.fromName = fromName;
        this.toName = toName;
        this.minSquared = minSquared;
        this.maxSquared = maxSquared;
    }

    @Override
    public boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        Location from = resolve(scope, subject, fromName);
        Location to = resolve(scope, subject, toName);
        if (from == null || to == null)
            return false;

        double distanceSquared = from.distanceSquared(to);
        if (minSquared.isPresent() && distanceSquared < minSquared.getAsDouble())
            return false;
        if (maxSquared.isPresent() && distanceSquared >= maxSquared.getAsDouble())
            return false;
        return true;
    }

    private @Nullable Location resolve(@NotNull CastScope scope, @Nullable Target subject, @Nullable String name) {
        if (name == null)
            return subject == null ? null : subject.location();
        Context context = scope.getContext(name);
        Target first = context == null ? null : context.first();
        return first == null ? null : first.location();
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "range");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/range";
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        String fromName = data.of("From").get(String.class).orElse(null);
        String toName = data.of("To").get(String.class).orElse(CastScope.SOURCE);
        OptionalDouble minNum = data.of("Min").assertRange(0.0, null).getDouble();
        OptionalDouble maxNum = data.of("Max").assertRange(0.0, null).getDouble();

        OptionalDouble min = minNum.isPresent() ? OptionalDouble.of(minNum.getAsDouble() * minNum.getAsDouble()) : OptionalDouble.empty();
        OptionalDouble max = maxNum.isPresent() ? OptionalDouble.of(maxNum.getAsDouble() * maxNum.getAsDouble()) : OptionalDouble.empty();
        return applyParentArgs(data, new RangeCondition(fromName, toName, min, max));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
