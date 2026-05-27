package me.deecaad.core.emitter;

import me.deecaad.core.transition.Transition;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * {@link EmitterSettings} for a {@link DisplayEntityEmitter}. Adds the display type, initial data,
 * per-entity transitions (transform/velocity/cycles), physics (acceleration, drag), per-entity
 * randomization (lifetime/scale/cycle phase), spin, and optional kill hooks.
 */
public final class DisplayEntityEmitterSettings extends EmitterSettings {

    private static final Vector3f ZERO = new Vector3f();

    private final EntityType displayType;
    private final Object displayData;
    private final @Nullable Transition<Vector3f> translation;
    private final @Nullable Transition<Vector3f> scale;
    private final @Nullable Transition<Quaterniond> rotation;
    private final @Nullable Transition<Vector> velocity;
    private final @Nullable Transition<BlockData> blockCycle;
    private final @Nullable Transition<ItemStack> itemCycle;

    private final Vector3f acceleration;
    private final double drag;

    private final int lifetimeJitterTicks;
    private final Vector3f scaleJitter;
    private final int cyclePhaseJitterTicks;

    private final @Nullable Vector3f spinAxis;
    private final double spinRadiansPerTick;

    private final @Nullable Predicate<EmittedDisplay> killWhen;
    private final @Nullable Consumer<EmittedDisplay> onKilled;

    private DisplayEntityEmitterSettings(@NotNull Builder b) {
        super(b);
        this.displayType = Objects.requireNonNull(b.displayType, "displayType required");
        if (displayType != EntityType.BLOCK_DISPLAY
            && displayType != EntityType.ITEM_DISPLAY
            && displayType != EntityType.TEXT_DISPLAY) {
            throw new IllegalArgumentException("displayType must be a display entity, got " + displayType);
        }
        this.displayData = b.displayData;
        this.translation = b.translation;
        this.scale = b.scale;
        this.rotation = b.rotation;
        this.velocity = b.velocity;
        this.blockCycle = b.blockCycle;
        this.itemCycle = b.itemCycle;

        this.acceleration = new Vector3f(b.acceleration);
        if (b.drag < 0.0 || b.drag > 1.0)
            throw new IllegalArgumentException("drag must be in [0, 1], got " + b.drag);
        this.drag = b.drag;

        if (b.lifetimeJitterTicks < 0)
            throw new IllegalArgumentException("lifetimeJitterTicks must be >= 0, got " + b.lifetimeJitterTicks);
        if (b.lifetimeJitterTicks >= b.emittedLifetimeTicks)
            throw new IllegalArgumentException(
                "lifetimeJitterTicks (" + b.lifetimeJitterTicks + ") must be < emittedLifetimeTicks ("
                    + b.emittedLifetimeTicks + ") so the minimum lifetime stays >= 1");
        this.lifetimeJitterTicks = b.lifetimeJitterTicks;
        this.scaleJitter = new Vector3f(b.scaleJitter);
        if (b.cyclePhaseJitterTicks < 0)
            throw new IllegalArgumentException("cyclePhaseJitterTicks must be >= 0, got " + b.cyclePhaseJitterTicks);
        this.cyclePhaseJitterTicks = b.cyclePhaseJitterTicks;

        if (b.spinAxis != null) {
            if (b.spinAxis.lengthSquared() < 1e-12f)
                throw new IllegalArgumentException("spinAxis must be non-zero");
            this.spinAxis = new Vector3f(b.spinAxis).normalize();
        } else {
            this.spinAxis = null;
        }
        this.spinRadiansPerTick = b.spinRadiansPerTick;

        int lifetime = getEmittedLifetimeTicks();
        validateTransitionDuration("translation", translation, lifetime);
        validateTransitionDuration("scale", scale, lifetime);
        validateTransitionDuration("rotation", rotation, lifetime);
        validateTransitionDuration("velocity", velocity, lifetime);

        this.killWhen = b.killWhen;
        this.onKilled = b.onKilled;
    }

    private static void validateTransitionDuration(String name, @Nullable Transition<?> t, int lifetime) {
        if (t != null && t.getDurationTicks() != lifetime)
            throw new IllegalArgumentException(
                name + " transition durationTicks (" + t.getDurationTicks() + ") must equal emittedLifetimeTicks (" + lifetime + ")");
    }

    public @NotNull EntityType getDisplayType() {
        return displayType;
    }

    public @Nullable Object getDisplayData() {
        return displayData;
    }

    public @Nullable Transition<Vector3f> getTranslation() {
        return translation;
    }

    public @Nullable Transition<Vector3f> getScale() {
        return scale;
    }

    public @Nullable Transition<Quaterniond> getRotation() {
        return rotation;
    }

    public @Nullable Transition<Vector> getVelocity() {
        return velocity;
    }

    public @Nullable Transition<BlockData> getBlockCycle() {
        return blockCycle;
    }

    public @Nullable Transition<ItemStack> getItemCycle() {
        return itemCycle;
    }

