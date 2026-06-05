package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Targets every player in the current world. Not shown to players directly; the
 * serializer swaps a {@code @World} targeter with a player-only mechanic for this
 * one as a performance optimization.
 */
public class WorldPlayersTargeter extends WorldTargeter {

    public WorldPlayersTargeter() {
    }

    public WorldPlayersTargeter(String worldName) {
        super(worldName);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "world_players");
    }

    @Override
    public @NotNull Context target(@NotNull CastScope scope) {
        World world = resolveWorld(scope);
        if (world == null) {
            MechanicsCore.getInstance().getDebugger().warning("There was an error getting the world for '" + getWorldName() + "'");
            return Context.empty();
        }
        return wrap(Context.ofEntities(world.getPlayers()));
    }
}
