package me.deecaad.core.compatibility.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FakeDisplayEntity_1_21_R7 extends FakeEntity_1_21_R7 implements FakeDisplayEntity {

    public FakeDisplayEntity_1_21_R7(@NotNull Location location, @NotNull EntityType type, @Nullable Object data) {
        super(location, type, data);

        if (type != EntityType.BLOCK_DISPLAY && type != EntityType.ITEM_DISPLAY && type != EntityType.TEXT_DISPLAY)
            throw new IllegalArgumentException("Not a display entity type: " + type);

        // Block and item data are applied in the parent's entity-creation switch. Text data is
        // applied here since it requires the Bukkit-side Component conversion.
        if (type == EntityType.TEXT_DISPLAY && data != null)
            setText(data instanceof Component component ? component : Component.text(data.toString()));
    }

    private Display display() {
        return (Display) entity.getBukkitEntity();
    }

    @Override
    public void setTransformation(@NotNull Transformation transformation) {
        display().setTransformation(transformation);
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        display().setInterpolationDuration(ticks);
    }

    @Override
    public void setInterpolationDelay(int ticks) {
        display().setInterpolationDelay(ticks);
    }

    @Override
    public void setTeleportDuration(int ticks) {
        display().setTeleportDuration(ticks);
    }

    @Override
    public void setBillboard(@NotNull Display.Billboard billboard) {
        display().setBillboard(billboard);
    }

    @Override
    public void setBrightness(@Nullable Display.Brightness brightness) {
        display().setBrightness(brightness);
    }

    @Override
    public void setViewRange(float range) {
        display().setViewRange(range);
    }

    @Override
    public void setShadowRadius(float radius) {
        display().setShadowRadius(radius);
    }

    @Override
    public void setShadowStrength(float strength) {
        display().setShadowStrength(strength);
    }

    @Override
    public void setDisplayWidth(float width) {
        display().setDisplayWidth(width);
    }

    @Override
    public void setDisplayHeight(float height) {
        display().setDisplayHeight(height);
    }

    @Override
    public void setGlowColorOverride(@Nullable Color color) {
        display().setGlowColorOverride(color);
    }

    @Override
    public void setBlock(@NotNull BlockData block) {
        FakeDisplayEntity.ensureType(type, EntityType.BLOCK_DISPLAY);
        ((BlockDisplay) entity.getBukkitEntity()).setBlock(block);
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        FakeDisplayEntity.ensureType(type, EntityType.ITEM_DISPLAY);
        ((ItemDisplay) entity.getBukkitEntity()).setItemStack(item);
    }

    @Override
    public void setItemDisplayTransform(@NotNull ItemDisplay.ItemDisplayTransform transform) {
        FakeDisplayEntity.ensureType(type, EntityType.ITEM_DISPLAY);
        ((ItemDisplay) entity.getBukkitEntity()).setItemDisplayTransform(transform);
    }

    @Override
    public void setText(@NotNull Component text) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).text(text);
    }

    @Override
    public void setLineWidth(int width) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setLineWidth(width);
    }

    @Override
    public void setBackgroundColor(@Nullable Color color) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setBackgroundColor(color);
    }

    @Override
    public void setTextOpacity(byte opacity) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setTextOpacity(opacity);
    }

    @Override
    public void setAlignment(@NotNull TextDisplay.TextAlignment alignment) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setAlignment(alignment);
    }

    @Override
    public void setSeeThrough(boolean seeThrough) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setSeeThrough(seeThrough);
    }

    @Override
    public void setShadowed(boolean shadowed) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setShadowed(shadowed);
    }

    @Override
    public void setDefaultBackground(boolean defaultBackground) {
        FakeDisplayEntity.ensureType(type, EntityType.TEXT_DISPLAY);
        ((TextDisplay) entity.getBukkitEntity()).setDefaultBackground(defaultBackground);
    }
}