    public @NotNull Vector3fc getAcceleration() {
        return acceleration;
    }

    public double getDrag() {
        return drag;
    }

    public int getLifetimeJitterTicks() {
        return lifetimeJitterTicks;
    }

    public @NotNull Vector3fc getScaleJitter() {
        return scaleJitter;
    }

    public int getCyclePhaseJitterTicks() {
        return cyclePhaseJitterTicks;
    }

    public @Nullable Vector3fc getSpinAxis() {
        return spinAxis;
    }

    public double getSpinRadiansPerTick() {
        return spinRadiansPerTick;
    }

    public @Nullable Predicate<EmittedDisplay> getKillWhen() {
        return killWhen;
    }

    public @Nullable Consumer<EmittedDisplay> getOnKilled() {
        return onKilled;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder extends EmitterSettings.Builder<Builder> {

        private EntityType displayType;
        private @Nullable Object displayData;
        private @Nullable Transition<Vector3f> translation;
        private @Nullable Transition<Vector3f> scale;
        private @Nullable Transition<Quaterniond> rotation;
        private @Nullable Transition<Vector> velocity;
        private @Nullable Transition<BlockData> blockCycle;
        private @Nullable Transition<ItemStack> itemCycle;

        private Vector3f acceleration = new Vector3f(ZERO);
        private double drag = 0.0;

        private int lifetimeJitterTicks = 0;
        private Vector3f scaleJitter = new Vector3f(ZERO);
        private int cyclePhaseJitterTicks = 0;

        private @Nullable Vector3f spinAxis;
        private double spinRadiansPerTick = 0.0;

        private @Nullable Predicate<EmittedDisplay> killWhen;
        private @Nullable Consumer<EmittedDisplay> onKilled;

        @Override protected @NotNull Builder self() {
            return this;
        }

        @Override public @NotNull DisplayEntityEmitterSettings build() {
            return new DisplayEntityEmitterSettings(this);
        }

        public @NotNull Builder displayType(@NotNull EntityType displayType) {
            this.displayType = displayType;
            return this;
        }

        public @NotNull Builder displayData(@Nullable Object displayData) {
            this.displayData = displayData;
            return this;
        }

        public @NotNull Builder translation(@Nullable Transition<Vector3f> translation) {
            this.translation = translation;
            return this;
        }

        public @NotNull Builder scale(@Nullable Transition<Vector3f> scale) {
            this.scale = scale;
            return this;
        }

        public @NotNull Builder rotation(@Nullable Transition<Quaterniond> rotation) {
            this.rotation = rotation;
            return this;
        }

        public @NotNull Builder velocity(@Nullable Transition<Vector> velocity) {
            this.velocity = velocity;
            return this;
        }

        public @NotNull Builder blockCycle(@Nullable Transition<BlockData> blockCycle) {
            this.blockCycle = blockCycle;
            return this;
        }

        public @NotNull Builder itemCycle(@Nullable Transition<ItemStack> itemCycle) {
            this.itemCycle = itemCycle;
            return this;
        }

        public @NotNull Builder acceleration(@NotNull Vector3fc acceleration) {
            this.acceleration = new Vector3f(acceleration);
            return this;
        }

        public @NotNull Builder drag(double drag) {
            this.drag = drag;
            return this;
        }

        public @NotNull Builder lifetimeJitterTicks(int lifetimeJitterTicks) {
            this.lifetimeJitterTicks = lifetimeJitterTicks;
            return this;
        }

        public @NotNull Builder scaleJitter(@NotNull Vector3fc scaleJitter) {
            this.scaleJitter = new Vector3f(scaleJitter);
            return this;
        }

        public @NotNull Builder cyclePhaseJitterTicks(int cyclePhaseJitterTicks) {
            this.cyclePhaseJitterTicks = cyclePhaseJitterTicks;
            return this;
        }

        /**
         * Axis around which each emitted entity spins; normalized at build time. Combined with
         * {@link #spinRadiansPerTick(double)} to drive continuous rotation on top of any authored
         * rotation transition.
         */
        public @NotNull Builder spinAxis(@Nullable Vector3fc spinAxis) {
            this.spinAxis = spinAxis == null ? null : new Vector3f(spinAxis);
            return this;
        }

        public @NotNull Builder spinRadiansPerTick(double spinRadiansPerTick) {
            this.spinRadiansPerTick = spinRadiansPerTick;
            return this;
        }

        /**
         * Optional predicate evaluated each tick on every live emission. When it returns {@code true}
         * the entity is destroyed (and {@link #onKilled(Consumer)} fires if set). When {@code null},
         * the kill path is bypassed entirely with zero per-tick cost.
         */
        public @NotNull Builder killWhen(@Nullable Predicate<EmittedDisplay> killWhen) {
            this.killWhen = killWhen;
            return this;
        }

        public @NotNull Builder onKilled(@Nullable Consumer<EmittedDisplay> onKilled) {
            this.onKilled = onKilled;
            return this;
        }
    }
}
