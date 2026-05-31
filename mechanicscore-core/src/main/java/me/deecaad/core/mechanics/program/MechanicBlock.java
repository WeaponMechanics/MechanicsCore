package me.deecaad.core.mechanics.program;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A named, ordered list of {@link Statement statements}. Defining a block makes
 * it callable by name (like a function), which is how recursion and reuse work.
 */
public final class MechanicBlock {

    private final @NotNull String name;
    private final @NotNull List<Statement> statements;

    public MechanicBlock(@NotNull String name, @NotNull List<Statement> statements) {
        this.name = name;
        this.statements = List.copyOf(statements);
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull List<Statement> statements() {
        return statements;
    }
}
