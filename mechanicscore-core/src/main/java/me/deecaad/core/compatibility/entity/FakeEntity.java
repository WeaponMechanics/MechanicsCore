package me.deecaad.core.compatibility.entity;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.tick.TransformTree;
import me.deecaad.core.utils.Transform;
import me.deecaad.core.utils.TransformLike;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

import java.util.function.Consumer;

import static me.deecaad.core.utils.NumberUtil.square;

/**
 * Defines a packet based {@link org.bukkit.entity.Entity} with no server functions. Faked entities
 * are not ticked, rendered, moved, or in any other way "handled" by the server.
 *
 * <p>
 * Fake entities are usually used for visual effects, since faked entities can control appearances
 * per player. After changing a visual effect (metadata + display name + gravity + etc), a metadata
 * packet must be sent using {@link #updateMeta()}.
 */
public abstract class FakeEntity implements TransformLike {

    // Use these constants to change an entity's metadata. Note that '2' is
    // intentionally missing due to it being unused in new MC versions.
    public static final int FIRE_FLAG = 0;
    public static final int SNEAKING_FLAG = 1;
    public static final int SPRINTING_FLAG = 3;
    public static final int SWIMMING_FLAG = 4;
    public static final int INVISIBLE_FLAG = 5;
    public static final int GLOWING_FLAG = 6;
    public static final int GLIDING_FLAG = 7;

    protected final EntityType type;
    protected final Transform transform;
    protected Location location;
    protected Location offset;
    protected Vector motion;
    protected int cache = -1;
    private boolean forceTeleport;
    private TransformTree tickTree;

    public FakeEntity(@NotNull Location location, @NotNull EntityType type) {
        this.type = type;
        this.location = new Location(location.getWorld(), 0, 0, 0);
        this.motion = new Vector();
        this.transform = new Transform(this,
            new Vector(location.getX(), location.getY(), location.getZ()),
            Transform.fromYawPitch(location.getYaw(), location.getPitch()));
    }

    /**
     * Returns the {@link Transform} that drives this entity's position and rotation. The transform
     * is the source of truth -- attach it as a child of another transform to build hierarchies of
     * fake entities that move together.
     *
     * @return The non-null transform.
     */
    @Override
    public @NotNull Transform getTransform() {
        return transform;
    }

    /**
     * Materializes the current world {@link #getTransform() transform} into move/teleport packets.
     * Called automatically when this entity (or a parent) is moved.
     */
    @Override
    public final void applyTransform() {
        Vector world = transform.getPosition();
        Quaterniond rotation = transform.getRotation();
        double x = world.getX();
        double y = world.getY();
        double z = world.getZ();
        float yaw = Transform.yaw(rotation);
        float pitch = Transform.pitch(rotation);

        if (offset != null) {
            x += offset.getX();
            y += offset.getY();
            z += offset.getZ();
            yaw += offset.getYaw();
            pitch += offset.getPitch();
        }

        boolean raw = forceTeleport;
        forceTeleport = false;
        double lengthSquared = raw ? 0.0 : square(x - location.getX()) + square(y - location.getY()) + square(z - location.getZ());

        if (raw || lengthSquared == 0.0 || lengthSquared > 64.0) {
            setLocation(x, y, z, yaw, pitch);
            setPositionRaw(x, y, z, yaw, pitch);
        } else {
            setPositionRotation(x - location.getX(), y - location.getY(), z - location.getZ(), yaw, pitch);
            setLocation(x, y, z, yaw, pitch);
        }

        if (type == EntityType.ARMOR_STAND)
            updateMeta();
    }

    /**
     * Starts driving this entity's transform tree once per tick. Required for hierarchies that
     * change continuously (children of an {@link me.deecaad.core.utils.EntityTransform}, animated
     * transforms). Call {@link #stopTicking()} when the entity is removed.
     */
    public void startTicking() {
        if (tickTree != null)
            return;
        tickTree = new TransformTree(this, location);
        MechanicsCore.getInstance().getTickManager().add(tickTree);
    }

