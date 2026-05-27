package me.deecaad.core.emitter;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.entity.FakeDisplayEntity;
import me.deecaad.core.compatibility.entity.FakeEntity;
import me.deecaad.core.transition.DisplayTransformTrack;
import me.deecaad.core.transition.Transition;
import me.deecaad.core.transition.TransitionPlayback;
import me.deecaad.core.transition.TransitionPlayback.Segment;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Emits persistent {@link FakeDisplayEntity} packets. Each emitted entity carries its own age,
 * velocity, and a precomputed playback schedule for any per-entity transitions. Entities are
 * detached from the emitter's transform once spawned so they follow their own world path.
 *
 * <p>
 * When an emitted entity has motion (initial velocity, velocity transition, acceleration, or spin),
 * the motion is baked into the {@link Transformation} each tick and pushed with
 * {@code interpolation_duration = 1}. The entity itself stays anchored at its spawn location, and
 * the client smooths motion via the built-in display interpolation rather than per-tick teleport
 * packets.
 */
public final class DisplayEntityEmitter extends AbstractEmitter<DisplayEntityEmitterSettings> {

    public DisplayEntityEmitter(@NotNull DisplayEntityEmitterSettings settings) {
        super(settings);
    }

    private static final Vector3f IDENTITY_SCALE = new Vector3f(1, 1, 1);

    static final class Emitted implements EmittedDisplay {
        final FakeEntity entity;
        final Location spawnLocation;
        Vector velocity;
        int lifetime;
        int age;

        final Vector3f translationOffset = new Vector3f();
        final Vector3f scaleBias = new Vector3f();

        final boolean movesViaTransform;
        @Nullable DisplayTransformTrack track;

        @Nullable Vector3f lastScale;

        int blockPhase;
        int itemPhase;

        @Nullable List<Segment<Transformation>> transformSchedule;
        int transformIndex;

        @Nullable List<Segment<BlockData>> blockSchedule;
        int blockIndex;

        @Nullable List<Segment<ItemStack>> itemSchedule;
        int itemIndex;

        @Nullable Vector3fc spinAxis;
        double spinAngle;
        double spinRadiansPerTick;

        Emitted(FakeEntity entity, Location spawnLocation, Vector velocity, int lifetime, boolean movesViaTransform) {
            this.entity = entity;
            this.spawnLocation = spawnLocation;
            this.velocity = velocity;
            this.lifetime = lifetime;
            this.movesViaTransform = movesViaTransform;
        }

        @Override public int age() {
            return age;
        }

        @Override public int lifetime() {
            return lifetime;
        }

        @Override public @NotNull Location worldLocation() {
            Location l = spawnLocation.clone();
            l.add(translationOffset.x(), translationOffset.y(), translationOffset.z());
            return l;
        }

        @Override public @NotNull Vector3fc translationOffset() {
            return translationOffset;
        }

        @Override public @NotNull Vector velocity() {
            return velocity.clone();
        }

        @Override public @NotNull Vector3fc currentScale() {
            return lastScale != null ? lastScale : IDENTITY_SCALE;
        }
    }

    private final List<Emitted> live = new ArrayList<>();

