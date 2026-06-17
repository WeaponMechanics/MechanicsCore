package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A named block of parsed statements.
 */
public record BlockNode(@NotNull String name, @NotNull List<StmtNode> statements) {
}