    /**
     * Stops the per-tick driver started by {@link #startTicking()}.
     */
    public void stopTicking() {
        if (tickTree != null) {
            tickTree.stop();
            tickTree = null;
        }
    }

    // Cascade helpers for version classes to call from show/remove. They walk the transform
    // subtree, traversing through plain Transform nodes to reach deeper fake entities.

    protected final void showChildren() {
        forEachFakeChild(transform, FakeEntity::show);
    }

    protected final void showChildren(@NotNull Player player) {
        forEachFakeChild(transform, child -> child.show(player));
    }

    protected final void removeChildren() {
        forEachFakeChild(transform, FakeEntity::remove);
    }

    protected final void removeChildren(@NotNull Player player) {
        forEachFakeChild(transform, child -> child.remove(player));
    }

    private static void forEachFakeChild(@NotNull Transform node, @NotNull Consumer<FakeEntity> action) {
        for (TransformLike child : node.getChildren()) {
            if (child instanceof FakeEntity fakeEntity)
                action.accept(fakeEntity);
            else
                forEachFakeChild(child.getTransform(), action);
        }
    }

    public EntityType getType() {
        return type;
    }

    public final boolean isOnFire() {
        return getMeta(FIRE_FLAG);
    }

    public final boolean isGlowing() {
        return getMeta(GLOWING_FLAG);
    }

    public final boolean isInvisible() {
        return getMeta(INVISIBLE_FLAG);
    }

    /**
     * Gets the stored meta-data flag.
     *
     * @param metaFlag The index location of which flag to edit.
     * @return Whether the flag is enabled or disabled.
     * @see #setMeta(int, boolean)
     */
    public abstract boolean getMeta(int metaFlag);

    public final void setOnFire(boolean isOnFire) {
        this.setMeta(FIRE_FLAG, isOnFire);
    }

    public final void setGlowing(boolean isGlowing) {
        this.setMeta(GLOWING_FLAG, isGlowing);
    }

    public final void setInvisible(boolean isInvisible) {
        this.setMeta(INVISIBLE_FLAG, isInvisible);
    }

    /**
     * Sets this entity's meta-data at the given <code>metaFlag</code> index. You can toggle a meta flag
     * by checking {@link #getMeta(int)}. smaller than <code>metaFlag</code> should be one of the flag
     * constants in the {@link FakeEntity} (this) class. After these modifications, call
     * {@link #updateMeta()} to show player's changes.
     *
     * @param metaFlag The index location of which flag to edit.
     * @param isEnabled Whether to enable or disable the flag.
     * @see #getMeta(int)
     */
    public abstract void setMeta(int metaFlag, boolean isEnabled);

    /**
     * Returns the data that was used in the constructor (or in {@link #setData(Object)}). The data will
     * either be an {@link ItemStack}, or a {@link org.bukkit.block.data.BlockData}
     *
     * @return The nullable extra data (block or item).
     */
    @Nullable public abstract Object getData();

    /**
     * Sets the data usually used in the constructor of a {@link FakeEntity}. As of the time of writing,
     * <code>data</code> may only be an {@link ItemStack}, and this method will only have any behavior
     * for armorstands and dropped items.
     *
     * @param data The nullable data.
     */
    public abstract void setData(@Nullable Object data);

    /**
     * Sets the display name of the entity. Use <code>null</code> to remove the any previous display
     * name. Supports color codes using {@link org.bukkit.ChatColor}. After calling this method, use
     * {@link #updateMeta()} to show the information to clients.
     *
     * @param display The nullable entity display-name.
     */
    public abstract void setDisplay(@Nullable String display);

    /**
     * Disables entity gravity. This has no effect server-side, and will not affect
     * motion/position/rotation/anything. Instead, this method tells the client that the entity should
     * not automatically have gravity applied. After calling this method use {@link #updateMeta()} to
     * show the information to clients.
     *
     * @param gravity true -> gravity, false -> no gravity.
     */
    public abstract void setGravity(boolean gravity);

