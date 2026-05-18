package me.deecaad.core.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A node in a transform hierarchy: a local position and rotation that may inherit from a parent.
 * Rotation math is backed by JOML's {@link Quaterniond}.
 *
 * <p>
 * Mutating a transform propagates to its {@code owner} and cascades down to all children, calling
 * {@link TransformLike#applyTransform()} on each.
 */
public class Transform implements TransformLike {

    private Vector localPosition;
    private Quaterniond localRotation;

    private TransformLike parent;
    private final List<TransformLike> children;
    private final TransformLike owner;

    public Transform() {
        this(null);
    }

    public Transform(TransformLike owner) {
        this.children = new ArrayList<>();
        this.localRotation = new Quaterniond();
        this.localPosition = new Vector();
        this.owner = owner != null ? owner : this;
    }

    public Transform(TransformLike owner, @NotNull Vector localPosition, @NotNull Quaterniond localRotation) {
        this(owner);
        this.localPosition = localPosition.clone();
        this.localRotation = new Quaterniond(localRotation).normalize();
    }

    @Override
    public @NotNull Transform getTransform() {
        return this;
    }

    public TransformLike getParent() {
        return parent;
    }

    public TransformLike getChild(int i) {
        return children.get(i);
    }

    public List<TransformLike> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(@NotNull TransformLike child) {
        child.getTransform().setParent(this);
    }

    public void removeChild(@NotNull TransformLike child) {
        if (child.getTransform().getParent() == this)
            child.getTransform().setParent(null);
    }

    public void setParent(TransformLike parent) {
        // Detach from the old parent, baking the current world transform into local space.
        if (this.parent != null) {
            localPosition = getPosition();
            localRotation = getRotation();
            this.parent.getTransform().children.remove(owner);
        }

        // Attach to the new parent, converting the world transform into its local space.
        if (parent != null) {
            this.parent = parent;
            localPosition = toLocalPosition(localPosition);
            localRotation = toLocalRotation(localRotation);
            parent.getTransform().children.add(owner);
        } else {
            this.parent = null;
        }

        propagate();
    }

    public Vector getForward() {
        return rotate(getRotation(), new Vector(0, 0, 1));
    }

    public void setForward(Vector forward) {
        setRotation(lookAt(forward, new Vector(0, 1, 0)));
    }

    public Vector getRight() {
        return rotate(getRotation(), new Vector(-1, 0, 0));
    }

    public void setRight(Vector right) {
        setRotation(new Quaterniond().rotationTo(new Vector3d(-1, 0, 0), toVector3d(right)));
    }

    public Vector getUp() {
        return rotate(getRotation(), new Vector(0, 1, 0));
    }

    public void setUp(Vector up) {
        setRotation(new Quaterniond().rotationTo(new Vector3d(0, 1, 0), toVector3d(up)));
    }

    public Vector getLocalPosition() {
        return localPosition.clone();
    }

    public void setLocalPosition(Vector localPosition) {
        this.localPosition = localPosition.clone();
        propagate();
    }

    public Vector getPosition() {
        if (parent == null)
            return getLocalPosition();

        Transform p = parent.getTransform();
        return p.getPosition().add(rotate(p.getRotation(), localPosition));
    }

    public void setPosition(Location position) {
        setPosition(position.toVector());
    }

    public void setPosition(Vector position) {
        this.localPosition = toLocalPosition(position);
        propagate();
    }

    public Quaterniond getLocalRotation() {
        return new Quaterniond(localRotation);
    }

    public void setLocalRotation(Quaterniond localRotation) {
        this.localRotation = new Quaterniond(localRotation).normalize();
        propagate();
    }

    public Quaterniond getRotation() {
        if (parent == null)
            return getLocalRotation();

        return parent.getTransform().getRotation().mul(localRotation);
    }

    public void setRotation(Quaterniond rotation) {
        this.localRotation = toLocalRotation(rotation);
        propagate();
    }

    public void applyRotation(Quaterniond rotation) {
        localRotation.mul(new Quaterniond(rotation).normalize());
        propagate();
    }

    /**
     * Sets the world position and rotation in a single update (one propagation).
     *
     * @param position The non-null world position.
     * @param rotation The non-null world rotation.
     */
    public void set(@NotNull Vector position, @NotNull Quaterniond rotation) {
        this.localPosition = toLocalPosition(position);
        this.localRotation = toLocalRotation(rotation);
        propagate();
    }

    private Vector toLocalPosition(Vector worldPosition) {
        if (parent == null)
            return worldPosition.clone();

        Transform p = parent.getTransform();
        return rotate(p.getRotation().invert(), worldPosition.clone().subtract(p.getPosition()));
    }

    private Quaterniond toLocalRotation(Quaterniond worldRotation) {
        if (parent == null)
            return new Quaterniond(worldRotation).normalize();

        return parent.getTransform().getRotation().invert().mul(worldRotation).normalize();
    }

    protected void propagate() {
        owner.applyTransform();
        for (TransformLike child : children)
            child.getTransform().propagate();
    }

    /**
     * Walks this transform and its descendants, calling {@link TransformLike#update()} on each.
     * Intended to be invoked once per tick by an external ticking system.
     */
    public void tick() {
        owner.update();
        for (TransformLike child : children)
            child.getTransform().tick();
    }

    /**
     * Builds a rotation that points the local forward axis (+Z) along <code>direction</code>, with
     * the local up axis (+Y) aligned as closely as possible to <code>up</code>. If <code>up</code>
     * is parallel to <code>direction</code>, an arbitrary perpendicular axis is chosen instead.
     *
     * @param direction The direction the local +Z axis should point.
     * @param up The reference up direction.
     * @return A non-null JOML quaternion.
     */
    public static Quaterniond lookAt(Vector direction, Vector up) {
        Vector3d forward = toVector3d(direction);
        if (forward.lengthSquared() < 1e-12)
            return new Quaterniond();
        forward.normalize();

        Vector3d right = toVector3d(up).cross(forward);
        if (right.lengthSquared() < 1e-12) {
            right = new Vector3d(1, 0, 0).cross(forward);
            if (right.lengthSquared() < 1e-12)
                right = new Vector3d(0, 0, 1).cross(forward);
        }
        right.normalize();
        Vector3d realUp = new Vector3d(forward).cross(right).normalize();

        return new Quaterniond().setFromNormalized(new Matrix3d().set(right, realUp, forward));
    }

    /**
     * Rotates a Bukkit {@link Vector} by a JOML quaternion, returning a new vector.
     *
     * @param rotation The non-null rotation to apply.
     * @param vector The non-null vector to rotate (not modified).
     * @return A new rotated vector.
     */
    public static Vector rotate(Quaterniondc rotation, Vector vector) {
        Vector3d result = rotation.transform(toVector3d(vector));
        return new Vector(result.x, result.y, result.z);
    }

    /**
     * Converts a Minecraft yaw/pitch (degrees) to a JOML rotation in the standard right-handed
     * frame.
     *
     * @param yaw The Minecraft yaw, in degrees.
     * @param pitch The Minecraft pitch, in degrees.
     * @return A non-null JOML quaternion.
     */
    public static Quaterniond fromYawPitch(float yaw, float pitch) {
        return new Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch));
    }

    /**
     * Extracts the Minecraft yaw (degrees) from a JOML rotation. Computed from the rotated forward
     * vector to avoid the gimbal lock that {@code getEulerAnglesYXZ} hits at pitch = ±90°.
     *
     * @param rotation The non-null rotation.
     * @return The Minecraft yaw, in degrees.
     */
    public static float yaw(Quaterniondc rotation) {
        Vector3d forward = rotation.transform(new Vector3d(0, 0, 1));
        return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    /**
     * Extracts the Minecraft pitch (degrees) from a JOML rotation.
     *
     * @param rotation The non-null rotation.
     * @return The Minecraft pitch, in degrees.
     */
    public static float pitch(Quaterniondc rotation) {
        Vector3d forward = rotation.transform(new Vector3d(0, 0, 1));
        double y = Math.max(-1.0, Math.min(1.0, -forward.y));
        return (float) Math.toDegrees(Math.asin(y));
    }

    private static Vector3d toVector3d(Vector vector) {
        return new Vector3d(vector.getX(), vector.getY(), vector.getZ());
    }
}
