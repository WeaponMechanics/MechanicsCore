package me.deecaad.core.mechanics.program;

import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.optimize.Optimizer;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.sema.SemanticAnalyzer;
import me.deecaad.core.mechanics.sema.SymbolSource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compiles a single block's statement lines into an optimized {@link Program}:
 * parse (AST + spans) to sema (resolve + typecheck) to optimize. Diagnostics are
 * recorded on the given reporter; the caller decides how to surface them. Shared
 * by {@link MechanicSerializer} (a config {@code Mechanics:} list) and the global
 * {@code mechanics/} folder loader.
 */
public final class MechanicCompiler {

    private MechanicCompiler() {
    }

    public static @NotNull Program compile(@NotNull String blockName, @NotNull String configPath,
                                           @NotNull List<String> lines, @NotNull File file,
                                           @NotNull SymbolSource symbols, @NotNull DiagnosticReporter reporter) {
        List<StmtNode> nodes = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++)
            nodes.add(StatementParser.parse(lines.get(i), i, file, configPath, reporter));

        ProgramNode programNode = new ProgramNode(Map.of(blockName, new BlockNode(blockName, nodes)), blockName);
        return Optimizer.optimize(new SemanticAnalyzer(symbols).analyze(programNode, file, reporter));
    }
}
