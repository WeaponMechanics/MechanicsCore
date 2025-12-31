package me.deecaad.core.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for working with Adventure components on items.
 */
public final class AdventureUtil {

    private AdventureUtil() {
    }

    /**
     * Returns the display name of the item in adventure format.
     *
     * @param item The item get the name from.
     * @return The name component.
     */
    public static @NotNull Component getName(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName())
            return Component.empty();

        return meta.displayName();
    }

    /**
     * Sets the display name of the item. Parsed using MiniMessage.
     *
     * @param item The item to set the name.
     * @param name The value of the name.
     */
    public static void setNameUnparsed(@NotNull ItemStack item, @NotNull String name) {
        ItemMeta meta = item.getItemMeta();
        setNameUnparsed(Objects.requireNonNull(meta), name);
        item.setItemMeta(meta);
    }

    /**
     * Sets the display name of the item's meta. Parsed using MiniMessage.
     *
     * @param meta The meta to set the name.
     * @param name The value of the name.
     */
    public static void setNameUnparsed(@NotNull ItemMeta meta, @NotNull String name) {
        // <!italic> effectively strips away Minecraft's predefined formatting
        setName(meta, MiniMessage.miniMessage().deserialize("<!italic>" + name));
    }

    /**
     * Sets the display name of the item.
     *
     * @param item The item to set the name.
     * @param name The value of the name.
     */
    public static void setName(@NotNull ItemStack item, @NotNull Component name) {
        ItemMeta meta = item.getItemMeta();
        setName(Objects.requireNonNull(meta), name);
        item.setItemMeta(meta);
    }

    /**
     * Sets the display name of the item's meta.
     *
     * @param meta The meta to set the name.
     * @param name The value of the name.
     */
    public static void setName(@NotNull ItemMeta meta, @NotNull Component name) {
        meta.displayName(name);
    }

    public static @Nullable List<Component> getLore(@NotNull ItemStack item) {
        return getLore(Objects.requireNonNull(item.getItemMeta()));
    }

    public static @Nullable List<Component> getLore(@NotNull ItemMeta meta) {
        return meta.lore();
    }

    /**
     * Sets the lore of the item.
     *
     * <p>
     * The list should be a list of strings (or any list where {@link Object#toString()} is acceptable).
     * The strings are then parsed using MiniMessage, then set as the lore. This is quite slow, so
     * consider parsing the list with MiniMessage first then setting it using
     * {@link #setLore(ItemStack, List)}.
     *
     * @param item The item to set the lore.
     * @param unparsedText The list of strings.
     */
    public static void setLoreUnparsed(@NotNull ItemStack item, @NotNull List<?> unparsedText) {
        ItemMeta meta = item.getItemMeta();
        setLoreUnparsed(Objects.requireNonNull(meta), unparsedText);
        item.setItemMeta(meta);
    }

    /**
     * Sets the lore of the item meta.
     *
     * <p>
     * The list should be a list of strings (or any list where {@link Object#toString()} is acceptable).
     * The strings are then parsed using MiniMessage, then set as the lore. This is quite slow, so
     * consider parsing the list with MiniMessage first then setting it using
     * {@link #setLore(ItemStack, List)}.
     *
     * @param meta The item meta to set the lore.
     * @param unparsedText The list of strings.
     */
    public static void setLoreUnparsed(@NotNull ItemMeta meta, @NotNull List<?> unparsedText) {
        List<Component> lore = new ArrayList<>(unparsedText.size());
        for (Object obj : unparsedText) {
            // <!italic> effectively strips away Minecraft's predefined formatting
            Component component = MiniMessage.miniMessage().deserialize("<!italic>" + StringUtil.colorAdventure(obj.toString()));
            lore.add(component);
        }
        meta.lore(lore);
    }

    /**
     * Sets the lore of the item.
     *
     * @param item The item to set the lore.
     * @param lines The list of adventure components for lore.
     */
    public static void setLore(@NotNull ItemStack item, @NotNull List<Component> lines) {
        ItemMeta meta = item.getItemMeta();
        setLore(Objects.requireNonNull(meta), lines);
        item.setItemMeta(meta);
    }

    /**
     * Sets the lore of the item.
     *
     * @param meta The item to set the lore.
     * @param lines The list of adventure components for lore.
     */
    public static void setLore(@NotNull ItemMeta meta, @NotNull List<Component> lines) {
        meta.lore(lines);
    }

    /**
     * Replaces any PlaceholderAPI placeholders present in the display name and lore of the item. If
     * PlaceholderAPI is not installed, this method is skipped.
     *
     * @param player The player holding the item.
     * @param itemStack The item to apply the placeholders to.
     */
    public static void updatePlaceholders(Player player, ItemStack itemStack) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null)
            return;

        Component displayName = itemMeta.displayName();
        if (displayName != null) {
            String name = GsonComponentSerializer.gson().serialize(displayName);
            name = PlaceholderAPI.setPlaceholders(player, name);
            itemMeta.displayName(GsonComponentSerializer.gson().deserialize(name));
        }

        List<Component> lore = itemMeta.lore();
        if (lore != null && !lore.isEmpty()) {
            List<String> loreStrings = new ArrayList<>(lore.size());
            for (Component component : lore)
                loreStrings.add(GsonComponentSerializer.gson().serialize(component));

            PlaceholderAPI.setPlaceholders(player, loreStrings);

            for (int i = 0; i < loreStrings.size(); i++)
                lore.set(i, GsonComponentSerializer.gson().deserialize(loreStrings.get(i)));

            itemMeta.lore(lore);
        }

        itemStack.setItemMeta(itemMeta);
    }
}