    @Override
    protected void emit(@NotNull Location point, @NotNull Vector velocity) {
        if (live.size() >= settings.getLiveCap())
            return;

        EntityType type = settings.getDisplayType();
        Object data = settings.getDisplayData();

        FakeEntity entity = CompatibilityAPI.getCompatibility().getEntityCompatibility()
            .generateFakeDisplay(point, type, data);
        FakeDisplayEntity display = (FakeDisplayEntity) entity;

        boolean hasTransformTrack = settings.getTranslation() != null
            || settings.getScale() != null
            || settings.getRotation() != null;
        boolean accelActive = settings.getAcceleration().lengthSquared() > 1e-12f;
        boolean spinActive = settings.getSpinAxis() != null && settings.getSpinRadiansPerTick() != 0;
        boolean movesViaTransform = velocity.lengthSquared() > 1e-12
            || settings.getVelocity() != null
            || accelActive
            || spinActive;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int lifetime = settings.getEmittedLifetimeTicks();
        int jitter = settings.getLifetimeJitterTicks();
        if (jitter > 0)
            lifetime = Math.max(1, lifetime + rng.nextInt(2 * jitter + 1) - jitter);

        Emitted em = new Emitted(entity, point.clone(), velocity.clone(), lifetime, movesViaTransform);

        Vector3fc sj = settings.getScaleJitter();
        if (sj.x() != 0 || sj.y() != 0 || sj.z() != 0) {
            em.scaleBias.set(
                (float) ((rng.nextDouble() * 2 - 1) * sj.x()),
                (float) ((rng.nextDouble() * 2 - 1) * sj.y()),
                (float) ((rng.nextDouble() * 2 - 1) * sj.z()));
        }

        int phaseJitter = settings.getCyclePhaseJitterTicks();
        if (phaseJitter > 0) {
            em.blockPhase = rng.nextInt(phaseJitter + 1);
            em.itemPhase = rng.nextInt(phaseJitter + 1);
        }

        if (spinActive) {
            em.spinAxis = settings.getSpinAxis();
            em.spinRadiansPerTick = settings.getSpinRadiansPerTick();
        }

        if (hasTransformTrack) {
            em.track = new DisplayTransformTrack(
                settings.getTranslation(),
                settings.getScale(),
                settings.getRotation());
        }

        if (movesViaTransform) {
            display.setInterpolationDuration(0);
            display.setInterpolationDelay(0);
            pushTransformation(display, em, 0, null);
        } else if (em.track != null) {
            display.setInterpolationDuration(0);
            display.setInterpolationDelay(0);
            pushTransformation(display, em, 0, em.track.valueAt(0));

            em.transformSchedule = em.track.schedule();
            if (!em.transformSchedule.isEmpty()) {
                Segment<Transformation> seg = em.transformSchedule.get(0);
                display.setInterpolationDuration(seg.interpolationDuration());
                display.setInterpolationDelay(0);
                pushTransformation(display, em, 0, seg.value());
                em.transformIndex = 1;
            }
        } else if (em.scaleBias.lengthSquared() > 0) {
            display.setInterpolationDuration(0);
            display.setInterpolationDelay(0);
            pushTransformation(display, em, 0, null);
        }

        Transition<BlockData> blockCycle = settings.getBlockCycle();
        if (blockCycle != null) {
            display.setBlock(blockCycle.evaluateAtTick(em.blockPhase));
            em.blockSchedule = TransitionPlayback.sample(blockCycle);
            em.blockIndex = advanceSchedulePastPhase(em.blockSchedule, em.blockPhase);
        }

        Transition<ItemStack> itemCycle = settings.getItemCycle();
        if (itemCycle != null) {
            display.setItem(itemCycle.evaluateAtTick(em.itemPhase));
            em.itemSchedule = TransitionPlayback.sample(itemCycle);
            em.itemIndex = advanceSchedulePastPhase(em.itemSchedule, em.itemPhase);
        }

        entity.updateMeta();
        entity.show();
        live.add(em);
    }

    private static <T> int advanceSchedulePastPhase(List<Segment<T>> schedule, int phase) {
        int i = 0;
        while (i < schedule.size() && schedule.get(i).tickOffset() <= phase)
            i++;
        return i;
    }

