package me.deecaad.core.emitter;

import me.deecaad.core.transition.Transition;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Fire-and-forget particle emitter. Each emission point spawns one or more particles at that
 * location; there's no per-particle tracking (Minecraft particles aren't controllable post-spawn).
 * Optional transitions on the emitter's normalized age vary particle type, color (used with
 * {@code DUST}), and count over the emitter's lifetime.
 */
public final class ParticleEmitter extends AbstractEmitter<ParticleEmitterSettings> {

    public ParticleEmitter(@NotNull ParticleEmitterSettings settings) {
        super(settings);
    }

    @Override
    protected void emit(@NotNull Location point, @NotNull Vector velocity) {
        World world = point.getWorld();
        if (world == null)
            return;

        double t = normalizedAge();

        Transition<Particle> particleTrans = settings.getParticleTransition();
        Particle type = particleTrans != null ? particleTrans.evaluate(t) : settings.getParticle();

        Transition<Double> countTrans = settings.getCountTransition();
        int count = countTrans != null ? Math.max(0, countTrans.evaluate(t).intValue()) : settings.getCountPerPoint();
        if (count == 0)
            return;

        Vector spread = settings.getSpread();
        Object data = null;
        Transition<Color> colorTrans = settings.getColorTransition();
        if (colorTrans != null)
            data = new Particle.DustOptions(colorTrans.evaluate(t), 1.0f);

        world.spawnParticle(
            type, point, count,
            spread.getX(), spread.getY(), spread.getZ(),
            settings.getParticleSpeed(), data);
    }
}
