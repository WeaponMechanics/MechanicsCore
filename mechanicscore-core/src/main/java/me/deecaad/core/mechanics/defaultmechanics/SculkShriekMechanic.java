package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.bukkit.block.SculkShrieker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SculkShriekMechanic extends ActivateBlockMechanic<SculkShrieker> {

    public SculkShriekMechanic() {
        super(SculkShrieker.class);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "sculkshriek");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/sculk-shriek";
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || !(subject.entity() instanceof Player player))
            return;
        forEachBlock(player.getLocation(), shrieker -> shrieker.tryShriek(player));
    }

    @Override
    public @NotNull Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        return applyParentArgs(data, new SculkShriekMechanic());
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.PLAYER;
    }
}
