package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerPlayersTargeter extends Targeter {

    public ServerPlayersTargeter() {
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    @Override
    public @NotNull Context target(@NotNull CastScope scope) {
        return wrap(Context.ofEntities(Bukkit.getServer().getOnlinePlayers()));
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "server_players");
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/targeters/serverplayers";
    }

    @NotNull @Override
    public Targeter serialize(@NotNull SerializeData data) throws SerializerException {
        return applyParentArgs(data, new ServerPlayersTargeter());
    }
}
