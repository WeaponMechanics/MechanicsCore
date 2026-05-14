package me.deecaad.core.utils;

import com.cjcrafter.foliascheduler.ServerImplementation;
import com.cjcrafter.foliascheduler.util.MinecraftVersions;
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
 * Wraps a bukkit {@link Entity} to use {@link Transform} methods easily. Entity transforms cannot
 * have parents, but they can have children. Not very performance friendly when having many
 * children, since the quaternions are not cached every tick.
 * <p>
 * TODO add cache to deal with potential performance problems
 */
public class EntityTransform extends Transform {

    private final Entity entity;

    public EntityTransform(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Transform getParent() {
        return null; // cannot have a parent
    }

    @Override
    public void setParent(Transform parent) {
        throw new IllegalArgumentException("EntityTransform cannot have parent");
    }

    @Override
    public Vector getLocalPosition() {
        return entity.getLocation().toVector();
    }

    @Override
    public void setLocalPosition(Vector localPosition) {
        ServerImplementation server = MechanicsCore.getInstance().getFoliaScheduler();
        server.teleportAsync(entity, localPosition.toLocation(entity.getWorld()));
    }

    @Override
    public Quaterniond getLocalRotation() {
        return Transform.lookAt(entity.getLocation().getDirection(), new Vector(0, 1, 0));
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
