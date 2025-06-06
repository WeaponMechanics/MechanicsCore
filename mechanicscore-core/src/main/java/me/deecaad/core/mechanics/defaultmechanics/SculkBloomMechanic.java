package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.SculkCatalyst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SculkBloomMechanic extends ActivateBlockMechanic<SculkCatalyst> {

    private int charge;

    /**
     * Default constructor for serializer
     */
    public SculkBloomMechanic() {
        super(SculkCatalyst.class);
    }

    public SculkBloomMechanic(int charge) {
        super(SculkCatalyst.class);
        this.charge = charge;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "sculkbloom");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/sculk-bloom";
    }

    @Override
    protected void use0(CastData cast) {
        Location target = cast.getTargetLocation();
        if (target == null)
            return;

        forEachBlock(target, catalyst -> catalyst.bloom(target.getBlock(), charge));
    }

    @Override
    public @NotNull Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        int charge = data.of("Charge").assertRange(1, null).getInt().orElse(5);
        return applyParentArgs(data, new SculkBloomMechanic(charge));
    }
}
