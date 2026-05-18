package me.deecaad.core.tick;

import com.cjcrafter.foliascheduler.ServerImplementation;
import com.cjcrafter.foliascheduler.TaskImplementation;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

final class BukkitTickManager implements TickManager {

    private final Plugin plugin;
    private final ServerImplementation scheduler;
    private final List<Tickable> tickables = new ArrayList<>();
    private final List<Tickable> pending = new ArrayList<>();
    private TaskImplementation<Void> task;
    private volatile boolean shutdown;

    BukkitTickManager(@NotNull Plugin plugin, @NotNull ServerImplementation scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public synchronized void add(@NotNull Tickable tickable) {
        if (shutdown)
            throw new IllegalStateException("TickManager has been shut down");

        pending.add(tickable);
        if (task == null) {
            Consumer<TaskImplementation<Void>> consumer = t -> tickAll();
            task = scheduler.global().runAtFixedRate(consumer, 1, 1);
        }
    }

    private void tickAll() {
        synchronized (this) {
            if (!pending.isEmpty()) {
                tickables.addAll(pending);
                pending.clear();
            }
        }

        for (Iterator<Tickable> it = tickables.iterator(); it.hasNext();) {
            Tickable t = it.next();
            boolean finished;
            try {
                finished = t.tick();
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.WARNING, "Tickable threw during tick; removing", ex);
                finished = true;
            }
            if (finished) {
                safeRemove(t);
                it.remove();
            }
        }
    }

    @Override
    public synchronized void shutdown() {
        if (shutdown)
            return;
        shutdown = true;

        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.WARNING, "Error cancelling shared tick task", ex);
            }
            task = null;
        }
        for (Tickable t : tickables)
            safeRemove(t);
        tickables.clear();
        for (Tickable t : pending)
            safeRemove(t);
        pending.clear();
    }

    private void safeRemove(@NotNull Tickable tickable) {
        try {
            tickable.remove();
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING, "Tickable threw during remove", ex);
        }
    }
}
