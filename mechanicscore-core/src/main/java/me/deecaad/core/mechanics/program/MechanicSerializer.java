package me.deecaad.core.mechanics.program;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SnakeYamlConfig;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticRenderer;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
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

        // Re-anchor each diagnostic from "relative to the mechanic string" onto the real YAML line, so
        // it renders against the actual source (with its '- ' and quotes) and a real line number.
        List<int[]> origins = data.getConfig() instanceof SnakeYamlConfig syc
            ? syc.listItemPositions(data.getKey()) : List.of();

        // Log every diagnostic in the pretty caret style, then fail on errors.
        MechanicsLogger debug = MechanicsCore.getInstance() == null ? null : MechanicsCore.getInstance().getDebugger();
        if (debug != null) {
            SnakeYamlConfig syc = data.getConfig() instanceof SnakeYamlConfig c ? c : null;
            for (Diagnostic diagnostic : reporter.all())
                DiagnosticRenderer.log(debug, reanchor(diagnostic, origins, syc));
        }
        if (reporter.hasErrors())
            throw data.exception(null, "Found " + reporter.errorCount() + " error(s) in this Mechanics list (see above).");

        return program;
    }

    /**
     * Maps a diagnostic from mechanic-string-relative columns (line 0) onto the real YAML line and
     * column recorded for its list item, so the rendered caret lands under the actual source.
     */
    static @NotNull Diagnostic reanchor(@NotNull Diagnostic diagnostic, @NotNull List<int[]> origins, SnakeYamlConfig config) {
        int index = diagnostic.source().listIndex();
        if (config == null || index < 0 || index >= origins.size())
            return diagnostic;

        int line = origins.get(index)[0];
        int column = origins.get(index)[1];
        if (line < 0)
            return diagnostic;

        String rawLine = config.sourceLine(line);
        SourceRef source = new SourceRef(diagnostic.source().file(), diagnostic.source().configPath(), index, rawLine,
            config.contextBefore(line));
        List<Span> secondary = new ArrayList<>(diagnostic.secondary().size());
        for (Span span : diagnostic.secondary())
            secondary.add(shift(span, line, column));
        return new Diagnostic(diagnostic.severity(), diagnostic.kind(), diagnostic.message(),
            source, shift(diagnostic.primary(), line, column), secondary, diagnostic.hint());
    }

    private static @NotNull Span shift(@NotNull Span span, int line, int column) {
        if (span.line() < 0)
            return span;
        return Span.of(line, column + span.start(), column + span.end());
    }
}
