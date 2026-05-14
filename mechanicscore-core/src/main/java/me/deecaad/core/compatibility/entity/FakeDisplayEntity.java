package me.deecaad.core.compatibility.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The packet based display entity API ({@link EntityType#BLOCK_DISPLAY},
 * {@link EntityType#ITEM_DISPLAY}, or {@link EntityType#TEXT_DISPLAY}).
 *
 * <p>
 * After modifying any of the properties defined here, you must call
 * {@link FakeEntity#updateMeta()} to flush the changes to viewers.
 *
 * <p>
 * The type-specific methods ({@code setBlock}, {@code setItem*}, {@code setText*}) throw an
 * {@link IllegalStateException} when called on a display entity of the wrong
 * {@link FakeEntity#getType() type}.
 */
public interface FakeDisplayEntity {

    /**
     * Throws an {@link IllegalStateException} unless <code>actual</code> matches
     * <code>required</code>. Intended for implementations to guard type-specific methods.
     *
     * @param actual The entity's actual type.
     * @param required The type required to call the method.
     */
    static void ensureType(@NotNull EntityType actual, @NotNull EntityType required) {
        if (actual != required)
            throw new IllegalStateException("Cannot use this method on a " + actual + " (requires " + required + ")");
    }

    /**
     * Sets the affine transformation applied to this display: translation, scale, and the left/right
     * rotation quaternions. This is the "display matrix" used to position, scale, and rotate the
     * rendered block/item/text relative to the entity's location.
     *
     * <p>
     * {@link Transformation} is composed of JOML types ({@link org.joml.Vector3f},
     * {@link org.joml.Quaternionf}), so a JOML {@link org.joml.Matrix4f} can be decomposed and
     * passed in directly for arbitrary matrix transformations.
     *
     * @param transformation The non-null transformation.
     */
    void setTransformation(@NotNull Transformation transformation);

    /**
     * Sets the duration (in ticks) over which {@link #setTransformation(Transformation)} changes are
     * interpolated. Use <code>0</code> to apply transformations instantly.
     *
     * @param ticks The interpolation duration in ticks.
     */
    void setInterpolationDuration(int ticks);

    /**
     * Sets the delay (in ticks) before a transformation interpolation begins.
     *
     * @param ticks The interpolation delay in ticks.
     */
    void setInterpolationDelay(int ticks);

    /**
     * Sets the duration (in ticks) over which position changes (move/teleport packets) are
     * interpolated client-side. Use <code>0</code> to disable position interpolation.
     *
     * @param ticks The teleport duration in ticks.
     */
    void setTeleportDuration(int ticks);

    /**
     * Sets the billboard mode, controlling which axes the display pivots on to face the viewer.
     *
     * @param billboard The non-null billboard mode.
     */
    void setBillboard(@NotNull Display.Billboard billboard);

    /**
     * Sets the brightness override (block light + sky light). Use <code>null</code> to let the
     * display use the brightness of the block it occupies.
     *
     * @param brightness The nullable brightness override.
     */
    void setBrightness(@Nullable Display.Brightness brightness);

    /**
     * Sets the view range multiplier. A value of <code>1.0</code> is the default range; larger
     * values make the display visible from further away.
     *
     * @param range The view range multiplier.
     */
    void setViewRange(float range);

    /**
     * Sets the radius of the shadow rendered beneath the display. Use <code>0</code> for no shadow.
     *
     * @param radius The shadow radius.
     */
    void setShadowRadius(float radius);

    /**
     * Sets the strength (opacity falloff) of the shadow rendered beneath the display.
     *
     * @param strength The shadow strength.
     */
    void setShadowStrength(float strength);

    /**
     * Sets the width of the display's culling bounding box. Use <code>0</code> to disable culling.
     *
     * @param width The culling box width.
     */
    void setDisplayWidth(float width);

    /**
     * Sets the height of the display's culling bounding box. Use <code>0</code> to disable culling.
     *
     * @param height The culling box height.
     */
    void setDisplayHeight(float height);

    /**
     * Sets the glow color override used when the entity is glowing (see
     * {@link FakeEntity#setGlowing(boolean)}). Use <code>null</code> to use the default team-based
     * color.
     *
     * @param color The nullable glow color override.
     */
    void setGlowColorOverride(@Nullable Color color);

    /**
     * Sets the block shown by this display. Only valid for {@link EntityType#BLOCK_DISPLAY}.
     *
     * @param block The non-null block data.
     * @throws IllegalStateException If this is not a block display.
     */
    void setBlock(@NotNull BlockData block);

    /**
     * Sets the item shown by this display. Only valid for {@link EntityType#ITEM_DISPLAY}.
     *
     * @param item The nullable item stack.
     * @throws IllegalStateException If this is not an item display.
     */
    void setItem(@Nullable ItemStack item);

    /**
     * Sets the transform context (e.g. {@code GUI}, {@code FIXED}, {@code THIRD_PERSON_LEFT_HAND})
     * used to render the item. Only valid for {@link EntityType#ITEM_DISPLAY}.
     *
     * @param transform The non-null item display transform.
     * @throws IllegalStateException If this is not an item display.
     */
    void setItemDisplayTransform(@NotNull ItemDisplay.ItemDisplayTransform transform);

    /**
     * Sets the text shown by this display. Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param text The non-null text component.
     * @throws IllegalStateException If this is not a text display.
     */
    void setText(@NotNull Component text);

    /**
     * Sets the maximum line width before text wraps. Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param width The line width.
     * @throws IllegalStateException If this is not a text display.
     */
    void setLineWidth(int width);

    /**
     * Sets the background color behind the text. Use <code>null</code> for the default background.
     * Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param color The nullable background color.
     * @throws IllegalStateException If this is not a text display.
     */
    void setBackgroundColor(@Nullable Color color);

    /**
     * Sets the opacity of the text (<code>-1</code> for fully opaque, otherwise <code>0-255</code>).
     * Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param opacity The text opacity.
     * @throws IllegalStateException If this is not a text display.
     */
    void setTextOpacity(byte opacity);

    /**
     * Sets the text alignment. Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param alignment The non-null text alignment.
     * @throws IllegalStateException If this is not a text display.
     */
    void setAlignment(@NotNull TextDisplay.TextAlignment alignment);

    /**
     * Sets whether the text is visible through blocks. Only valid for
     * {@link EntityType#TEXT_DISPLAY}.
     *
     * @param seeThrough true to render through blocks.
     * @throws IllegalStateException If this is not a text display.
     */
    void setSeeThrough(boolean seeThrough);

    /**
     * Sets whether the text has a drop shadow. Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param shadowed true to render a drop shadow.
     * @throws IllegalStateException If this is not a text display.
     */
    void setShadowed(boolean shadowed);

    /**
     * Sets whether the text uses the default background color (overriding
     * {@link #setBackgroundColor(Color)}). Only valid for {@link EntityType#TEXT_DISPLAY}.
     *
     * @param defaultBackground true to use the default background.
     * @throws IllegalStateException If this is not a text display.
     */
    void setDefaultBackground(boolean defaultBackground);
}
