package me.deecaad.core.tick;

import com.cjcrafter.foliascheduler.ServerImplementation;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Manages a collection of {@link Tickable}s, driving each once per tick using the platform's
 * preferred strategy.
 *
 * <ul>
 *   <li>On Folia, one scheduled region task per {@code Tickable} (necessary because regions own
 *       different threads).
 *   <li>On Spigot/Paper, one shared global task iterates every {@code Tickable} (cheaper than N
 *       individual tasks when N is large).
 * </ul>
 *
 * Build one with {@link #create(Plugin, ServerImplementation)} and shut it down on plugin disable.
 */
public interface TickManager {

    /**
     * Registers a tickable. From this point on, {@link Tickable#tick()} is invoked once per tick on
     * the appropriate thread until it returns {@code true} (or this manager is shut down).
     *
     * @param tickable The non-null tickable to register.
     */
    void add(@NotNull Tickable tickable);

    /**
     * Cancels all scheduled work and calls {@link Tickable#remove()} on every registered tickable.
     * Safe to call multiple times; subsequent {@link #add(Tickable)} calls throw.
     */
    void shutdown();

    /**
     * Returns the manager appropriate for this server: a Folia variant if the server exposes
     * {@code Server#isOwnedByCurrentRegion(Location)}, otherwise a Bukkit shared-loop variant.
     *
     * @param plugin The non-null plugin (used for logging).
     * @param scheduler The non-null foliascheduler abstraction.
     * @return A non-null tick manager.
     */
    static @NotNull TickManager create(@NotNull Plugin plugin, @NotNull ServerImplementation scheduler) {
        try {
            Server.class.getMethod("isOwnedByCurrentRegion", Location.class);
            return new FoliaTickManager(plugin, scheduler);
        } catch (NoSuchMethodException e) {
            return new BukkitTickManager(plugin, scheduler);
        }
    }
}
