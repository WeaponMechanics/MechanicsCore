package me.deecaad.core.emitter;

import me.deecaad.core.file.serializers.VectorProvider;
import me.deecaad.core.utils.shape.Shape;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable common configuration for an {@link Emitter}: shape, fallback emission direction,
 * speed, lifetime, emission rate, burst pattern, and per-emitted-item lifetime. Built via a
 * subclass {@link Builder}; the runtime emitter holds only mutable per-instance state.
 */
public abstract class EmitterSettings {

    private final Shape shape;
    private final VectorProvider direction;
    private final double speed;
    private final int durationTicks;
    private final double rate;
    private final int burstCount;
    private final int burstInterval;
    private final int emittedLifetimeTicks;
    private final int liveCap;

    protected EmitterSettings(@NotNull Builder<?> b) {
        this.shape = Objects.requireNonNull(b.shape, "shape required");
        this.direction = Objects.requireNonNull(b.direction, "direction required");
        this.speed = b.speed;
        this.durationTicks = b.durationTicks;
        this.rate = b.rate;
        this.burstCount = b.burstCount;
        this.burstInterval = Math.max(1, b.burstInterval);
        this.emittedLifetimeTicks = b.emittedLifetimeTicks;
        this.liveCap = b.liveCap;
    }

    public @NotNull Shape getShape() {
        return shape;
    }

    public @NotNull VectorProvider getDirection() {
        return direction;
    }

    public double getSpeed() {
        return speed;
    }

    /** {@code -1} = infinite (runs until externally stopped). */
    public int getDurationTicks() {
        return durationTicks;
    }

    /** Items emitted per tick (continuous mode). Fractional values are accumulated across ticks. */
    public double getRate() {
        return rate;
    }

    /** {@code > 0} switches to burst mode: emit this many items every {@link #getBurstInterval()} ticks. */
    public int getBurstCount() {
        return burstCount;
    }

    public int getBurstInterval() {
        return burstInterval;
    }

    public int getEmittedLifetimeTicks() {
        return emittedLifetimeTicks;
    }

    public int getLiveCap() {
        return liveCap;
    }

    /**
     * Recursive-generic builder so subclass builders chain fluently. Subclasses must implement
     * {@link #self()} returning {@code this} and {@link #build()} returning their concrete settings.
     */
    public abstract static class Builder<B extends Builder<B>> {

        protected Shape shape;
        protected VectorProvider direction;
        protected double speed;
        protected int durationTicks = -1;
        protected double rate = 1.0;
        protected int burstCount;
        protected int burstInterval = 1;
        protected int emittedLifetimeTicks = 20;
        protected int liveCap = Integer.MAX_VALUE;

        protected abstract @NotNull B self();

        public abstract @NotNull EmitterSettings build();

        public @NotNull B shape(@NotNull Shape shape) {
            this.shape = shape;
            return self();
        }

        public @NotNull B direction(@NotNull VectorProvider direction) {
            this.direction = direction;
            return self();
        }

        public @NotNull B speed(double speed) {
            this.speed = speed;
            return self();
        }

        public @NotNull B durationTicks(int durationTicks) {
            this.durationTicks = durationTicks;
            return self();
        }

        public @NotNull B rate(double rate) {
            this.rate = rate;
            return self();
        }

        /**
         * Switches the emitter into burst mode: emit {@code count} items every {@code interval} ticks.
         */
        public @NotNull B burst(int count, int interval) {
            this.burstCount = count;
            this.burstInterval = Math.max(1, interval);
            return self();
        }

        public @NotNull B emittedLifetimeTicks(int emittedLifetimeTicks) {
            this.emittedLifetimeTicks = emittedLifetimeTicks;
            return self();
        }

        public @NotNull B liveCap(int liveCap) {
            this.liveCap = liveCap;
            return self();
        }
    }
}
