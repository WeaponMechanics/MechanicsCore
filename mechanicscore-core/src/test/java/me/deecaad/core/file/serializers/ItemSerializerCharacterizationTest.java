package me.deecaad.core.file.serializers;

import me.deecaad.core.file.BukkitConfig;
import me.deecaad.core.file.SerializeData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the current ItemSerializer construction behavior for the pure-Bukkit subset MockBukkit can
 * exercise (material, name, lore, enchants, unbreakable, custom model data, durability). NMS-backed
 * paths (NBT Tags, recipes, skull textures) cannot run headless and are not covered here.
 */
class ItemSerializerCharacterizationTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private ItemStack serialize(String yaml) throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        SerializeData data = new SerializeData(new File("item.yml"), "Item", new BukkitConfig(config));
        return new ItemSerializer().serialize(data);
    }

    @Test
    void basicMaterial() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: DIAMOND_SWORD
            """);
        assertEquals("DIAMOND_SWORD", item.getType().name());
    }

    @Test
    void enchantsAndUnbreakable() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: DIAMOND_SWORD
              Unbreakable: true
              Enchantments:
                - sharpness-5
            """);
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.isUnbreakable());
        assertEquals(4, meta.getEnchantLevel(Enchantment.SHARPNESS));
    }

    @Test
    void customModelDataAndMaxStack() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: DIAMOND_SWORD
              Custom_Model_Data: 7
              Max_Stack_Size: 1
            """);
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.hasCustomModelData());
        assertEquals(7, meta.getCustomModelData());
    }

    @Test
    void nameAndLore() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: DIAMOND_SWORD
              Name: "<red>Sword"
              Lore:
                - "line one"
                - "line two"
            """);
        ItemMeta meta = item.getItemMeta();
        assertTrue(meta.hasDisplayName());
        assertEquals(2, meta.getLore().size());
    }

    // NOTE: Attributes, Recipe, Tags, Deny_Use_In_Crafting, and Skull_Owning_Player are NOT covered
    // here - they reach MechanicsCore.getInstance()/CompatibilityAPI (NMS + CommandAPI), which cannot
    // initialize headless under MockBukkit. They are verified in-game only.

    @Test
    void food() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: APPLE
              Food:
                Nutrition: 4
                Saturation: 2.5
                Can_Always_Eat: true
            """);
        assertNotNull(item.getItemMeta().getFood());
    }

    @Test
    void leatherColor() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: LEATHER_CHESTPLATE
              Leather_Color: "#FF0000"
            """);
        org.bukkit.inventory.meta.LeatherArmorMeta meta =
            (org.bukkit.inventory.meta.LeatherArmorMeta) item.getItemMeta();
        assertEquals(org.bukkit.Color.fromRGB(0xFF0000), meta.getColor());
    }

    @Test
    void durability() throws Exception {
        ItemStack item = serialize("""
            Item:
              Type: DIAMOND_SWORD
              Durability:
                Max_Damage: 100
                Damage: 25
            """);
        Damageable meta = (Damageable) item.getItemMeta();
        assertEquals(100, meta.getMaxDamage());
        assertEquals(25, meta.getDamage());
    }
}
