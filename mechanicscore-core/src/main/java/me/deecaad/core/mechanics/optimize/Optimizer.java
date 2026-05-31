package me.deecaad.core.mechanics.optimize;

import me.deecaad.core.mechanics.program.Program;
import org.jetbrains.annotations.NotNull;

/**
 * Runs the optimizer passes over a resolved {@link Program} in order. Each pass
 * is semantics-preserving and rebuilds the instance graph.
 */
public final class Optimizer {

    private Optimizer() {
    }

    public static @NotNull Program optimize(@NotNull Program program) {
        program = ConstantFolder.apply(program);
        program = DeadCodeElimination.apply(program);
        program = QuerySubstitution.apply(program);   // before grouping: sets playersOnly used by groupKey
        program = Grouping.apply(program);
        return program;
    }
}
