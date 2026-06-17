package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ShapeTargeter extends RelativeTargeter {

    public ShapeTargeter() {
    }

    @Override
    public boolean isEntity() {
        return false;
    }

    @Override
    public final @NotNull Context target(@NotNull CastScope scope) {
        Context originContext = scope.getContext(getFrom());
        Target originTarget = originContext == null ? null : originContext.first();
        if (originTarget == null)
            return Context.empty();

        Location origin = isEye() && originTarget.entity() != null
            ? originTarget.entity().getEyeLocation()
            : originTarget.location();

        Iterator<Vector> points = getPoints(scope, origin);
        List<Target> targets = new ArrayList<>();
        while (points.hasNext()) {
            Vector point = points.next();
            targets.add(pointTarget(origin.clone().add(point)));
        }
        return Context.of(targets);
    }

    /**
     * Produces the points of this shape, relative to the origin.
     *
     * @param scope the cast scope.
     * @param origin the resolved origin location.
     * @return an iterator of relative point vectors.
     */
    public abstract @NotNull Iterator<Vector> getPoints(@NotNull CastScope scope, @NotNull Location origin);
}
