package me.deecaad.core.mechanics.scope;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single target, unifying an entity and a location into one type. Either may be absent: an entity
 * target also carries its location, and a pure-location target has no entity.
 */
public interface Target {

    /**
     * The entity behind this target, or {@code null} for a pure-location target.
     */
    @Nullable LivingEntity entity();

    /**
     * The location of this target. Always non-null. For entity targets this is
     * derived from the entity (optionally eye-level, optionally offset).
     */
    @NotNull Location location();

    /**
     * The world this target is in.
     */
    @NotNull World world();

    static Target of(@NotNull LivingEntity entity) {
        return new EntityTarget(entity);
    }

    static Target of(@NotNull Location location) {
        return new PointTarget(location);
    }
}
