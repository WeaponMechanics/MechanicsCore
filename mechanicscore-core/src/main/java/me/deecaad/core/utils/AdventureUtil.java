package me.deecaad.core.utils;

import com.cjcrafter.foliascheduler.util.FieldAccessor;
import com.cjcrafter.foliascheduler.util.MethodInvoker;
import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import me.deecaad.core.MechanicsCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 * MechanicsCore is built to be craftbukkit compatible to reduce support requests. We do not use
 * Paper code since we upload premium plugins on Spigot. Unfortunately, this means we must find
 * "creative" solutions when using the Adventure chat API with Minecraft code.
 *
 * <p>
 * This class adds methods to:
 * <ul>
 * <li>Set item display name</li>
 * <li>Set item lore</li>
 * </ul>
 */
public final class AdventureUtil {

    // 1.16+ use adventure in item lore and display name (hex code support)
    public static FieldAccessor loreField;
    public static FieldAccessor displayField;
    public static MethodInvoker fromJsonMethod;
    public static MethodInvoker toJsonMethod;

    static {
        if (MinecraftVersions.NETHER_UPDATE.isAtLeast()) { // before 1.16, hex was not supported by MC
            Class<?> c = ReflectionUtil.getCraftBukkitClass("inventory.CraftMetaItem");
            loreField = ReflectionUtil.getField(c, "lore");
            displayField = ReflectionUtil.getField(c, "displayName");
        }
        if (MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast()) {
            Class<?> c = ReflectionUtil.getCraftBukkitClass("util.CraftChatMessage");
            Class<?> componentClass = ReflectionUtil.getMinecraftClass("network.chat", "IChatBaseComponent");
            fromJsonMethod = ReflectionUtil.getMethod(c, "fromJSON", String.class);
            toJsonMethod = ReflectionUtil.getMethod(c, "toJSON", componentClass);
        }
    }

    /**
     * Don't let anyone instantiate this class.
     */
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
        if (meta == null)
            return Component.empty();

        if (MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast()) {
            Object component = displayField.get(meta);
            String json = (String) toJsonMethod.invoke(null, component);
            return GsonComponentSerializer.gson().deserialize(json);
        } else if (MinecraftVersions.NETHER_UPDATE.isAtLeast()) {
            return GsonComponentSerializer.gson().deserialize((String) displayField.get(meta));
        } else {
            return LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
        }
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
        setName(meta, MechanicsCore.getPlugin().message.deserialize("<!italic>" + name));
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
        // before 1.16, hex was not supported
        if (MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast()) {
            String json = GsonComponentSerializer.gson().serialize(name);
            Object component = fromJsonMethod.invoke(null, json);
            displayField.set(meta, component);
        } else if (MinecraftVersions.NETHER_UPDATE.isAtLeast()) {
            String str = GsonComponentSerializer.gson().serialize(name);
            displayField.set(meta, str);
        } else {
            String str = LegacyComponentSerializer.legacySection().serialize(name);
            meta.setDisplayName(str);
        }
    }

    @Nullable public static List<Component> getLore(@NotNull ItemStack item) {
        return getLore(Objects.requireNonNull(item.getItemMeta()));
    }

    @Nullable public static List<Component> getLore(@NotNull ItemMeta meta) {
        List<?> lore = !MinecraftVersions.NETHER_UPDATE.isAtLeast()
            ? meta.getLore()
            : (List<String>) loreField.get(meta);

        if (lore == null)
            return null;

        List<Component> components = new ArrayList<>(lore.size());
        for (Object line : lore) {
            if (MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast()) {
                String json = (String) toJsonMethod.invoke(null, line);
                components.add(GsonComponentSerializer.gson().deserialize(json));
            } else if (MinecraftVersions.NETHER_UPDATE.isAtLeast()) {
                components.add(GsonComponentSerializer.gson().deserialize((String) line));
            } else {
                components.add(LegacyComponentSerializer.legacySection().deserialize((String) line));
            }
        }

        return components;
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
        List<Object> lore = new ArrayList<>(unparsedText.size());
        for (Object obj : unparsedText) {
            // <!italic> effectively strips away Minecraft's predefined formatting
            Component component = MechanicsCore.getPlugin().message.deserialize("<!italic>" + StringUtil.colorAdventure(obj.toString()));
            if (MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast()) {
                String json = GsonComponentSerializer.gson().serialize(component);
                Object componentObj = fromJsonMethod.invoke(null, json);
                lore.add(componentObj);
            } else if (MinecraftVersions.NETHER_UPDATE.isAtLeast()) {
                lore.add(GsonComponentSerializer.gson().serialize(component));
            } else {
                lore.add(LegacyComponentSerializer.legacySection().serialize(component));
            }
        }

        if (!MinecraftVersions.NETHER_UPDATE.isAtLeast())
            meta.setLore((List<String>) (List<?>) lore);
        else
            loreField.set(meta, lore);
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
        List<Object> lore = new ArrayList<>(lines.size());
        for (Component component : lines) {
            if (MinecraftVersions.TRAILS_AND_TAILS.get(5).isAtLeast()) {
                String json = GsonComponentSerializer.gson().serialize(component);
                Object componentObj = fromJsonMethod.invoke(null, json);
                lore.add(componentObj);
            } else if (MinecraftVersions.NETHER_UPDATE.isAtLeast()) {
                lore.add(GsonComponentSerializer.gson().serialize(component));
            } else {
                lore.add(LegacyComponentSerializer.legacySection().serialize(component));
            }
        }

        if (!MinecraftVersions.NETHER_UPDATE.isAtLeast())
            meta.setLore((List<String>) (List<?>) lore);
        else
            loreField.set(meta, lore);
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

        // Slow AF, but this isn't done often and there isn't really a faster way
        // (except by skipping placeholders)
        String name = GsonComponentSerializer.gson().serialize(AdventureUtil.getName(itemStack));
        name = PlaceholderAPI.setPlaceholders(player, name);
        AdventureUtil.setName(itemMeta, GsonComponentSerializer.gson().deserialize(name));

        List<Component> lore = AdventureUtil.getLore(itemMeta);
        if (lore != null && !lore.isEmpty()) {
            // Convert components to strings
            List<String> loreStrings = new ArrayList<>(lore.size());
            for (Component component : lore)
                loreStrings.add(GsonComponentSerializer.gson().serialize(component));

            // Let placeholderapi do its thing
            PlaceholderAPI.setPlaceholders(player, loreStrings);

            // convert strings back to components
            for (int i = 0; i < loreStrings.size(); i++)
                lore.set(i, GsonComponentSerializer.gson().deserialize(loreStrings.get(i)));

            AdventureUtil.setLore(itemMeta, lore);
        }

        itemStack.setItemMeta(itemMeta);
    }
}
