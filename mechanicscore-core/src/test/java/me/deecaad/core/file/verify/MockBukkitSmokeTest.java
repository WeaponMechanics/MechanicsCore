package me.deecaad.core.file.verify;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockBukkitSmokeTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void server_bootsHeadless() {
        assertNotNull(MockBukkit.getMock());
    }

    @Test
    void registries_areBacked() {
        assertNotNull(Material.DIAMOND_SWORD);
        Registry<Enchantment> enchants = Registry.ENCHANTMENT;
        assertNotNull(enchants);
        assertNotNull(enchants.get(org.bukkit.NamespacedKey.minecraft("sharpness")));
    }

    @Test
    void xmaterial_parses() {
        assertTrue(com.cryptomorin.xseries.XMaterial.matchXMaterial("DIAMOND_SWORD").isPresent());
    }
}
