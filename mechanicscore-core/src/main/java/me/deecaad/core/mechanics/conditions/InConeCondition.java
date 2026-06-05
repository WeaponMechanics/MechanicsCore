package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.file.serializers.VectorProvider;
import me.deecaad.core.file.serializers.VectorSerializer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.utils.EntityTransform;
import me.deecaad.core.utils.VectorUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks whether the subject lies within a cone projected from an apex context
 * (defaults to {@code source}).
 */
public class InConeCondition extends Condition {

    private VectorProvider direction;
    private double cosAngle;
    private @NotNull String apexName = CastScope.SOURCE;

    public InConeCondition() {
    }

    public InConeCondition(@Nullable VectorProvider direction, double angle, @NotNull String apexName) {
        this.direction = direction;
        this.cosAngle = Math.cos(Math.toRadians(angle));
        this.apexName = apexName;
    }

    @Override
    public boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        if (subject == null)
            return false;

        Context apexContext = scope.getContext(apexName);
        Target apex = apexContext == null ? null : apexContext.first();
        if (apex == null)
            return false;

        Location apexLocation = apex.entity() != null ? apex.entity().getEyeLocation() : apex.location();
        Vector source = apexLocation.toVector();
        Vector direction;
        if (this.direction != null) {
            Quaterniond rotation = apex.entity() != null ? new EntityTransform(apex.entity()).getLocalRotation() : null;
            direction = this.direction.provide(rotation).normalize();
        } else {
            direction = apexLocation.getDirection();
        }

        // Move the cone's origin back a bit to catch overlapping entities.
        VectorUtil.addScaledVector(source, direction, -0.5);

        Vector target = subject.location().toVector();
        Vector toTarget = target.clone().subtract(source).normalize();
        return direction.dot(toTarget) >= cosAngle;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "incone");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/incone";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().doubleKey("Angle").range(0.0, 180.0).nested("Direction", VectorSerializer.class).stringKey("Apex");
    }

    @Override
    public @NotNull Condition serialize(@NotNull SerializeData data) throws SerializerException {
        double angle = data.of("Angle").assertRange(0.0, 180.0).getDouble().orElse(30.0);
        VectorProvider direction = data.of("Direction").serialize(VectorSerializer.class).orElse(null);
        String apexName = data.of("Apex").get(String.class).orElse(CastScope.SOURCE);
        return applyParentArgs(data, new InConeCondition(direction, angle, apexName));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
