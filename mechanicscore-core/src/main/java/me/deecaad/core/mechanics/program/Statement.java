package me.deecaad.core.mechanics.program;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.RandomUtil;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * One parsed line of a {@link MechanicBlock}. Either a variable assignment, a
 * context binding, a builtin mechanic invocation, or a call to another block.
 */
public sealed interface Statement {

    void execute(@NotNull CastScope scope, @NotNull MechanicExecutor executor);

    /**
     * {@code $name = <expr>}
     */
    record Assignment(@NotNull String name, @NotNull Expression expression) implements Statement {
        @Override
        public void execute(@NotNull CastScope scope, @NotNull MechanicExecutor executor) {
            scope.setVariable(name, expression.eval(scope));
        }
    }

    /**
     * {@code @name = Targeter{...}}
     */
    record Binding(@NotNull String name, @NotNull Targeter targeter) implements Statement {
        @Override
        public void execute(@NotNull CastScope scope, @NotNull MechanicExecutor executor) {
            scope.setContext(name, targeter.target(scope));
        }
    }

    /**
     * {@code Mechanic{...} @subject ?Condition...} for a builtin mechanic.
     */
    record BuiltinInvocation(@NotNull Mechanic mechanic, @NotNull Subject subject, @NotNull List<Condition> conditions) implements Statement {
        @Override
        public void execute(@NotNull CastScope scope, @NotNull MechanicExecutor executor) {
            if (!RandomUtil.chance(mechanic.getChance()))
                return;

            int repeat = mechanic.getRepeatAmount();
            int interval = mechanic.getRepeatInterval();
            int delay = mechanic.getDelayBeforePlay();

            if (repeat == 1 && interval == 1 && delay == 0) {
                runOnce(scope);
                return;
            }

            Location location = scheduleLocation(scope);
            if (location == null) {
                runOnce(scope);
                return;
            }

            MechanicsCore.getInstance().getFoliaScheduler().region(location).runAtFixedRate(new Consumer<>() {
                int runs = 0;

                @Override
                public void accept(TaskImplementation task) {
                    if (runs++ >= repeat) {
                        task.cancel();
                        return;
                    }
                    runOnce(scope);
                }
            }, Math.max(delay, 1), Math.max(interval, 1));
        }

        private void runOnce(@NotNull CastScope scope) {
            Context targets = subject.resolve(scope);
            Context savedTarget = scope.getContext(CastScope.TARGET);
            try {
                OUTER : for (Target target : targets) {
                    // Expose the current element as the 'target' context for
                    // conditions/expressions/placeholders on this line.
                    scope.setContext(CastScope.TARGET, Context.of(target));
                    for (Condition condition : conditions)
                        if (!condition.isAllowed(scope, target))
                            continue OUTER;
                    scope.getBudget().chargeAction();
                    mechanic.use0(scope, target);
                }
            } finally {
                if (savedTarget != null)
                    scope.setContext(CastScope.TARGET, savedTarget);
            }
        }

        private Location scheduleLocation(@NotNull CastScope scope) {
            Target first = scope.source().first();
            return first == null ? null : first.location();
        }
    }

    /**
     * {@code BlockName{} @subject ?Condition...} for a user-defined block. Blocks
     * take no arguments: set the variables they read beforehand. The block runs on
     * the shared scope (a macro), once per target after the conditions pass.
     */
    record BlockInvocation(@NotNull String blockName, @NotNull Subject subject, @NotNull List<Condition> conditions) implements Statement {
        @Override
        public void execute(@NotNull CastScope scope, @NotNull MechanicExecutor executor) {
            MechanicBlock block = executor.resolveBlock(blockName);
            if (block == null)
                return;

            Context targets = subject.resolve(scope);
            Context savedTarget = scope.getContext(CastScope.TARGET);
            try {
                OUTER : for (Target target : targets) {
                    scope.setContext(CastScope.TARGET, Context.of(target));
                    for (Condition condition : conditions)
                        if (!condition.isAllowed(scope, target))
                            continue OUTER;

                    executor.callBlock(block, scope);
                }
            } finally {
                if (savedTarget != null)
                    scope.setContext(CastScope.TARGET, savedTarget);
            }
        }
    }

    /**
     * Produced by the optimizer: several batchable, unconditioned mechanics that
     * share one subject. The subject query runs ONCE and each mechanic is applied
     * per target, instead of re-resolving the subject per mechanic.
     */
    record GroupedInvoke(@NotNull Subject subject, @NotNull List<Mechanic> mechanics) implements Statement {
        @Override
        public void execute(@NotNull CastScope scope, @NotNull MechanicExecutor executor) {
            Context targets = subject.resolve(scope);
            Context savedTarget = scope.getContext(CastScope.TARGET);
            try {
                for (Target target : targets) {
                    scope.setContext(CastScope.TARGET, Context.of(target));
                    for (Mechanic mechanic : mechanics) {
                        scope.getBudget().chargeAction();
                        mechanic.use0(scope, target);
                    }
                }
            } finally {
                if (savedTarget != null)
                    scope.setContext(CastScope.TARGET, savedTarget);
            }
        }
    }
}
