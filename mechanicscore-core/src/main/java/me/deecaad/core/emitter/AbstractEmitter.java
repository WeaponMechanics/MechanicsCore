package me.deecaad.core.emitter;

import me.deecaad.core.utils.Transform;
import me.deecaad.core.utils.shape.ShapePoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Common emission logic for emitters: holds the immutable {@link EmitterSettings}, the
 * {@link Transform} that places the emitter in the hierarchy, and the per-tick emission scheduling.
 * Subclasses fill in {@link #emit(Location, Vector)} and (optionally) {@link #tickAlive()} /
 * {@link #hasLiveItems()}.
 *
 * <p>
 * Set the world before registering with a {@code TickManager}: either via {@link #spawnAt(Location)}
 * (which also seeds the transform from the location) or via {@link #setWorld(World)}.
 */
public abstract class AbstractEmitter<S extends EmitterSettings> implements Emitter {

    protected final S settings;
    private final Transform transform;
    private @Nullable World world;
    private int age;
    private double accumulator;
    private volatile boolean stopped;

    protected AbstractEmitter(@NotNull S settings) {
        this.settings = settings;
        this.transform = new Transform(this);
    }

    public @NotNull S getSettings() {
        return settings;
    }

    @Override
    public final @NotNull Transform getTransform() {
        return transform;
    }

    /**
     * Convenience: seeds the transform from {@code location} (position + yaw/pitch) and captures
     * its world. Equivalent to setting the transform manually and calling {@link #setWorld(World)}.
     */
    public void spawnAt(@NotNull Location location) {
        this.world = location.getWorld();
        this.transform.set(location.toVector(),
            Transform.fromYawPitch(location.getYaw(), location.getPitch()));
    }

    public void setWorld(@NotNull World world) {
        this.world = world;
    }

    public @Nullable World getWorld() {
        return world;
    }

    public int getAge() {
        return age;
    }

    /**
     * @return Age normalized to {@code [0, 1]} by {@code durationTicks}; returns {@code 0} for
     *         infinite emitters.
     */
    public double normalizedAge() {
        int d = settings.getDurationTicks();
        if (d <= 0) return 0.0;
        return Math.min(1.0, age / (double) d);
    }

    /** Requests early termination; the next tick returns {@code true}. */
    public void stop() {
        this.stopped = true;
    }

    @Override
    public final boolean tick() {
        if (stopped)
            return true;
        if (world == null)
            throw new IllegalStateException("Emitter world not set; call spawnAt(...) or setWorld(...) before adding to a TickManager");

        int toEmit = computeEmitCount();
        for (int i = 0; i < toEmit; i++)
            emitOne();

        tickAlive();
        age++;
        return isFinished();
    }

    private int computeEmitCount() {
        int duration = settings.getDurationTicks();
        if (duration >= 0 && age >= duration)
            return 0;

        if (settings.getBurstCount() > 0)
            return age % settings.getBurstInterval() == 0 ? settings.getBurstCount() : 0;

        accumulator += settings.getRate();
        int n = (int) accumulator;
        accumulator -= n;
        return n;
    }

    private void emitOne() {
        ShapePoint sp = settings.getShape().getPoint(ThreadLocalRandom.current().nextDouble());
        Vector worldOffset = Transform.rotate(transform.getRotation(), sp.offset());
        Vector worldPos = transform.getPosition().add(worldOffset);
        // Spawn yaw/pitch are 0 by Location default. DisplayEntityEmitter depends on this so
        // translation-based motion equals world-space; do not pass yaw/pitch here.
        Location point = new Location(world, worldPos.getX(), worldPos.getY(), worldPos.getZ());

        Vector worldDir;
        if (sp.direction().lengthSquared() < 1e-12) {
            worldDir = settings.getDirection().provide(transform.getRotation()).clone();
        } else {
            worldDir = Transform.rotate(transform.getRotation(), sp.direction());
        }
        worldDir.multiply(settings.getSpeed());
        emit(point, worldDir);
    }

    @Override
    public @Nullable Location getTickLocation() {
        if (world == null)
            return null;
        Vector p = transform.getPosition();
        return new Location(world, p.getX(), p.getY(), p.getZ());
    }

    @Override
    public boolean isFinished() {
        int d = settings.getDurationTicks();
        return d >= 0 && age >= d && !hasLiveItems();
    }

    /**
     * Spawns one emitted item. Called once per item selected by the emission scheduler.
     */
    protected abstract void emit(@NotNull Location point, @NotNull Vector velocity);

    /**
     * Advances any persistent emitted items (display entities, etc.). No-op by default for
     * fire-and-forget emitters like particles.
     */
    protected void tickAlive() {
    }

    /**
     * @return {@code true} if any emitted item is still alive and must keep the emitter ticking
     *         past its duration.
     */
    protected boolean hasLiveItems() {
        return false;
    }
}
