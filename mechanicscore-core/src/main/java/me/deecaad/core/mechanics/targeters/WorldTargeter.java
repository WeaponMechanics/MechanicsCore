package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldTargeter extends Targeter {

    private String worldName;
    private boolean playersOnly;

    public WorldTargeter() {
    }

    public WorldTargeter(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    /**
     * Resolves the world: an explicit world name if set, otherwise the world of
     * the {@code From=} context (defaults to source).
     */
    protected @Nullable World resolveWorld(@NotNull CastScope scope) {
        if (worldName != null)
            return Bukkit.getWorld(worldName);
        Context from = scope.getContext(getFrom());
        Target first = from == null ? null : from.first();
        return first == null ? null : first.world();
    }

    @Override
    public @NotNull Context target(@NotNull CastScope scope) {
        World world = resolveWorld(scope);
        if (world == null) {
            MechanicsCore.getInstance().getDebugger().warning("There was an error getting the world for '" + worldName + "'");
            return Context.empty();
        }
        return wrap(Context.ofEntities(playersOnly ? world.getPlayers() : world.getLivingEntities()));
    }

    @Override
    public @NotNull me.deecaad.core.mechanics.targeters.Targeter specialize(@NotNull me.deecaad.core.mechanics.scope.TargetKind demand) {
        // Every consumer is satisfied by players: getPlayers() is far cheaper than getLivingEntities().
        if (demand == me.deecaad.core.mechanics.scope.TargetKind.PLAYER)
            playersOnly = true;
        return this;
    }

    @Override
    public @Nullable Object groupKey() {
        return "world:" + worldName + ":" + playersOnly + ":" + isEye() + ":" + (getOffset() != null) + ":" + getFrom();
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "world");
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/targeters/world";
    }

    @NotNull @Override
    public Targeter serialize(@NotNull SerializeData data) throws SerializerException {
        // When the world is not specified, it will default to the source's world
        String worldName = data.of("World").get(String.class).orElse(null);
        return applyParentArgs(data, new WorldTargeter(worldName));
    }

    /**
     * Returns <code>true</code> if this targeter uses the default values (no eye,
     * offset, or explicit world).
     */
    public boolean isDefaultValues() {
        return !isEye() && getOffset() == null && worldName == null;
    }
}
