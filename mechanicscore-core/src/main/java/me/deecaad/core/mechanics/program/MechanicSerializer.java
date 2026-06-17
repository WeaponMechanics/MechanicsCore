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
import java.util.LinkedHashSet;
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

    private MechanicSerializer(@NotNull Set<String> providedContexts, @NotNull Set<String> providedVariables) {
        this.providedContexts = Set.copyOf(providedContexts);
        this.providedVariables = Set.copyOf(providedVariables);
    }

    /**
     * Starts a serializer that declares the vocabulary a host seeds into the
     * {@link me.deecaad.core.mechanics.scope.CastScope} before running this list. Sema checks
     * {@code @context}/{@code $variable} references against the declared names, so the host must seed
     * the same names at cast time. Hand the built serializer to the call site that reads the list:
     *
     * <pre>{@code
     * Program mechanics = data.of("Mechanics").serialize(
     *     MechanicSerializer.builder()
     *         .context("Victim")
     *         .variable("damage")
     *         .build()
     * ).orElseThrow();
     * }</pre>
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Declares the extra contexts and {@code $variables} a {@code Mechanics:} list resolves against,
     * beyond the {@code source}/{@code target} builtins, at the call site that serializes it.
     */
    public static final class Builder {

        private final Set<String> contexts = new LinkedHashSet<>();
        private final Set<String> variables = new LinkedHashSet<>();

        private Builder() {
        }

        public @NotNull Builder context(@NotNull String name) {
            contexts.add(name);
            return this;
        }

        public @NotNull Builder contexts(@NotNull String... names) {
            for (String name : names)
                contexts.add(name);
            return this;
        }

        public @NotNull Builder variable(@NotNull String name) {
            variables.add(name);
            return this;
        }

        public @NotNull Builder variables(@NotNull String... names) {
            for (String name : names)
                variables.add(name);
            return this;
        }

        public @NotNull MechanicSerializer build() {
            return new MechanicSerializer(contexts, variables);
        }
    }

    @Override
    public @Nullable String getWikiLink() {
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
