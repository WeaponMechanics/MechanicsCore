package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IgniteMechanic extends Mechanic {

    private int ticks;

    public IgniteMechanic() {
    }

    public IgniteMechanic(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "ignite");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/ignite";
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || subject.entity() == null)
            return;
        subject.entity().setFireTicks(ticks);
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        int ticks = data.of("Time").getInt().orElse(100);
        return applyParentArgs(data, new IgniteMechanic(ticks));
    }
}
