package me.deecaad.core.mechanics.ast;

import me.deecaad.core.file.MapConfigLike;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A parsed {@code Name{key=value, ...}} call, before sema decides whether it is
 * a mechanic, targeter, condition, or block. {@code args} is the raw
 * {@code InlineSerializer.inlineFormat} output (preserving nested maps/lists) so
 * sema can reconstruct a {@code SerializeData} and reuse the existing
 * {@code serialize} methods. Arg column spans derive from {@code loc.span().start()}
 * plus each {@link MapConfigLike.Holder#index()}.
 */
public record InlineCallNode(@NotNull String name, @NotNull Loc nameLoc,
                             @NotNull Map<String, MapConfigLike.Holder> args, @NotNull Loc loc) {
}
