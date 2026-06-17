package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SneakingCondition extends Condition {

    public SneakingCondition() {
    }

    @Override
    protected boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        return subject != null && subject.entity() instanceof Player player && player.isSneaking();
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "sneaking");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/sneaking";
    }


    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        return applyParentArgs(data, new SneakingCondition());
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.PLAYER;
    }
}
