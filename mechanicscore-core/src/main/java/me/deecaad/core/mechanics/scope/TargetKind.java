package me.deecaad.core.mechanics.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The narrowest kind of target a mechanic or condition needs. Used by the
 * optimizer to substitute cheaper targeter queries (e.g. {@code getPlayers()}
 * instead of {@code getEntities()}) when every consumer is satisfied by players.
 *
 * <p>{@code PLAYER}, {@code LIVING_ENTITY}, {@code ENTITY} form a breadth ladder
 * (player is narrowest). {@code LOCATION} imposes no entity requirement - it is
 * satisfied by any target's location.
 */
public enum TargetKind {

    PLAYER(0),
    LIVING_ENTITY(1),
    ENTITY(2),
    LOCATION(-1);

    private final int rank;

    TargetKind(int rank) {
        this.rank = rank;
    }

    public boolean requiresEntity() {
        return rank >= 0;
    }

    private static @NotNull TargetKind broader(@NotNull TargetKind a, @NotNull TargetKind b) {
        return a.rank >= b.rank ? a : b;
    }

    /**
     * Joins the demands of several consumers into a single entity demand, or
     * {@code null} when no consumer needs an entity (so no specialization).
     */
    public static @Nullable TargetKind join(@NotNull Iterable<TargetKind> demands) {
        TargetKind result = null;
        for (TargetKind demand : demands) {
            if (!demand.requiresEntity())
                continue;
            result = result == null ? demand : broader(result, demand);
        }
        return result;
    }
}
