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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Emits persistent {@link FakeDisplayEntity} packets. Each emitted entity carries its own age,
 * velocity, and a precomputed playback schedule for any per-entity transitions (transform,
 * block-cycle, item-cycle). Entities are detached from the emitter's {@link
 * me.deecaad.core.utils.Transform} tree once spawned so they follow their own world path.
 */
public final class DisplayEntityEmitter extends AbstractEmitter<DisplayEntityEmitterSettings> {

    public DisplayEntityEmitter(@NotNull DisplayEntityEmitterSettings settings) {
        super(settings);
    }

    private static final class Emitted {
        final FakeEntity entity;
        Vector velocity;
        final int lifetime;
        int age;

        @Nullable List<Segment<Transformation>> transformSchedule;
        int transformIndex;

        @Nullable List<Segment<BlockData>> blockSchedule;
        int blockIndex;

        @Nullable List<Segment<ItemStack>> itemSchedule;
        int itemIndex;

        Emitted(FakeEntity entity, Vector velocity, int lifetime) {
            this.entity = entity;
            this.velocity = velocity;
            this.lifetime = lifetime;
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

        Emitted em = new Emitted(entity, velocity.clone(), settings.getItemLifetimeTicks());

        boolean hasTransformTrack = settings.getTranslation() != null
            || settings.getScale() != null
            || settings.getRotation() != null;
        if (hasTransformTrack) {
            DisplayTransformTrack track = new DisplayTransformTrack(
                settings.getTranslation(),
                settings.getScale(),
                settings.getRotation());

            display.setInterpolationDuration(0);
            display.setInterpolationDelay(0);
            display.setTransformation(track.valueAt(0));

            em.transformSchedule = track.schedule();
            if (!em.transformSchedule.isEmpty()) {
                Segment<Transformation> seg = em.transformSchedule.get(0);
                display.setInterpolationDuration(seg.interpolationDuration());
                display.setInterpolationDelay(0);
                display.setTransformation(seg.value());
                em.transformIndex = 1;
            }
        }

        Transition<BlockData> blockCycle = settings.getBlockCycle();
        if (blockCycle != null) {
            display.setBlock(blockCycle.evaluateAtTick(0));
            em.blockSchedule = TransitionPlayback.sample(blockCycle);
            em.blockIndex = !em.blockSchedule.isEmpty() && em.blockSchedule.get(0).tickOffset() == 0 ? 1 : 0;
        }

        Transition<ItemStack> itemCycle = settings.getItemCycle();
        if (itemCycle != null) {
            display.setItem(itemCycle.evaluateAtTick(0));
            em.itemSchedule = TransitionPlayback.sample(itemCycle);
            em.itemIndex = !em.itemSchedule.isEmpty() && em.itemSchedule.get(0).tickOffset() == 0 ? 1 : 0;
        }

        entity.updateMeta();
        entity.show();
        live.add(em);
    }

    @Override
    protected void tickAlive() {
        Transition<Vector> velocityTrans = settings.getVelocity();

        for (Iterator<Emitted> it = live.iterator(); it.hasNext();) {
            Emitted em = it.next();

            if (em.age >= em.lifetime) {
                em.entity.remove();
                it.remove();
                continue;
            }

            if (em.age > 0) {
                if (velocityTrans != null)
                    em.velocity = velocityTrans.evaluateAtTick(em.age).clone();
                Vector pos = em.entity.getTransform().getPosition().add(em.velocity);
                em.entity.getTransform().setPosition(pos);
            }

            boolean pushedMeta = false;
            FakeDisplayEntity display = (FakeDisplayEntity) em.entity;

            if (em.transformSchedule != null) {
                while (em.transformIndex < em.transformSchedule.size()
                    && em.transformSchedule.get(em.transformIndex).tickOffset() <= em.age) {
                    Segment<Transformation> seg = em.transformSchedule.get(em.transformIndex++);
                    display.setInterpolationDuration(seg.interpolationDuration());
                    display.setInterpolationDelay(0);
                    display.setTransformation(seg.value());
                    pushedMeta = true;
                }
            }
            if (em.blockSchedule != null) {
                while (em.blockIndex < em.blockSchedule.size()
                    && em.blockSchedule.get(em.blockIndex).tickOffset() <= em.age) {
                    display.setBlock(em.blockSchedule.get(em.blockIndex++).value());
                    pushedMeta = true;
                }
            }
            if (em.itemSchedule != null) {
                while (em.itemIndex < em.itemSchedule.size()
                    && em.itemSchedule.get(em.itemIndex).tickOffset() <= em.age) {
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
