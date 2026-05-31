package me.deecaad.core.mechanics.program;

import me.deecaad.core.mechanics.scope.CastScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A parsed mechanics program: a set of named {@link MechanicBlock blocks} plus
 * the name of the entry block. Block calls resolve against this program's blocks
 * first (so a list's own recursion and sibling calls work), then fall back to the
 * global {@link GlobalBlocks} registry for blocks defined in the mechanics/ folder.
 */
public final class Program {

    private final @NotNull Map<String, MechanicBlock> blocks;
    private final @NotNull String entry;

    public Program(@NotNull Map<String, MechanicBlock> blocks, @NotNull String entry) {
        this.blocks = Map.copyOf(blocks);
        this.entry = entry;
    }

    public @Nullable MechanicBlock getBlock(@NotNull String name) {
        return blocks.get(name);
    }

    public @NotNull MechanicBlock getEntry() {
        return blocks.get(entry);
    }

    public @NotNull Map<String, MechanicBlock> blocks() {
        return blocks;
    }

    public @NotNull String entry() {
        return entry;
    }

    /**
     * Runs the entry block against the given scope. Block calls resolve to a local
     * block first, then fall back to the global registry, so a list can call blocks
     * defined in the {@code mechanics/} folder.
     */
    public void run(@NotNull CastScope scope) {
        new MechanicExecutor(this::resolveBlock).execute(getEntry(), scope);
    }

    private @Nullable MechanicBlock resolveBlock(@NotNull String name) {
        MechanicBlock local = getBlock(name);
        return local != null ? local : GlobalBlocks.get(name);
    }
}
