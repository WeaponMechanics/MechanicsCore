package me.deecaad.core.compatibility.entity;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface EquipmentChangeConsumer {
    void accept(@Nullable ItemStack oldItem, @Nullable ItemStack newItem, @NotNull EquipmentSlot slot);
}
