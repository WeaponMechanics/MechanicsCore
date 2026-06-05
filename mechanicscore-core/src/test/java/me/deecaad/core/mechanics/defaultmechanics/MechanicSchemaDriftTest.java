package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.file.BukkitConfig;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drift guard: a serializer's {@link ConfigSchema} must declare exactly the keys its
 * {@code serialize()} reads. {@link SerializeData}'s recording mode captures every key accessed
 * during a serialize() run; we assert that set equals the declared schema keys (normalized). Drift
 * in either direction - a declared key never read, or a key read but not declared - fails here,
 * keeping schema() honest as more mechanics migrate.
 */
class MechanicSchemaDriftTest {

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    private static Set<String> normalized(Set<String> keys) {
        return keys.stream().map(MechanicSchemaDriftTest::norm).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> declaredKeys(ConfigSchema schema) {
        return schema.keys().stream().map(k -> norm(k.name())).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> recordReads(Mechanic mechanic, String body) throws SerializerException {
        String yaml = "Mechanic:\n" + body.replaceAll("(?m)^", "  ");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
        SerializeData data = new SerializeData(new File("mechanic.yml"), "Mechanic", new BukkitConfig(config));

        SerializeData.startRecording();
        try {
            mechanic.serialize(data);
        } finally {
            // stopRecording is also called below; ensure recording is off even on failure.
        }
        return normalized(SerializeData.stopRecording());
    }

    @Test
    void damageMechanic_schemaMatchesSerializeReads() throws SerializerException {
        DamageMechanic mechanic = new DamageMechanic();
        Set<String> read = recordReads(mechanic, """
            Damage: 5.0
            Ignore_Armor: true
            Reset_Cooldown: true
            Repeat_Amount: 2
            Repeat_Interval: 3
            Delay_Before_Play: 4
            Chance: 50%
            """);
        assertEquals(declaredKeys(mechanic.schema()), read,
            "DamageMechanic.serialize() reads must match its schema() declarations");
    }

    @Test
    void sculkBloomMechanic_schemaIncludesBlockParentArgs() throws SerializerException {
        SculkBloomMechanic mechanic = new SculkBloomMechanic();
        Set<String> read = recordReads(mechanic, """
            Charge: 5
            Max_Blocks: 3
            Search_Radius: 8.0
            Repeat_Amount: 2
            Repeat_Interval: 3
            Delay_Before_Play: 4
            Chance: 50%
            """);
        assertEquals(declaredKeys(mechanic.schema()), read,
            "SculkBloomMechanic must declare ActivateBlockMechanic's Max_Blocks/Search_Radius parent args");
    }
}
