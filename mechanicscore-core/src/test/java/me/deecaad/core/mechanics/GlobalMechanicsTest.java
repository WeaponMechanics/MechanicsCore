package me.deecaad.core.mechanics;

import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.diagnostic.DiagnosticReporter;
import me.deecaad.core.mechanics.expression.ExpressionParser;
import me.deecaad.core.mechanics.parse.StatementParser;
import me.deecaad.core.mechanics.program.GlobalBlocks;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.program.Subject;
import me.deecaad.core.mechanics.scope.CastBudget;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Value;
import me.deecaad.core.mechanics.sema.SemanticAnalyzer;
import me.deecaad.core.mechanics.sema.SymbolSource;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalMechanicsTest {

    /** A SymbolSource that knows no built-ins but exposes a fixed set of global block names. */
    private record StubSymbols(Set<String> globals) implements SymbolSource {
        @Override public @Nullable Mechanic mechanic(@NotNull String name) { return null; }
        @Override public @Nullable Targeter targeter(@NotNull String name) { return null; }
        @Override public @Nullable Condition condition(@NotNull String name) { return null; }
        @Override public @NotNull Set<String> mechanicNames() { return Set.of(); }
        @Override public @NotNull Set<String> targeterNames() { return Set.of(); }
        @Override public @NotNull Set<String> conditionNames() { return Set.of(); }
        @Override public @NotNull Set<String> blockNames() { return globals; }
    }

    @AfterEach
    void tearDown() {
        GlobalBlocks.clear();
    }

    private static CastScope scope() {
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        Mockito.when(entity.getName()).thenReturn("Bob");
        Location location = new Location(null, 0, 0, 0);
        Mockito.when(entity.getLocation()).thenReturn(location);
        Mockito.when(entity.getEyeLocation()).thenReturn(location);
        return CastScope.builder(entity).budget(CastBudget.defaults()).build();
    }

    @Test
    void semaResolvesAGlobalOnlyNameAsABlockCall() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        StmtNode node = StatementParser.parse("helper{}", 0, new File("t.yml"), "Mechanics", reporter);
        ProgramNode programNode = new ProgramNode(Map.of("Mechanics", new BlockNode("Mechanics", List.of(node))), "Mechanics");

        Program program = new SemanticAnalyzer(new StubSymbols(Set.of("helper")))
            .analyze(programNode, new File("t.yml"), reporter);

        assertFalse(reporter.hasErrors(), () -> "a known global block should not be 'unknown': " + reporter.all());
        Statement.BlockInvocation call = assertInstanceOf(Statement.BlockInvocation.class, program.getEntry().statements().get(0));
        assertEquals("helper", call.blockName());
    }

    @Test
    void normalProgramCallsAGlobalBlockAndSharesScope() {
        // Global block "compute": $result = $input * 2
        MechanicBlock compute = new MechanicBlock("compute",
            List.of(new Statement.Assignment("result", ExpressionParser.parse("$input * 2"))));
        GlobalBlocks.install(Map.of("compute", compute));

        // Main (no local "compute"): $input = 5; compute @source
        Statement init = new Statement.Assignment("input", ExpressionParser.parse("5"));
        Statement call = new Statement.BlockInvocation("compute", new Subject.Reference(CastScope.SOURCE), List.of());
        MechanicBlock main = new MechanicBlock("Main", List.of(init, call));

        CastScope scope = scope();
        new Program(Map.of("Main", main), "Main").run(scope);

        Value result = scope.getVariable("result");
        assertNotNull(result, "global block should have run via the registry fallback");
        assertEquals(10.0, result.asNumber(), "global block wrote to the shared scope");
    }

    @Test
    void loaderCompilesCrossCallingAndRecursiveBlocks() {
        Map<String, GlobalMechanicsLoader.Decl> declared = Map.of(
            "alpha", decl("beta{}"),                   // calls another global block
            "beta", decl("$x = 1"),                    // leaf
            "loop", decl("loop{} @target"));           // self-recursive

        DiagnosticReporter reporter = new DiagnosticReporter();
        Map<String, MechanicBlock> compiled = GlobalMechanicsLoader.compile(
            declared, new StubSymbols(Set.of("alpha", "beta", "loop")), reporter);

        assertFalse(reporter.hasErrors(), () -> reporter.all().toString());
        assertEquals(Set.of("alpha", "beta", "loop"), compiled.keySet());

        // alpha's cross-call to beta stays a runtime BlockInvocation (not inlined).
        assertInstanceOf(Statement.BlockInvocation.class, compiled.get("alpha").statements().get(0));
        // the self-recursive call survives optimization.
        assertInstanceOf(Statement.BlockInvocation.class, compiled.get("loop").statements().get(0));
    }

    private static GlobalMechanicsLoader.Decl decl(String... lines) {
        return new GlobalMechanicsLoader.Decl(new File("t.yml"), "block.Mechanics", new ArrayList<>(List.of(lines)));
    }
}
