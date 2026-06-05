package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SourceTargeter extends Targeter {

    public SourceTargeter() {
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    @Override
    public @NotNull Context target(@NotNull CastScope scope) {
        return wrap(scope.source());
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "source");
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/targeters/source";
    }


    @NotNull @Override
    public Targeter serialize(@NotNull SerializeData data) throws SerializerException {
        return applyParentArgs(data, new SourceTargeter());
    }
}
