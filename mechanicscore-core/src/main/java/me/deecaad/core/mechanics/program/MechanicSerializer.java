package me.deecaad.core.mechanics.program;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SnakeYamlConfig;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.DiagnosticRenderer;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
import me.deecaad.core.mechanics.sema.GlobalSymbolSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Compiles a list of statement strings into an optimized {@link Program}. Drives
 * the pipeline: parse (AST + spans) to sema (resolve + typecheck, collecting
 * diagnostics) to optimize (rewrite the instance graph). All diagnostics are
 * logged; if any are errors, compilation fails. The entry block may call itself
 * by name (recursion).
 */
public class MechanicSerializer implements Serializer<Program> {

    private final @NotNull Set<String> providedContexts;
    private final @NotNull Set<String> providedVariables;

    public MechanicSerializer() {
        this(Set.of(), Set.of());
    }

    public MechanicSerializer(@NotNull Set<String> providedContexts) {
        this(providedContexts, Set.of());
    }

    /**
     * @param providedContexts  Context names the host seeds into the {@link me.deecaad.core.mechanics.scope.CastScope}
     *                          before running this list, beyond {@code source}/{@code target}. Sema checks
     *                          {@code @context} references against them, so the host must seed the same names
     *                          at cast time. A host with extra contexts constructs this directly and serializes
     *                          {@code data.move("Mechanics")} (the reflective {@code serialize(Class)} path uses
     *                          the no-arg form).
     * @param providedVariables {@code $variable} names the host seeds before running. A {@code $ref} that is
     *                          neither provided nor assigned in the list is a hard error.
     */
    public MechanicSerializer(@NotNull Set<String> providedContexts, @NotNull Set<String> providedVariables) {
        this.providedContexts = Set.copyOf(providedContexts);
        this.providedVariables = Set.copyOf(providedVariables);
    }

    /**
     * Sugar for a host serializer nesting a {@code Mechanics:} list that seeds extra contexts at cast
     * time. Compiles {@code data.move("Mechanics")} declaring the given context names (beyond
     * {@code source}/{@code target}), so {@code @context} references in the list are checked. The host
     * must seed the same names into the {@link me.deecaad.core.mechanics.scope.CastScope} when it runs.
     *
     * <pre>
     * Program mechanics = MechanicSerializer.compile(data, "Entities", "Blocks", "Up");
     * </pre>
     */
    public static @NotNull Program compile(@NotNull SerializeData data, @NotNull String... providedContexts) throws SerializerException {
        return new MechanicSerializer(Set.of(providedContexts)).serialize(data.move("Mechanics"));
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

        Program program = MechanicCompiler.compile(entryName, entryName, lines, data.getFile(), new GlobalSymbolSource(providedContexts, providedVariables), reporter);

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
        if (reporter.hasErrors()) {
            // The detailed errors already rendered above with their own locations, so this aggregate is a
            // message-only summary: no file/path means the renderer skips the location line and caret.
            int errors = reporter.errorCount();
            String key = data.getKey();
            String section = key == null ? "this Mechanics list" : key.substring(key.lastIndexOf('.') + 1);
            throw new SerializerException(
                new ArrayList<>(List.of("Found " + errors + " error" + (errors == 1 ? "" : "s") + " in " + section + " (see above)")),
                DiagnosticKind.OTHER, null, null, -1);
        }

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
