package me.deecaad.core.mechanics.scope;

/**
 * Shared across an entire cast (not copied per call frame). Guards against
 * runaway recursion and fan-out that would otherwise freeze the server thread.
 */
public final class CastBudget {

    public static final int DEFAULT_MAX_DEPTH = 100;
    public static final long DEFAULT_MAX_ACTIONS = 100_000;

    private final int maxDepth;
    private final long maxActions;
    private long actionsUsed;

    public CastBudget(int maxDepth, long maxActions) {
        this.maxDepth = maxDepth;
        this.maxActions = maxActions;
    }

    public static CastBudget defaults() {
        return new CastBudget(DEFAULT_MAX_DEPTH, DEFAULT_MAX_ACTIONS);
    }

    /**
     * Throws if the given call depth exceeds the cap. Called before forking a
     * scope for a block call.
     */
    public void checkDepth(int depth) {
        if (depth > maxDepth)
            throw new CastAbortException("Exceeded max call depth (" + maxDepth + "). This usually means infinite recursion - add a stop condition.");
    }

    /**
     * Charges one action against the budget. Called per mechanic invocation and
     * per block call.
     */
    public void chargeAction() {
        if (++actionsUsed > maxActions)
            throw new CastAbortException("Exceeded max actions (" + maxActions + ") in a single cast.");
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public long getMaxActions() {
        return maxActions;
    }

    public long getActionsUsed() {
        return actionsUsed;
    }
}