    private static void pushTransformation(FakeDisplayEntity display, Emitted em, int tick, @Nullable Transformation precomputed) {
        Transformation base = precomputed != null
            ? precomputed
            : (em.track != null ? em.track.valueAt(tick) : null);

        Vector3f translation = base != null
            ? new Vector3f(base.getTranslation()).add(em.translationOffset)
            : new Vector3f(em.translationOffset);

        Vector3f scale = base != null
            ? new Vector3f(base.getScale()).add(em.scaleBias)
            : new Vector3f(IDENTITY_SCALE).add(em.scaleBias);
        // Clamp non-negative: jitter can drive any axis below zero, which flips winding order
        // on the client and inverts back-face culling. Invisible (==0) is the lesser evil.
        if (scale.x < 0) scale.x = 0;
        if (scale.y < 0) scale.y = 0;
        if (scale.z < 0) scale.z = 0;

        Quaternionf left;
        if (em.spinAxis != null && em.spinAngle != 0) {
            Quaternionf spin = new Quaternionf().rotationAxis((float) em.spinAngle, em.spinAxis);
            left = base != null
                ? new Quaternionf(base.getLeftRotation()).mul(spin)
                : spin;
        } else {
            left = base != null ? new Quaternionf(base.getLeftRotation()) : new Quaternionf();
        }

        Quaternionf right = base != null ? new Quaternionf(base.getRightRotation()) : new Quaternionf();

        em.lastScale = scale;
        display.setTransformation(new Transformation(translation, left, scale, right));
    }

    @Override
    protected void tickAlive() {
        Transition<Vector> velocityTrans = settings.getVelocity();
        Vector3fc accel = settings.getAcceleration();
        double drag = settings.getDrag();
        Predicate<EmittedDisplay> killWhen = settings.getKillWhen();
        Consumer<EmittedDisplay> onKilled = settings.getOnKilled();

        for (Iterator<Emitted> it = live.iterator(); it.hasNext();) {
            Emitted em = it.next();

            if (em.age >= em.lifetime) {
                em.entity.remove();
                it.remove();
                continue;
            }

            if (killWhen != null && killWhen.test(em)) {
                if (onKilled != null) onKilled.accept(em);
                em.entity.remove();
                it.remove();
                continue;
            }

            FakeDisplayEntity display = (FakeDisplayEntity) em.entity;
            boolean pushedMeta = false;

            if (em.movesViaTransform) {
                if (em.age > 0) {
                    if (velocityTrans != null)
                        em.velocity = velocityTrans.evaluateAtTick(em.age).clone();
                    if (accel.lengthSquared() > 1e-12f)
                        em.velocity.add(new Vector(accel.x(), accel.y(), accel.z()));
                    if (drag > 0)
                        em.velocity.multiply(1.0 - drag);

                    em.translationOffset.add(
                        (float) em.velocity.getX(),
                        (float) em.velocity.getY(),
                        (float) em.velocity.getZ());

                    em.spinAngle += em.spinRadiansPerTick;

                    display.setInterpolationDuration(1);
                    display.setInterpolationDelay(0);
                    pushTransformation(display, em, em.age, null);
                    pushedMeta = true;
                }
            } else if (em.transformSchedule != null) {
                while (em.transformIndex < em.transformSchedule.size()
                    && em.transformSchedule.get(em.transformIndex).tickOffset() <= em.age) {
                    Segment<Transformation> seg = em.transformSchedule.get(em.transformIndex++);
                    display.setInterpolationDuration(seg.interpolationDuration());
                    display.setInterpolationDelay(0);
                    pushTransformation(display, em, em.age, seg.value());
                    pushedMeta = true;
                }
            }

            if (em.blockSchedule != null) {
                int target = em.age + em.blockPhase;
                while (em.blockIndex < em.blockSchedule.size()
                    && em.blockSchedule.get(em.blockIndex).tickOffset() <= target) {
                    display.setBlock(em.blockSchedule.get(em.blockIndex++).value());
                    pushedMeta = true;
                }
            }
            if (em.itemSchedule != null) {
                int target = em.age + em.itemPhase;
                while (em.itemIndex < em.itemSchedule.size()
                    && em.itemSchedule.get(em.itemIndex).tickOffset() <= target) {
                    display.setItem(em.itemSchedule.get(em.itemIndex++).value());
                    pushedMeta = true;
                }
            }

            if (pushedMeta)
                em.entity.updateMeta();

            em.age++;
        }
    }

    @Override
    protected boolean hasLiveItems() {
        return !live.isEmpty();
    }

    @Override
    public void remove() {
        for (Emitted em : live)
            em.entity.remove();
        live.clear();
    }
}
