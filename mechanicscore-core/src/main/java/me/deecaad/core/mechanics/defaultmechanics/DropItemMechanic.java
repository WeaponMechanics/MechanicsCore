package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.AnyVectorProvider;
import me.deecaad.core.file.serializers.ItemSerializer;
import me.deecaad.core.file.serializers.VectorProvider;
import me.deecaad.core.file.serializers.VectorSerializer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.utils.EntityTransform;
import me.deecaad.core.utils.ImmutableVector;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;

/**
 * Drops a real item at the target location.
 */
public class DropItemMechanic extends Mechanic {

    private ItemStack item;
    private VectorProvider velocity;

    public DropItemMechanic() {
    }

    public DropItemMechanic(ItemStack item, VectorProvider velocity) {
        this.item = item;
        this.velocity = velocity;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null)
            return;
        World world = subject.world();
        Location spawnPosition = subject.location();
        if (world == null)
            return;

        world.dropItem(spawnPosition, item, itemEntity -> {
            EntityTransform localTransform = subject.entity() == null ? null : new EntityTransform(subject.entity());
            Quaterniond localRotation = localTransform == null ? null : localTransform.getLocalRotation();
            itemEntity.setVelocity(velocity.provide(localRotation).multiply(1.0 / 20.0));
        });
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "dropitem");
    }

    @Override
    public @NotNull Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        ItemStack item = new ItemSerializer().serialize(data);
        VectorProvider zero = new AnyVectorProvider(false, new ImmutableVector());
        VectorProvider velocity = data.of("Velocity").serialize(VectorSerializer.class).orElse(zero);
        return applyParentArgs(data, new DropItemMechanic(item, velocity));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
