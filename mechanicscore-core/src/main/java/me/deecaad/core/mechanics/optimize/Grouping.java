package me.deecaad.core.mechanics.optimize;

import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.program.Subject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges consecutive batchable, unconditioned, unscheduled mechanics that share
 * a subject query into one {@link Statement.GroupedInvoke}, so the (potentially
 * expensive) subject query runs once instead of once per mechanic.
 */
public final class Grouping {

    private Grouping() {
    }

    public static @NotNull Program apply(@NotNull Program program) {
        Map<String, MechanicBlock> blocks = new LinkedHashMap<>();
        for (Map.Entry<String, MechanicBlock> entry : program.blocks().entrySet()) {
            MechanicBlock block = entry.getValue();
            blocks.put(entry.getKey(), new MechanicBlock(block.name(), groupBlock(block.statements())));
        }
        return new Program(blocks, program.entry());
    }

    private static @NotNull List<Statement> groupBlock(@NotNull List<Statement> in) {
        List<Statement> out = new ArrayList<>();
        String runKey = null;
        Subject runSubject = null;
        List<Mechanic> runMechanics = null;

        for (Statement statement : in) {
            String key = statement instanceof Statement.BuiltinInvocation bi && isGroupable(bi)
                ? subjectKey(bi.subject()) : null;

            if (key != null && key.equals(runKey)) {
                runMechanics.add(((Statement.BuiltinInvocation) statement).mechanic());
            } else {
                flush(out, runSubject, runMechanics);
                if (key != null) {
                    Statement.BuiltinInvocation bi = (Statement.BuiltinInvocation) statement;
                    runKey = key;
                    runSubject = bi.subject();
                    runMechanics = new ArrayList<>();
                    runMechanics.add(bi.mechanic());
                } else {
                    out.add(statement);
                    runKey = null;
                    runSubject = null;
                    runMechanics = null;
                }
            }
        }
        flush(out, runSubject, runMechanics);
        return out;
    }

    private static void flush(@NotNull List<Statement> out, @Nullable Subject subject, @Nullable List<Mechanic> mechanics) {
        if (mechanics == null || mechanics.isEmpty())
            return;
        if (mechanics.size() == 1)
            out.add(new Statement.BuiltinInvocation(mechanics.get(0), subject, List.of()));
        else
            out.add(new Statement.GroupedInvoke(subject, mechanics));
    }

    private static boolean isGroupable(@NotNull Statement.BuiltinInvocation bi) {
        Mechanic m = bi.mechanic();
        return bi.conditions().isEmpty()
            && m.isBatchablePlayerEffect()
            && m.getChance() == 1.0
            && m.getRepeatAmount() == 1
            && m.getRepeatInterval() == 1
            && m.getDelayBeforePlay() == 0
            && subjectKey(bi.subject()) != null;
    }

    private static @Nullable String subjectKey(@NotNull Subject subject) {
        if (subject instanceof Subject.Reference ref)
            return "ref:" + ref.name();
        Subject.Inline inline = (Subject.Inline) subject;
        Object key = inline.targeter().groupKey();
        return key == null ? null : "inl:" + key;
    }
}
