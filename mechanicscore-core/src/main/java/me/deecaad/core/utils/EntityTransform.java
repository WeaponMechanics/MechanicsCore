package me.deecaad.core.utils;

import com.cjcrafter.foliascheduler.ServerImplementation;
import me.deecaad.core.MechanicsCore;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Wraps a bukkit {@link Entity} as a {@link Transform}. Entity transforms cannot have a parent, but
 * they can have children. This transform tries to follow the attached entity as close as possible.
 */
public class EntityTransform extends Transform {

    private final Entity entity;

    private Vector cachedPosition;
    private Quaterniond cachedRotation;
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;

    public EntityTransform(Entity entity) {
        this.entity = entity;
        refresh();
    }

    private boolean refresh() {
        Location loc = entity.getLocation();
        boolean changed = loc.getX() != lastX || loc.getY() != lastY || loc.getZ() != lastZ
            || loc.getYaw() != lastYaw || loc.getPitch() != lastPitch;
        lastX = loc.getX();
        lastY = loc.getY();
        lastZ = loc.getZ();
        lastYaw = loc.getYaw();
        lastPitch = loc.getPitch();
        cachedPosition = loc.toVector();
        cachedRotation = Transform.fromYawPitch(loc.getYaw(), loc.getPitch());
        return changed;
    }

    @Override
    public void update() {
        if (refresh())
            propagate();
    }

    @Override
    public TransformLike getParent() {
        return null; // cannot have a parent
    }

    @Override
    public void setParent(TransformLike parent) {
        throw new IllegalArgumentException("EntityTransform cannot have parent");
    }

    @Override
    public Vector getLocalPosition() {
        return cachedPosition.clone();
    }

    @Override
    public void setLocalPosition(Vector localPosition) {
        ServerImplementation server = MechanicsCore.getInstance().getFoliaScheduler();
        server.teleportAsync(entity, localPosition.toLocation(entity.getWorld()));
    }

    @Override
    public Quaterniond getLocalRotation() {
        return new Quaterniond(cachedRotation);
    }

    @Override
    public void setLocalRotation(Quaterniond localRotation) {
        // Boundary translation: decompose the standard right-handed rotation into Minecraft's
        // yaw/pitch. Minecraft yaw increases clockwise (viewed from above), which is the opposite
        // of a standard +Y rotation, so the yaw component must be negated.
        Vector3d euler = localRotation.getEulerAnglesYXZ(new Vector3d());
        if (entity.getType() == EntityType.ARMOR_STAND) {
            ArmorStand stand = (ArmorStand) entity;
            stand.setHeadPose(new EulerAngle(euler.x, euler.y, euler.z));
        } else {
            float yaw = (float) Math.toDegrees(-euler.y);
            float pitch = (float) Math.toDegrees(euler.x);
            entity.setRotation(yaw, pitch);
        }
    }

    @Override
    public void applyRotation(Quaterniond rotation) {
        Quaterniond local = getLocalRotation();
        local.mul(rotation);
        setLocalRotation(local);
    }
}
