package me.deecaad.core.utils;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.MechanicsCore;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Drives a {@link Transform} tree once per tick via a repeating region task. Use this for transform
 * trees that change continuously -- ones rooted at an {@link EntityTransform}, or containing
 * animated transforms. {@link #stop()} must be called when the tree is no longer needed.
 */
public final class TransformTicker {

    private final TransformLike root;
    private final Location region;
    private TaskImplementation<Void> task;

    public TransformTicker(@NotNull TransformLike root, @NotNull Location region) {
        this.root = root;
        this.region = region;
    }

    public boolean isRunning() {
        return task != null;
    }

    public void start() {
        if (task != null)
            return;

        Consumer<TaskImplementation<Void>> consumer = t -> root.getTransform().tick();
        task = MechanicsCore.getInstance().getFoliaScheduler()
            .region(region)
            .runAtFixedRate(consumer, 1, 1);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
