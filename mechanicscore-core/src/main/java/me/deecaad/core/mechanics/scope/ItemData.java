package me.deecaad.core.mechanics.scope;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The item involved in a cast (e.g. the weapon), its display title, and the slot it occupies. A
 * built-in {@link CastScope} attachment that backs the {@code item}/{@code itemTitle}/{@code slot}
 * accessors used by placeholder handlers.
 */
public record ItemData(@Nullable ItemStack item, @Nullable String title, @Nullable EquipmentSlot slot) {
}
