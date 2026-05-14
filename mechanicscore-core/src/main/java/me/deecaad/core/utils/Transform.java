package me.deecaad.core.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in a transform hierarchy: a local position and rotation that may inherit from a parent
 * transform. Rotation math is backed by JOML's {@link Quaterniond}.
 */
public class Transform {

    private Vector localPosition;
    private Quaterniond localRotation;

    // Transforms inherit their parent's position/rotation
    private Transform parent;
    private final List<Transform> children;

    public Transform() {
        children = new ArrayList<>();
        localRotation = new Quaterniond();
        localPosition = new Vector();
    }

    public Transform(Transform parent) {
        this();
        setParent(parent);
    }

    public Transform getParent() {
        return parent;
    }

    public Transform getChild(int i) {
        return children.get(i);
    }

    public void setParent(Transform parent) {
        // First we need to adjust based on the previous parent.
        if (this.parent != null) {
            localPosition = getPosition();
            localRotation = getRotation();

            this.parent.children.remove(this);
        }

        // Now we need to adjust it for the new parent's position
        if (parent != null) {

            // Bit more complicated... Find the difference between the 2
            // quaternions
            Quaterniond parentRotation = parent.getRotation();
            localRotation = parentRotation.invert().mul(localRotation);

            Vector position = parent.getPosition();
            localPosition.subtract(position);

            parent.children.add(this);
        }

        this.parent = parent;
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
        this.localPosition = localPosition;
    }

    public Vector getPosition() {
        if (getParent() == null)
            return getLocalPosition();

        return getParent().getPosition().add(rotate(getParent().getRotation(), localPosition));
    }

    public void setPosition(Location position) {
        setPosition(position.toVector());
    }

    public void setPosition(Vector position) {
        if (getParent() == null) {
            setLocalPosition(position);
        } else {
            Vector parentPos = getParent().getPosition();
            Quaterniond parentRot = getParent().getRotation();

            setLocalPosition(rotate(parentRot, position.subtract(parentPos)));
        }
    }

    public Quaterniond getLocalRotation() {
        return new Quaterniond(localRotation);
    }

    public void setLocalRotation(Quaterniond localRotation) {
        this.localRotation = new Quaterniond(localRotation).normalize();
    }

    public Quaterniond getRotation() {
        if (getParent() == null)
            return getLocalRotation();

        return getParent().getRotation().mul(localRotation);
    }

    public void setRotation(Quaterniond rotation) {
        if (getParent() == null) {
            setLocalRotation(rotation);
        } else {
            setLocalRotation(getParent().getRotation().invert().mul(rotation));
        }
    }

    public void applyRotation(Quaterniond rotation) {
        localRotation.mul(new Quaterniond(rotation).normalize());
    }

    /**
     * Builds a rotation that points the local forward axis (+Z) along <code>direction</code>, with
     * the local up axis (+Y) aligned as closely as possible to <code>up</code>.
     *
     * <p>
     * If <code>up</code> is parallel to <code>direction</code>, an arbitrary perpendicular axis is
     * chosen instead. If <code>direction</code> is a zero vector, the identity rotation is returned.
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
            // 'up' is parallel to 'direction' -- pick an arbitrary perpendicular axis.
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

    private static Vector3d toVector3d(Vector vector) {
        return new Vector3d(vector.getX(), vector.getY(), vector.getZ());
    }
}