    public double getX() {
        return location.getX();
    }

    public double getY() {
        return location.getY();
    }

    public double getZ() {
        return location.getZ();
    }

    public float getYaw() {
        return location.getYaw();
    }

    public float getPitch() {
        return location.getPitch();
    }

    /**
     * Sets the location offset for this entity. Entity yaw and pitch are also modified based on this
     * value. This offset is applied to entity when any move packet or such is sent to the player.
     *
     * @param offset The vector changing FakeEntity position
     */
    public void setOffset(Location offset) {
        this.offset = offset;
    }

    protected void setLocation(double x, double y, double z, float yaw, float pitch) {
        location.setX(x);
        location.setY(y);
        location.setZ(z);
        location.setYaw(yaw);
        location.setPitch(pitch);
    }

    /**
     * Shorthand for {@link #setMotion(double, double, double)}.
     *
     * @param motion The motion the entity is moving with.
     * @see #setMotion(double, double, double)
     */
    public final void setMotion(@NotNull Vector motion) {
        setMotion(motion.getX(), motion.getY(), motion.getZ());
    }

    /**
     * Sends an entity velocity packet to all players who can see this entity.
     *
     * @param dx The change of position in the x-axis.
     * @param dy The change of position in the y-axis.
     * @param dz The change of position in the z-axis.
     */
    public abstract void setMotion(double dx, double dy, double dz);

    /**
     * Sets the absolute rotation of the entity, updating the {@link #getTransform() transform} and
     * sending the resulting packets to all viewers.
     *
     * @param yaw The absolute yaw rotation of the entity.
     * @param pitch The absolute pitch rotation of the entity.
     */
    public final void setRotation(float yaw, float pitch) {
        transform.setRotation(Transform.fromYawPitch(yaw, pitch));
    }

    /**
     * Shorthand for calling {@link #setPosition(double, double, double, float, float)}.
     *
     * @param pos The non-null new position of the entity.
     * @param yaw The yaw to set the entity at.
     * @param pitch The pitch to set the entity at.
     */
    public final void setPosition(@NotNull Vector pos, float yaw, float pitch) {
        setPosition(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, false);
    }

    /**
     * Shorthand for calling {@link #setPosition(double, double, double, float, float, boolean)}.
     *
     * @param x The new position on the x-axis.
     * @param y The new position on the y-axis.
     * @param z The new position on the z-axis.
     */
    public final void setPosition(double x, double y, double z) {
        setPosition(x, y, z, getYaw(), getPitch(), false);
    }

    /**
     * Shorthand for calling {@link #setPosition(double, double, double, float, float, boolean)}.
     *
     * @param x The new position on the x-axis.
     * @param y The new position on the y-axis.
     * @param z The new position on the z-axis.
     * @param yaw The yaw to set the entity at.
     * @param pitch The pitch to set the entity at.
     */
    public final void setPosition(double x, double y, double z, float yaw, float pitch) {
        setPosition(x, y, z, yaw, pitch, false);
    }

    /**
     * Sets position of this entity. When the new location is within 8 blocks, a move-look packet is
     * sent (using a relative position). Otherwise, a teleport packet is sent (using an absolute
     * position).
     *
     * <p>
     * If you do not want to change the entity's yaw/pitch, you may use {@link #getYaw()} and
     * {@link #getPitch()}.
     *
     * @param x The new position on the x-axis.
     * @param y The new position on the y-axis.
     * @param z The new position on the z-axis.
     * @param yaw The yaw to set the entity at.
     * @param pitch The pitch to set the entity at.
     * @param raw true to always use a teleport packet.
     */
    public final void setPosition(double x, double y, double z, float yaw, float pitch, boolean raw) {
        this.forceTeleport = raw;
        transform.set(new Vector(x, y, z), Transform.fromYawPitch(yaw, pitch));
    }

    // private since nobody should use this method
    private void setPositionRotation(double dx, double dy, double dz, float yaw, float pitch) {
        setPositionRotation((short) (dx * 4096), (short) (dy * 4096), (short) (dz * 4096), convertYaw(yaw), convertPitch(pitch));
    }

