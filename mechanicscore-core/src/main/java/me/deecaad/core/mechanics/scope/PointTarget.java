package me.deecaad.core.mechanics.scope;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A target backed by a location. The location may be supplied lazily, which
 * shape/scatter targeters rely on.
 */
public final class PointTarget implements Target {

    private final Supplier<Location> location;

    public PointTarget(@NotNull Location location) {
        Location snapshot = location.clone();
        this.location = () -> snapshot;
    }

    public PointTarget(@NotNull Supplier<Location> location) {
        this.location = location;
    }

    @Override
    public @Nullable LivingEntity entity() {
        return null;
    }

    @Override
    public @NotNull Location location() {
        // Clone on read so callers cannot corrupt the backing location.
        return location.get().clone();
    }

    @Override
    public @NotNull World world() {
        return location.get().getWorld();
    }
}
