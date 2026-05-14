package me.deecaad.core.compatibility.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EntityCompatibility {

    /**
     * Internal use only.
     * <p>
     * Overrides the player's handle's inventory with a proxy that calls the consumer.
     * The consumer will have the old item, the new item, and the modified slot.
     *
     * @param player The player to override
     * @param consumer The callback on inventory modifications
     */
    void injectInventoryConsumer(@NotNull Player player, @NotNull EquipmentChangeConsumer consumer);

    /**
     * Generates a {@link FakeEntity} with the given entity type as a disguise.
     *
     * @param location The non-null starting location of the entity.
     * @param type The non-null type of the entity.
     * @param data The nullable extra data for item/fallingblock/armorstand.
     * @return The fake entity.
     */
    FakeEntity generateFakeEntity(Location location, EntityType type, Object data);

    /**
     * Shorthand for {@link #generateFakeEntity(Location, EntityType, Object)}. Generates a
     * {@link org.bukkit.entity.Item}.
     *
     * @param location The non-null starting location of the entity.
     * @param item The non-null item to show.
     * @return The fake entity.
     */
    default FakeEntity generateFakeEntity(Location location, ItemStack item) {
        return generateFakeEntity(location, EntityType.ITEM, item);
    }

    /**
     * Shorthand for {@link #generateFakeEntity(Location, EntityType, Object)}. Generates a
     * {@link org.bukkit.entity.FallingBlock}.
     *
     * @param location The non-null starting location of the entity.
     * @param block The non-null block state to show.
     * @return The fake entity.
     */
    default FakeEntity generateFakeEntity(Location location, BlockState block) {
        return generateFakeEntity(location, EntityType.FALLING_BLOCK, block);
    }

    /**
     * Generates a packet based {@link FakeDisplayEntity}.
     *
     * @param location The non-null starting location of the entity.
     * @param type The non-null display type ({@link EntityType#BLOCK_DISPLAY},
     *        {@link EntityType#ITEM_DISPLAY}, or {@link EntityType#TEXT_DISPLAY}).
     * @param data The nullable initial data: a {@link BlockData} for block displays, an
     *        {@link ItemStack} for item displays, or a {@link Component} for text displays.
     * @param <T> The returned object is both a {@link FakeEntity} and a {@link FakeDisplayEntity}.
     * @return The fake display entity.
     */
    default <T extends FakeEntity & FakeDisplayEntity> T generateFakeDisplay(Location location, EntityType type, @Nullable Object data) {
        throw new UnsupportedOperationException("Fake display entities are not yet implemented on this server version");
    }

    /**
     * Shorthand for {@link #generateFakeDisplay(Location, EntityType, Object)}. Generates a
     * {@link org.bukkit.entity.BlockDisplay}.
     *
     * @param location The non-null starting location of the entity.
     * @param block The non-null block to show.
     * @param <T> The returned object is both a {@link FakeEntity} and a {@link FakeDisplayEntity}.
     * @return The fake display entity.
     */
    default <T extends FakeEntity & FakeDisplayEntity> T generateFakeBlockDisplay(Location location, BlockData block) {
        return generateFakeDisplay(location, EntityType.BLOCK_DISPLAY, block);
    }

    /**
     * Shorthand for {@link #generateFakeDisplay(Location, EntityType, Object)}. Generates an
     * {@link org.bukkit.entity.ItemDisplay}.
     *
     * @param location The non-null starting location of the entity.
     * @param item The non-null item to show.
     * @param <T> The returned object is both a {@link FakeEntity} and a {@link FakeDisplayEntity}.
     * @return The fake display entity.
     */
    default <T extends FakeEntity & FakeDisplayEntity> T generateFakeItemDisplay(Location location, ItemStack item) {
        return generateFakeDisplay(location, EntityType.ITEM_DISPLAY, item);
    }

    /**
     * Shorthand for {@link #generateFakeDisplay(Location, EntityType, Object)}. Generates a
     * {@link org.bukkit.entity.TextDisplay}.
     *
     * @param location The non-null starting location of the entity.
     * @param text The non-null text to show.
     * @param <T> The returned object is both a {@link FakeEntity} and a {@link FakeDisplayEntity}.
     * @return The fake display entity.
     */
    default <T extends FakeEntity & FakeDisplayEntity> T generateFakeTextDisplay(Location location, Component text) {
        return generateFakeDisplay(location, EntityType.TEXT_DISPLAY, text);
    }

    /**
     * Uses a packet to spawn a fake item in the player's inventory. If the player is in
     * {@link org.bukkit.GameMode#CREATIVE}, then they will be able to grab the item, regardless. This
     * issue isn't present with standard players.
     *
     * @param player The non-null player to see the change
     * @param slot The slot number to set.
     * @param item Which item to replace, or null.
     */
    void setSlot(Player player, EquipmentSlot slot, @Nullable ItemStack item);

    /**
     * Creates a metadata packet for the entity, force updating all metadata. This can be modified by
     * {@link #modifyMetaPacket(Object, EntityMeta, boolean)} to make the entity invisible, glow, etc.
     *
     * @param entity The non-null entity.
     * @return The non-null packet.
     */
    Object generateMetaPacket(Entity entity);

    /**
     * Sets the given entity metadata flag to true/false.
     *
     * @param obj The metadata packet from {@link #generateMetaPacket(Entity)}/
     * @param meta The meta flag you want to change.
     * @param enabled true/false.
     */
    void modifyMetaPacket(Object obj, EntityMeta meta, boolean enabled);

    /**
     * This enum outlines the different flags and their byte location for
     * <a href="https://wiki.vg/Entity_metadata#Entity">EntityMetaData</a>.
     */
    enum EntityMeta {

        FIRE(0), // If the entity is on fire
        SNEAKING(1), // If the entity is sneaking
        UNUSED(2), // If the entity is mounted (no longer used in recent versions)
        SPRINTING(3), // If the entity is running
        SWIMMING(4), // If the entity is swimming
        INVISIBLE(5), // If the entity is invisible
        GLOWING(6), // If the entity is glowing
        GLIDING(7); // If the entity is gliding using an elytra

        private final byte mask;

        EntityMeta(int location) {
            this.mask = (byte) (1 << location);
        }

        public byte getMask() {
            return mask;
        }

        public byte set(byte data, boolean is) {
            return (byte) (is ? data | mask : data & ~(mask));
        }
    }
}