    /**
     * Sends an entity move-look packet to all players who can see this entity. Effectively sets the
     * relative position of the entity.
     *
     * <p>
     * This method is protected to prevent accidental/improper usage.
     *
     * @param dx The change of position across the x-axis.
     * @param dy The change of position across the y-axis.
     * @param dz The change of position across the z-axis.
     * @param yaw The absolute yaw rotation of the entity.
     * @param pitch The absolute pitch rotation of the entity.
     */
    protected abstract void setPositionRotation(short dx, short dy, short dz, byte yaw, byte pitch);

    /**
     * Sends an entity teleport packet to all players who can see this entity. Effectively sets the
     * absolute position of the entity.
     *
     * <p>
     * This method is protected to prevent accidental/improper usage.
     *
     * @param x The absolute x position of the entity.
     * @param y The absolute y position of the entity.
     * @param z The absolute z position of the entity.
     * @param yaw The absolute yaw rotation of the entity.
     * @param pitch The absolute pitch rotation of the entity.
     */
    protected abstract void setPositionRaw(double x, double y, double z, float yaw, float pitch);

    protected final byte convertPitch(float degrees) {
        degrees *= 256.0f / 360.0f;
        if (!type.isAlive() && !isDisplay()) {
            return (byte) -degrees;
        }
        return (byte) degrees;
    }

    protected final byte convertYaw(float degrees) {
        degrees *= 256.0f / 360.0f;
        return switch (type) {
            case ARROW -> (byte) -degrees;
            case WITHER_SKULL, ENDER_DRAGON -> (byte) (degrees - 128.0f);
            default -> {
                if (!type.isAlive() && type != EntityType.ARMOR_STAND && !isDisplay()) {
                    yield (byte) (degrees - 64.0f);
                }
                yield (byte) degrees;
            }
        };
    }

    private boolean isDisplay() {
        return type == EntityType.BLOCK_DISPLAY || type == EntityType.ITEM_DISPLAY || type == EntityType.TEXT_DISPLAY;
    }

    /**
     * Shows this entity to all players within range of the entity. Effectively the same as calling
     * {@link #show(Player)} for each player. Sends an Add Entity packet and an Entity Meta packet.
     */
    public abstract void show();

    /**
     * Shows the entity to the given player. Sends an add Entity packet and an Entity Meta packet.
     *
     * @param player The non-null player to show the entity to.
     */
    public abstract void show(@NotNull Player player);

    /**
     * Updates the meta for all players that currently see it. This method should be used after any
     * modifications to entity meta.
     */
    public abstract void updateMeta();

    /**
     * Hides the entity for all players that currently see it. Sends an entity destroy packet. Players
     * will not be able to see position or rotation or meta or velocity changes after calling this
     * method (Unless they are added back using {@link #show(Player)}).
     */
    public abstract void remove();

    /**
     * Hides the entity for the given player. Sends an Entity Destroy packet. The player will not be
     * able to see position or rotation or meta or velocity changes after calling this method (Unless
     * they are added back using {@link #show(Player)}).
     *
     * @param player The non-null player to hide the entity from.
     */
    public abstract void remove(@NotNull Player player);

    /**
     * Plays an entity effect for this entity. Make sure that the given effect can be used for this
     * entity's {@link #getType}.
     *
     * @param effect The non-null effect to play.
     */
    public abstract void playEffect(@NotNull EntityEffect effect);

    /**
     * Sets new item to given equipment slot.
     *
     * @param equipmentSlot the equipment slot to modify
     * @param itemStack the item stack set to slot
     */
    public abstract void setEquipment(@NotNull EquipmentSlot equipmentSlot, @Nullable ItemStack itemStack);

    /**
     * Updates the equipment for all players that currently see it. This method should be used after any
     * modifications to {@link #setEquipment(EquipmentSlot, ItemStack)}.
     */
    public abstract void updateEquipment();
}