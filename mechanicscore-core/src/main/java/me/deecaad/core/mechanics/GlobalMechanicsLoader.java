package me.deecaad.core.mechanics;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.MechanicsPlugin;
import me.deecaad.core.mechanics.ast.Loc;
import me.deecaad.core.diagnostic.SourceRef;
import me.deecaad.core.diagnostic.Span;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticRenderer;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.program.GlobalBlocks;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.MechanicCompiler;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.sema.GlobalSymbolSource;
import me.deecaad.core.mechanics.sema.SymbolSource;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads the {@code plugins/MechanicsCore/mechanics/} folder into {@link GlobalBlocks}.
 * Every top-level key in a file is a block name with a child {@code Mechanics:} list;
 * blocks may call each other (nesting, recursion, macros) and are callable from any
 * normal mechanics list. Compiles each block as its own single-block program with all
 * declared names visible, so cross-block calls resolve as runtime block calls.
 */
public final class GlobalMechanicsLoader {

    private GlobalMechanicsLoader() {
    }

    /**
     * A gathered, not-yet-compiled block: where it came from and its raw lines.
     */
    public record Decl(@Nullable File file, @NotNull String configPath, @NotNull List<String> lines) {
    }

    /**
     * Loads (or reloads) the mechanics folder and installs the result globally.
     * Safe to call on enable and on reload.
     */
    public static void load(@NotNull MechanicsPlugin plugin) {
        MechanicsLogger debug = plugin.getDebugger();
        File folder = new File(plugin.getDataFolder(), "mechanics");
        if (!folder.exists()) {
            folder.mkdirs();
            URL example = plugin.getClass().getClassLoader().getResource("MechanicsCore/mechanics/example.yml");
            if (example != null)
                FileUtil.ensureDefaults(example, new File(folder, "example.yml"));
        }

        DiagnosticReporter reporter = new DiagnosticReporter();
        Map<String, Decl> declared = gather(folder, reporter);

        Set<String> declaredLower = new HashSet<>();
        for (String name : declared.keySet())
            declaredLower.add(name.toLowerCase(Locale.ROOT));

        Map<String, MechanicBlock> compiled = compile(declared, withDeclaredBlocks(declaredLower), reporter);
        GlobalBlocks.install(compiled);

        for (Diagnostic diagnostic : reporter.all())
            DiagnosticRenderer.log(debug, diagnostic);
        debug.info("Loaded " + compiled.size() + " global mechanic block(s)"
            + (reporter.hasErrors() ? " with " + reporter.errorCount() + " error(s)" : ""));
    }

    /**
     * Reads every {@code .yml} file in the folder, treating each top-level key as a
     * block name with a child {@code Mechanics:} list. Records diagnostics for blocks
     * with no list, duplicate names, or names that clash with a built-in mechanic.
     */
    static @NotNull Map<String, Decl> gather(@NotNull File folder, @NotNull DiagnosticReporter reporter) {
        Map<String, Decl> declared = new LinkedHashMap<>();
        Set<String> seenLower = new HashSet<>();
        GlobalSymbolSource symbols = new GlobalSymbolSource();

        List<File> files = new ArrayList<>();
        collectYaml(folder, files);
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (String key : yaml.getKeys(false)) {
                String path = key + ".Mechanics";
                Loc loc = wholeKey(file, path, key);
                List<?> list = yaml.getList(path);
                if (list == null) {
                    reporter.warning(loc, "Block '" + key + "' has no '" + key + ": Mechanics:' list and was skipped");
                    continue;
                }
                String lower = key.toLowerCase(Locale.ROOT);
                if (!seenLower.add(lower)) {
                    reporter.error(loc, "Duplicate global block name '" + key + "'");
                    continue;
                }
                if (symbols.mechanic(key) != null) {
                    reporter.error(loc, "Global block '" + key + "' clashes with a built-in mechanic of the same name",
                        "Rename the block; built-in mechanics always win name resolution");
                    continue;
                }
                List<String> lines = new ArrayList<>(list.size());
                for (Object line : list)
                    lines.add(String.valueOf(line));
                declared.put(key, new Decl(file, path, lines));
            }
        }
        return declared;
    }

    /**
     * Compiles each declared block as its own single-block program. {@code symbols}
     * must report all declared block names from {@link SymbolSource#blockNames()} so
     * cross-block calls resolve. Pure (no Bukkit), so it is unit-testable.
     */
    public static @NotNull Map<String, MechanicBlock> compile(@NotNull Map<String, Decl> declared,
                                                              @NotNull SymbolSource symbols,
                                                              @NotNull DiagnosticReporter reporter) {
        Map<String, MechanicBlock> out = new LinkedHashMap<>();
        for (Map.Entry<String, Decl> entry : declared.entrySet()) {
            String name = entry.getKey();
            Decl decl = entry.getValue();

            int errorsBefore = reporter.errorCount();
            Program program = MechanicCompiler.compile(name, decl.configPath(), decl.lines(), decl.file(), symbols, reporter);

            // Skip a block that failed to compile: better an "unknown block" than a half-broken one.
            if (reporter.errorCount() == errorsBefore)
                out.put(name, program.getEntry());
        }
        return out;
    }

    private static @NotNull SymbolSource withDeclaredBlocks(@NotNull Set<String> declaredLower) {
        GlobalSymbolSource delegate = new GlobalSymbolSource();
        return new SymbolSource() {
            @Override public @Nullable Mechanic mechanic(@NotNull String name) { return delegate.mechanic(name); }
            @Override public @Nullable Targeter targeter(@NotNull String name) { return delegate.targeter(name); }
            @Override public @Nullable Condition condition(@NotNull String name) { return delegate.condition(name); }
            @Override public @NotNull Set<String> mechanicNames() { return delegate.mechanicNames(); }
            @Override public @NotNull Set<String> targeterNames() { return delegate.targeterNames(); }
            @Override public @NotNull Set<String> conditionNames() { return delegate.conditionNames(); }
            @Override public @NotNull Set<String> blockNames() { return declaredLower; }
        };
    }

    private static void collectYaml(@NotNull File dir, @NotNull List<File> out) {
        File[] children = dir.listFiles();
        if (children == null)
            return;
        for (File child : children) {
            if (child.isDirectory())
                collectYaml(child, out);
            else if (child.getName().endsWith(".yml") || child.getName().endsWith(".yaml"))
                out.add(child);
        }
    }

    private static @NotNull Loc wholeKey(@NotNull File file, @NotNull String configPath, @NotNull String key) {
        return new Loc(new SourceRef(file, configPath, -1, key), Span.of(0, 0, key.length()));
    }
}
