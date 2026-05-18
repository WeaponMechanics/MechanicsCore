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

import java.util.Objects;

/**
 * {@link EmitterSettings} for a {@link DisplayEntityEmitter}. Adds the display type
 * ({@code BLOCK_DISPLAY}, {@code ITEM_DISPLAY}, or {@code TEXT_DISPLAY}), the initial display data,
 * and per-entity transitions for transform (translation/scale/rotation), velocity, and stepped
 * block/item cycling.
 */
public final class DisplayEntityEmitterSettings extends EmitterSettings {

    private final EntityType displayType;
    private final Object displayData;
    private final @Nullable Transition<Vector3f> translation;
    private final @Nullable Transition<Vector3f> scale;
    private final @Nullable Transition<Quaterniond> rotation;
    private final @Nullable Transition<Vector> velocity;
    private final @Nullable Transition<BlockData> blockCycle;
    private final @Nullable Transition<ItemStack> itemCycle;

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
    }
}
