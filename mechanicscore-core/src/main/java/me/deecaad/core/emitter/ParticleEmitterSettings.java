package me.deecaad.core.emitter;

import me.deecaad.core.transition.Transition;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * {@link EmitterSettings} for a {@link ParticleEmitter}. Adds particle type, per-emission count and
 * spread, particle speed, and optional emitter-lifetime transitions for particle type, color (used
 * with {@code DUST}), and count.
 */
public final class ParticleEmitterSettings extends EmitterSettings {

    private final Particle particle;
    private final int countPerPoint;
    private final Vector spread;
    private final double particleSpeed;
    private final @Nullable Transition<Particle> particleTransition;
    private final @Nullable Transition<Color> colorTransition;
    private final @Nullable Transition<Double> countTransition;

    private ParticleEmitterSettings(@NotNull Builder b) {
        super(b);
        this.particle = Objects.requireNonNull(b.particle, "particle required");
        this.countPerPoint = b.countPerPoint;
        this.spread = b.spread.clone();
        this.particleSpeed = b.particleSpeed;
        this.particleTransition = b.particleTransition;
        this.colorTransition = b.colorTransition;
        this.countTransition = b.countTransition;
    }

    public @NotNull Particle getParticle() {
        return particle;
    }

    public int getCountPerPoint() {
        return countPerPoint;
    }

    public @NotNull Vector getSpread() {
        return spread.clone();
    }

    public double getParticleSpeed() {
        return particleSpeed;
    }

    public @Nullable Transition<Particle> getParticleTransition() {
        return particleTransition;
    }

    public @Nullable Transition<Color> getColorTransition() {
        return colorTransition;
    }

    public @Nullable Transition<Double> getCountTransition() {
        return countTransition;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder extends EmitterSettings.Builder<Builder> {

        private Particle particle;
        private int countPerPoint = 1;
        private Vector spread = new Vector(0, 0, 0);
        private double particleSpeed;
        private @Nullable Transition<Particle> particleTransition;
        private @Nullable Transition<Color> colorTransition;
        private @Nullable Transition<Double> countTransition;

        @Override protected @NotNull Builder self() {
            return this;
        }

        @Override public @NotNull ParticleEmitterSettings build() {
            return new ParticleEmitterSettings(this);
        }

        public @NotNull Builder particle(@NotNull Particle particle) {
            this.particle = particle;
            return this;
        }

        public @NotNull Builder countPerPoint(int countPerPoint) {
            this.countPerPoint = countPerPoint;
            return this;
        }

        public @NotNull Builder spread(@NotNull Vector spread) {
            this.spread = spread.clone();
            return this;
        }

        public @NotNull Builder particleSpeed(double particleSpeed) {
            this.particleSpeed = particleSpeed;
            return this;
        }

        public @NotNull Builder particleTransition(@Nullable Transition<Particle> particleTransition) {
            this.particleTransition = particleTransition;
            return this;
        }

        public @NotNull Builder colorTransition(@Nullable Transition<Color> colorTransition) {
            this.colorTransition = colorTransition;
            return this;
        }

        public @NotNull Builder countTransition(@Nullable Transition<Double> countTransition) {
            this.countTransition = countTransition;
            return this;
        }
    }
}
