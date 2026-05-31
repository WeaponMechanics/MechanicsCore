package me.deecaad.core.mechanics.program;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.mechanics.scope.CastAbortException;
import me.deecaad.core.mechanics.scope.CastScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Runs the statements of a {@link MechanicBlock} against a {@link CastScope},
 * and handles block calls (the functional/recursive part). A single executor and
 * a single scope are used for an entire cast, so the call budget and all
 * variables/contexts are shared. Recursion is bounded by a call-depth counter.
 */
public final class MechanicExecutor {

    private final @NotNull Function<String, MechanicBlock> blockResolver;
    private int depth;

    public MechanicExecutor(@NotNull Function<String, MechanicBlock> blockResolver) {
        this.blockResolver = blockResolver;
    }

    /**
     * Runs a block, catching budget aborts at the top level so a runaway cast
     * fails loudly but does not crash the caller.
     */
    public void execute(@NotNull MechanicBlock block, @NotNull CastScope scope) {
        try {
            run(block, scope);
        } catch (CastAbortException e) {
            try {
                MechanicsCore core = MechanicsCore.getInstance();
                if (core != null)
                    core.getDebugger().warning("Cast aborted: " + e.getMessage());
            } catch (Throwable ignored) {
                // No plugin context (e.g. unit tests); nothing to log to.
            }
        }
    }

    private void run(@NotNull MechanicBlock block, @NotNull CastScope scope) {
        for (Statement statement : block.statements())
            statement.execute(scope, this);
    }

    public @Nullable MechanicBlock resolveBlock(@NotNull String name) {
        return blockResolver.apply(name);
    }

    /**
     * Runs a block on the shared cast scope (a macro call: no fork, so the block's
     * variable/context writes persist after it returns). The caller has already set
     * {@code @target} for this call. A call-depth counter bounds recursion; budget
     * aborts propagate to the top-level {@link #execute}.
     */
    public void callBlock(@NotNull MechanicBlock block, @NotNull CastScope scope) {
        scope.getBudget().chargeAction();
        scope.getBudget().checkDepth(++depth);
        try {
            run(block, scope);
        } finally {
            depth--;
        }
    }
}
