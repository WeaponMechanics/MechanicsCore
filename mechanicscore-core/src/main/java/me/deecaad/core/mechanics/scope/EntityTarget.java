package me.deecaad.core.mechanics.scope;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A target backed by a living entity. The location is computed lazily so it
 * tracks the entity if it moves between binding and use.
 */
public final class EntityTarget implements Target {

    private final LivingEntity entity;
    private final boolean eye;
    private final @Nullable Vector offset;

    public EntityTarget(@NotNull LivingEntity entity) {
        this(entity, false, null);
    }

    public EntityTarget(@NotNull LivingEntity entity, boolean eye, @Nullable Vector offset) {
        this.entity = entity;
        this.eye = eye;
        this.offset = offset;
    }

    @Override
    public @NotNull LivingEntity entity() {
        return entity;
    }

    @Override
    public @NotNull Location location() {
        // getLocation/getEyeLocation return fresh clones, so mutating is safe.
        Location base = eye ? entity.getEyeLocation() : entity.getLocation();
        if (offset != null)
            base.add(offset);
        return base;
    }

    @Override
    public @NotNull World world() {
        return entity.getWorld();
    }
}
