package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class OnGroundCondition extends Condition {

    private @Nullable Set<BlockType> blocks;
    private double distanceFromGround;

    public OnGroundCondition() {
    }

    public OnGroundCondition(@Nullable Set<BlockType> blocks, double distanceFromGround) {
        this.blocks = blocks;
        this.distanceFromGround = distanceFromGround;
    }

    @Override
    protected boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        if (subject == null || subject.entity() == null)
            return false;
        LivingEntity entity = subject.entity();
        if (!entity.isOnGround())
            return false;

        if (blocks == null)
            return true;

        Location loc = entity.getLocation();
        if (loc.getWorld() == null)
            return false;

        var standingMat = loc.clone().subtract(0.0, distanceFromGround, 0.0).getBlock().getType();
        return blocks.contains(standingMat.asBlockType());
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "onground");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/on-ground";
    }

    @Override
    public @NotNull Condition serialize(@NotNull SerializeData data) throws SerializerException {
        double distance = data.of("distanceFromGround").assertRange(0.0, null).getDouble().orElse(0.01);

        Optional<List<?>> opt = data.of("blocks").get(List.class).map(l -> (List<?>) l);
        List<?> raw = opt.orElse(null);

        if (raw == null || raw.isEmpty()) {
            return applyParentArgs(data, new OnGroundCondition(null, distance));
        }

        @SuppressWarnings("unchecked")
        List<MapConfigLike.Holder> materials = (List<MapConfigLike.Holder>) raw;

        Set<BlockType> parsed = new HashSet<>();
        RegistryValueSerializer<BlockType> serializer = new RegistryValueSerializer<>(BlockType.class, true);

        for (MapConfigLike.Holder holder : materials) {
            String token = String.valueOf(holder.value());
            parsed.addAll(serializer.deserialize(token, data.of("blocks").getLocation()));
        }

        if (parsed.isEmpty()) {
            throw data.exception("blocks",
                    "The 'blocks' list for on_ground resolved to nothing. Double-check your block ids/tags.");
        }

        return applyParentArgs(data, new OnGroundCondition(parsed, distance));
    }
}
