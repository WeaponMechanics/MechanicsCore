package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TargetTargeter extends Targeter {

    public TargetTargeter() {
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    @Override
    public @NotNull Context target(@NotNull CastScope scope) {
        return wrap(scope.target());
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "target");
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/targeters/target";
    }

    @NotNull @Override
    public Targeter serialize(@NotNull SerializeData data) throws SerializerException {
        return applyParentArgs(data, new TargetTargeter());
    }
}
