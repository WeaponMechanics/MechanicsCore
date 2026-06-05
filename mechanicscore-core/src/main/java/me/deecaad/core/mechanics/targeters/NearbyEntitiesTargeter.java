package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Targets living entities within a radius of an origin context (default source),
 * nearest first. Supports a result limit and excluding the entities of another
 * named context (handy for recursion, e.g. excluding already-hit entities).
 */
public class NearbyEntitiesTargeter extends Targeter {

    private double radius;
    private int limit;
    private @Nullable String exclude;
    private boolean playersOnly;

    public NearbyEntitiesTargeter() {
    }

    public NearbyEntitiesTargeter(double radius, int limit, @Nullable String exclude) {
        this.radius = radius;
        this.limit = limit;
        this.exclude = exclude;
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    @Override
    public @NotNull Context target(@NotNull CastScope scope) {
        Context originContext = scope.getContext(getFrom());
        Target originTarget = originContext == null ? null : originContext.first();
        if (originTarget == null)
            return Context.empty();

        Location origin = isEye() && originTarget.entity() != null
            ? originTarget.entity().getEyeLocation()
            : originTarget.location();
        World world = origin.getWorld();
        if (world == null)
            return Context.empty();

        Set<LivingEntity> excluded = new HashSet<>();
        if (exclude != null) {
            Context excludeContext = scope.getContext(exclude);
            if (excludeContext != null)
                excludeContext.entities().forEach(excluded::add);
        }

        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity entity : world.getNearbyLivingEntities(origin, radius)) {
            if (playersOnly && !(entity instanceof org.bukkit.entity.Player))
                continue;
            if (!excluded.contains(entity))
                found.add(entity);
        }
        found.sort(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(origin)));
        if (limit >= 0 && found.size() > limit)
            found = found.subList(0, limit);

        List<Target> targets = new ArrayList<>(found.size());
        for (LivingEntity entity : found)
            targets.add(entityTarget(entity));
        return Context.of(targets);
    }

    @Override
    public @NotNull Targeter specialize(@NotNull me.deecaad.core.mechanics.scope.TargetKind demand) {
        if (demand == me.deecaad.core.mechanics.scope.TargetKind.PLAYER)
            playersOnly = true;
        return this;
    }

    @Override
    public @Nullable Object groupKey() {
        return "nearby:" + getFrom() + ":" + radius + ":" + limit + ":" + exclude + ":" + playersOnly + ":" + isEye() + ":" + (getOffset() != null);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "nearby_entities");
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/targeters/nearbyentities";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().doubleKey("Radius").required().range(0.0, null).intKey("Limit").range(0, null).stringKey("Exclude");
    }

    @NotNull @Override
    public Targeter serialize(@NotNull SerializeData data) throws SerializerException {
        double radius = data.of("Radius").assertExists().assertRange(0.0, null).getDouble().getAsDouble();
        int limit = data.of("Limit").assertRange(0, null).getInt().orElse(-1);
        String exclude = data.of("Exclude").get(String.class).orElse(null);
        return applyParentArgs(data, new NearbyEntitiesTargeter(radius, limit, exclude));
    }
}
