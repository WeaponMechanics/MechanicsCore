package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import me.deecaad.core.mechanics.CastData;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class OnGroundCondition extends Condition {

    // If null -> "any block"
    private @Nullable Set<BlockType> blocks;
    private double distanceFromGround;

    /**
     * Default constructor for serializer.
     */
    public OnGroundCondition() {
    }

    public OnGroundCondition(@Nullable Set<BlockType> blocks, double distanceFromGround) {
        this.blocks = blocks;
        this.distanceFromGround = distanceFromGround;
    }

    @Override
    protected boolean isAllowed0(CastData cast) {
        if (cast.getTarget() == null) return false;
        if (!cast.getTarget().isOnGround()) return false;

        // No block filter -> any block is acceptable
        if (blocks == null) return true;

        var loc = cast.getTarget().getLocation();
        if (loc.getWorld() == null) return false;

        // Just below the feet
        var standingMat = loc.clone().subtract(0.0, distanceFromGround, 0.0).getBlock().getType();
        return blocks.contains(standingMat.asBlockType());
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "on_ground");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/on-ground";
    }

    @Override
    public @NotNull Condition serialize(@NotNull SerializeData data) throws SerializerException {
        // When sampling the block beneath the target, using a 0.0 offset reads the block at the feet position
        // (often AIR). A small positive offset samples slightly below the feet, which resolves to the block
        // the entity is standing on (works better for slabs/carpets/snow layers), so defaulting to 0.01
        // should do the trick
        double distance = data.of("distanceFromGround").assertRange(0.0, null).getDouble().orElse(0.01);

        Optional<List<?>> opt = data.of("blocks").get(List.class).map(l -> (List<?>) l);
        List<?> raw = opt.orElse(null);

        // Missing key or explicitly empty list then we match ANY block
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
            // Can happen if a tag exists but resolves to 0 blocks
            throw data.exception("blocks",
                    "The 'blocks' list for on_ground resolved to nothing. Double-check your block ids/tags.");
        }

        return applyParentArgs(data, new OnGroundCondition(parsed, distance));
    }
}
