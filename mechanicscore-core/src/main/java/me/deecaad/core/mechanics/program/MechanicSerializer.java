package me.deecaad.core.mechanics.program;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.diagnostic.Diagnostic;
import me.deecaad.core.mechanics.diagnostic.DiagnosticRenderer;
import me.deecaad.core.mechanics.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.sema.GlobalSymbolSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles a list of statement strings into an optimized {@link Program}. Drives
 * the pipeline: parse (AST + spans) to sema (resolve + typecheck, collecting
 * diagnostics) to optimize (rewrite the instance graph). All diagnostics are
 * logged; if any are errors, compilation fails. The entry block may call itself
 * by name (recursion).
 */
public class MechanicSerializer implements Serializer<Program> {

    public MechanicSerializer() {
    }

    @Override
    public String getKeyword() {
        return "Mechanics";
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/";
    }

    @NotNull @Override
    public Program serialize(@NotNull SerializeData data) throws SerializerException {
        List<?> list = data.getConfig().getList(data.getKey());
        if (list == null) {
            throw data.exception(null, "Could not find any list of mechanics",
                "Need help? https://cjcrafter.gitbook.io/mechanics/");
        }

        String entryName = data.getKey();
        DiagnosticReporter reporter = new DiagnosticReporter();

        List<String> lines = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (obj == null)
                throw data.listException(null, i, "Found a null/empty mechanic", "This happens when you have an empty line in your list");
            lines.add(obj.toString());
        }

        Program program = MechanicCompiler.compile(entryName, entryName, lines, data.getFile(), new GlobalSymbolSource(), reporter);

        // Log every diagnostic in the pretty caret style, then fail on errors.
        MechanicsLogger debug = MechanicsCore.getInstance() == null ? null : MechanicsCore.getInstance().getDebugger();
        if (debug != null) {
            for (Diagnostic diagnostic : reporter.all())
                DiagnosticRenderer.log(debug, diagnostic);
        }
        if (reporter.hasErrors())
            throw data.exception(null, "Found " + reporter.errorCount() + " error(s) in this Mechanics list (see above).");

        return program;
    }
}
