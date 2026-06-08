package me.deecaad.core.file.verify;

import me.deecaad.core.compatibility.HeadlessOperationException;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.SnakeYamlConfig;
import me.deecaad.core.file.serializers.ItemSerializer;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the headless-serialization boundary the config verifier relies on: real serializers run under
 * MockBukkit and validate values, except genuinely NMS-backed features (item NBT Tags), which fail
 * with the typed {@link HeadlessOperationException} so the verifier can report them as "in-game only".
 */
class HeadlessSerializationCoverageTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    private static void item(String yaml) throws Exception {
        SnakeYamlConfig config = SnakeYamlConfig.ofText(yaml);
        new ItemSerializer().serialize(new SerializeData(new File("item.yml"), "Item", config));
    }

    @Test
    void registryAndRangeFeatures_validateHeadless() {
        // Material, enchantments, food, durability, equip sound, leather color, skull: all headless.
        assertDoesNotThrow(() -> item("""
            Item:
              Type: DIAMOND_HELMET
              Enchantments:
                - sharpness-5
              Unbreakable: true
              Custom_Model_Data: 7
              Equippable:
                Slot: HEAD
                Equip_Sound: item.armor.equip_diamond
            """));
        assertDoesNotThrow(() -> item("Item:\n  Type: PLAYER_HEAD\n  Skull_Owning_Player: Notch\n"));
    }

    @Test
    void badValues_areConfigErrorsHeadless() {
        assertThrows(SerializerException.class, () -> item("Item:\n  Type: NOT_A_REAL_MATERIAL\n"));
        assertThrows(SerializerException.class, () -> item("Item:\n  Type: DIAMOND_SWORD\n  Max_Stack_Size: 999\n"));
        assertThrows(SerializerException.class,
            () -> item("Item:\n  Type: DIAMOND_HELMET\n  Equippable:\n    Slot: HEAD\n    Equip_Sound: NOT_A_SOUND\n"));
    }

    @Test
    void nmsBackedFeature_throwsTypedHeadlessSignal() {
        // NBT tags need net.minecraft.server, absent under MockBukkit: a typed, catchable signal.
        assertThrows(HeadlessOperationException.class,
            () -> item("Item:\n  Type: DIAMOND_SWORD\n  Tags:\n    - \"myplugin:mykey 5\"\n"));
    }
}
