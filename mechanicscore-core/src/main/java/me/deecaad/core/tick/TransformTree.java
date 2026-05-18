package me.deecaad.core.tick;

import me.deecaad.core.utils.TransformLike;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter that drives a {@link TransformLike} tree once per tick by walking it via
 * {@link me.deecaad.core.utils.Transform#tick()}. Register one with a {@link TickManager} for
 * trees containing animated transforms (e.g. an entity-bound root or per-tick computed nodes).
 */
public final class TransformTree implements Tickable {

    private final TransformLike root;
    private final Location region;
    private volatile boolean stopped;

    /**
     * @param root The non-null root of the tree to tick.
     * @param region The non-null location used to pick a region (Folia only); cloned defensively.
     */
    public TransformTree(@NotNull TransformLike root, @NotNull Location region) {
        this.root = root;
        this.region = region.clone();
    }

    /**
     * Stops this tree. The next tick will return {@code true} and the {@link TickManager} will
     * remove it.
     */
    public void stop() {
        stopped = true;
    }

    @Override
    public boolean tick() {
        if (stopped)
            return true;
        root.getTransform().tick();
        return false;
    }

    @Override
    public @NotNull Location getTickLocation() {
        return region;
    }
}
