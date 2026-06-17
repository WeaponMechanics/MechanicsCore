package me.deecaad.core.mechanics.optimize;

import me.deecaad.core.mechanics.conditions.CheckCondition;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Removes statements that can never act: {@code Chance=0} mechanics, invocations
 * guarded by a statically-false {@code ?Check}, and always-true {@code ?Check}
 * conditions (removed from the guard list).
 */
public final class DeadCodeElimination {

    private enum Verdict { TRUE, FALSE, UNKNOWN }

    private DeadCodeElimination() {
    }

    public static @NotNull Program apply(@NotNull Program program) {
        Map<String, MechanicBlock> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, MechanicBlock> entry : program.blocks().entrySet()) {
            MechanicBlock block = entry.getValue();
            List<Statement> statements = new ArrayList<>();
            for (Statement statement : block.statements()) {
                Statement kept = eliminate(statement);
                if (kept != null)
                    statements.add(kept);
            }
            blocks.put(entry.getKey(), new MechanicBlock(block.name(), statements));
        }
        return new Program(blocks, program.entry());
    }

    private static @Nullable Statement eliminate(@NotNull Statement statement) {
        if (statement instanceof Statement.BuiltinInvocation bi) {
            if (bi.mechanic().getChance() == 0.0)
                return null;
            List<Condition> conds = filter(bi.conditions());
            return conds == null ? null : new Statement.BuiltinInvocation(bi.mechanic(), bi.subject(), conds);
        }
        if (statement instanceof Statement.BlockInvocation b) {
            List<Condition> conds = filter(b.conditions());
            return conds == null ? null : new Statement.BlockInvocation(b.blockName(), b.subject(), conds);
        }
        return statement;
    }

    /**
     * Filters always-true conditions out; returns null if a condition is proven
     * false (the whole statement is dead).
     */
    private static @Nullable List<Condition> filter(@NotNull List<Condition> conditions) {
        List<Condition> result = new ArrayList<>(conditions.size());
        for (Condition condition : conditions) {
            switch (verdict(condition)) {
                case FALSE -> {
                    return null;
                }
                case TRUE -> {
                    // always passes; drop from the guard list
                }
                case UNKNOWN -> result.add(condition);
            }
        }
        return result;
    }

    private static @NotNull Verdict verdict(@NotNull Condition condition) {
        if (condition instanceof CheckCondition check && ConstantFolder.isConstant(check.getExpression())) {
            // isAllowed accounts for the Inverted flag; the constant expression ignores the (null) scope.
            return check.isAllowed(null, null) ? Verdict.TRUE : Verdict.FALSE;
        }
        return Verdict.UNKNOWN;
    }
}
