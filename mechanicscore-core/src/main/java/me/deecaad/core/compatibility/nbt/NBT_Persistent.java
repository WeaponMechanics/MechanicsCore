package me.deecaad.core.compatibility.nbt;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Isolates {@link PersistentDataType} usage so it is not class-loaded on 1.12.2, where it does not
 * exist. {@link NBTCompatibility} must not reference it directly.
 */
public abstract class NBT_Persistent implements NBTCompatibility {

    public boolean hasString(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key) {
        ItemMeta meta = bukkitItem.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getKey(plugin, key), PersistentDataType.STRING);
    }

    public String getString(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, String def) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return def;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        return nbt.getOrDefault(getKey(plugin, key), PersistentDataType.STRING, def);
    }

    public void setString(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, String value) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(bukkitItem.getType());
        if (meta == null) return; // AIR (or truly meta-less)

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        nbt.set(getKey(plugin, key), PersistentDataType.STRING, value);
        bukkitItem.setItemMeta(meta);
    }

    @Override
    public boolean hasInt(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key) {
        ItemMeta meta = bukkitItem.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getKey(plugin, key), PersistentDataType.INTEGER);
    }

    public int getInt(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, int def) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return def;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        return nbt.getOrDefault(getKey(plugin, key), PersistentDataType.INTEGER, def);
    }

    public void setInt(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, int value) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(bukkitItem.getType());
        if (meta == null) return;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        nbt.set(getKey(plugin, key), PersistentDataType.INTEGER, value);
        bukkitItem.setItemMeta(meta);
    }

    public boolean hasDouble(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(getKey(plugin, key), PersistentDataType.DOUBLE);
    }

    public double getDouble(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, double def) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return def;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        return nbt.getOrDefault(getKey(plugin, key), PersistentDataType.DOUBLE, def);
    }

    public void setDouble(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, double value) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(bukkitItem.getType());
        if (meta == null) return; // AIR / meta-less

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        nbt.set(getKey(plugin, key), PersistentDataType.DOUBLE, value);
        bukkitItem.setItemMeta(meta);
    }

    public boolean hasArray(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(getKey(plugin, key), PersistentDataType.INTEGER_ARRAY);
    }

    public int[] getArray(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, int[] def) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return def;

        int[] result = meta.getPersistentDataContainer().get(getKey(plugin, key), PersistentDataType.INTEGER_ARRAY);
        return result != null ? result : def;
    }

    public void setArray(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, int[] value) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null || value == null) return;

        meta.getPersistentDataContainer().set(getKey(plugin, key), PersistentDataType.INTEGER_ARRAY, value);
        bukkitItem.setItemMeta(meta);
    }

    public boolean hasStringArray(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(getKey(plugin, key), StringPersistentType.INSTANCE);
    }

    public String[] getStringArray(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, String[] def) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return def;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        String[] result = nbt.get(getKey(plugin, key), StringPersistentType.INSTANCE);
        return result != null ? result : def;
    }

    public void setStringArray(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key, String[] value) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(bukkitItem.getType());
        if (meta == null) return; // AIR / truly meta-less

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        if (value == null) {
            nbt.remove(getKey(plugin, key));
        } else {
            nbt.set(getKey(plugin, key), StringPersistentType.INSTANCE, value);
        }

        bukkitItem.setItemMeta(meta);
    }

    public void remove(@NotNull ItemStack bukkitItem, @NotNull String plugin, @NotNull String key) {
        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        nbt.remove(getKey(plugin, key));
        bukkitItem.setItemMeta(meta);
    }
}
