package me.deecaad.core.mechanics.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The parsed program: named blocks plus the entry block name.
 */
public record ProgramNode(@NotNull Map<String, BlockNode> blocks, @NotNull String entry) {
}
