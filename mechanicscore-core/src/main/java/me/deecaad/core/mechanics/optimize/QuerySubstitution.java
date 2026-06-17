package me.deecaad.core.mechanics.optimize;

import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.program.Subject;
import me.deecaad.core.mechanics.scope.TargetKind;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Substitutes a cheaper targeter query when every consumer of an inline subject
 * is satisfied by a narrower target kind (e.g. {@code getPlayers()} instead of
 * {@code getEntities()}). Only inline subjects are handled - their result is used
 * by exactly one statement, so the demand join is complete and safe.
 */
public final class QuerySubstitution {

    private QuerySubstitution() {
    }

    public static @NotNull Program apply(@NotNull Program program) {
        Map<String, MechanicBlock> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, MechanicBlock> entry : program.blocks().entrySet()) {
            MechanicBlock block = entry.getValue();
            List<Statement> statements = new ArrayList<>(block.statements().size());
            for (Statement statement : block.statements())
                statements.add(substitute(statement));
            blocks.put(entry.getKey(), new MechanicBlock(block.name(), statements));
        }
        return new Program(blocks, program.entry());
    }

    private static @NotNull Statement substitute(@NotNull Statement statement) {
        if (statement instanceof Statement.BuiltinInvocation(
                Mechanic mechanic, Subject subject, List<Condition> conditions
        ) && subject instanceof Subject.Inline(Targeter targeter)) {
            List<TargetKind> demands = new ArrayList<>();
            demands.add(mechanic.requiredTarget());
            for (Condition condition : conditions)
                demands.add(condition.requiredTarget());
            TargetKind demand = TargetKind.join(demands);
            if (demand == null)
                return statement;
            Targeter specialized = targeter.specialize(demand);
            return new Statement.BuiltinInvocation(mechanic, new Subject.Inline(specialized), conditions);
        }
        if (statement instanceof Statement.GroupedInvoke(
                Subject subject, List<Mechanic> mechanics
        ) && subject instanceof Subject.Inline(Targeter targeter)) {
            List<TargetKind> demands = new ArrayList<>();
            for (Mechanic mechanic : mechanics)
                demands.add(mechanic.requiredTarget());
            TargetKind demand = TargetKind.join(demands);
            if (demand == null)
                return statement;
            Targeter specialized = targeter.specialize(demand);
            return new Statement.GroupedInvoke(new Subject.Inline(specialized), mechanics);
        }
        return statement;
    }
}
