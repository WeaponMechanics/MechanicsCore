package me.deecaad.core.mechanics;

import me.deecaad.core.mechanics.targeters.ScatterTargeter;
import me.deecaad.core.mechanics.targeters.ServerPlayersTargeter;
import me.deecaad.core.mechanics.targeters.SourceTargeter;
import me.deecaad.core.mechanics.targeters.TargetTargeter;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.mechanics.targeters.WorldPlayersTargeter;
import me.deecaad.core.mechanics.targeters.WorldTargeter;
import me.deecaad.core.utils.MutableRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Utility class holding all built-in targeters, and the registry for all
 * targeters. This class is not meant to be instantiated.
 *
 * <p>The registry {@link Targeters#REGISTRY} is a mutable registry, meaning
 * that you can external sources may add/overwrite targeters.
 */
public final class Targeters {

    /**
     * The registry for all globally registered targeters.
     */
    public static final @NotNull MutableRegistry<Targeter> REGISTRY
        = new MutableRegistry.SimpleMutableRegistry<>(new HashMap<>());

    public static final @NotNull Targeter SCATTER = register(new ScatterTargeter());
    public static final @NotNull Targeter SERVER_PLAYERS = register(new ServerPlayersTargeter());
    public static final @NotNull Targeter SOURCE = register(new SourceTargeter());
    public static final @NotNull Targeter TARGET = register(new TargetTargeter());
    public static final @NotNull Targeter WORLD_PLAYERS = register(new WorldPlayersTargeter());
    public static final @NotNull Targeter WORLD = register(new WorldTargeter());


    private Targeters() {
    }

    private static @NotNull Targeter register(@NotNull Targeter targeter) {
        REGISTRY.add(targeter);
        return targeter;
    }
}
