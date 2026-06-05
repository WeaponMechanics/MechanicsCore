package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.SculkCatalyst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SculkBloomMechanic extends ActivateBlockMechanic<SculkCatalyst> {

    private int charge;

    public SculkBloomMechanic() {
        super(SculkCatalyst.class);
    }

    public SculkBloomMechanic(int charge) {
        super(SculkCatalyst.class);
        this.charge = charge;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "sculkbloom");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/sculk-bloom";
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null)
            return;
        Location target = subject.location();
        forEachBlock(target, catalyst -> catalyst.bloom(target.getBlock(), charge));
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().intKey("Charge").range(1, null);
    }

    @Override
    public @NotNull Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        int charge = data.of("Charge").assertRange(1, null).getInt().orElse(5);
        return applyParentArgs(data, new SculkBloomMechanic(charge));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
