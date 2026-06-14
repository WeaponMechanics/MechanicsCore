package me.deecaad.core.mechanics.scope;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * An optional interface for {@link CastScope} attachments that contribute placeholders. When an
 * attachment implementing this is set on a scope, its entries are merged into the scope's
 * placeholder map (and retracted if the attachment is replaced).
 *
 * <p>Keys are bare (no angle brackets, e.g. {@code "shooter_weapon_title"}) and MUST be namespaced
 * to avoid colliding with the context/variable placeholders ({@code source_*}, {@code target_*},
 * {@code $variables}) that share the same flat map.
 */
public interface CastAttachment {

    @NotNull Map<String, String> placeholders();
}
