package me.deecaad.core.tick;

import com.cjcrafter.foliascheduler.ServerImplementation;
import com.cjcrafter.foliascheduler.TaskImplementation;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

final class FoliaTickManager implements TickManager {

    private final Plugin plugin;
    private final ServerImplementation scheduler;
    private final Map<Tickable, TaskImplementation<Void>> tasks = new ConcurrentHashMap<>();
    private volatile boolean shutdown;

    FoliaTickManager(@NotNull Plugin plugin, @NotNull ServerImplementation scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void add(@NotNull Tickable tickable) {
        if (shutdown)
            throw new IllegalStateException("TickManager has been shut down");

        Location loc = tickable.getTickLocation();
        if (loc == null)
            throw new IllegalArgumentException("Tickable.getTickLocation() must be non-null on Folia: " + tickable);

        Consumer<TaskImplementation<Void>> consumer = task -> {
            boolean finished;
            try {
                finished = tickable.tick();
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.WARNING, "Tickable threw during tick; removing", ex);
                finished = true;
            }
            if (finished) {
                task.cancel();
                tasks.remove(tickable);
                safeRemove(tickable);
            }
        };

        TaskImplementation<Void> task = scheduler.region(loc).runAtFixedRate(consumer, 1, 1);
        tasks.put(tickable, task);
    }

    @Override
    public void shutdown() {
        if (shutdown)
            return;
        shutdown = true;

        for (Map.Entry<Tickable, TaskImplementation<Void>> entry : tasks.entrySet()) {
            try {
                entry.getValue().cancel();
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.WARNING, "Error cancelling Tickable task", ex);
            }
            safeRemove(entry.getKey());
        }
        tasks.clear();
    }

    private void safeRemove(@NotNull Tickable tickable) {
        try {
            tickable.remove();
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING, "Tickable threw during remove", ex);
        }
    }
}
