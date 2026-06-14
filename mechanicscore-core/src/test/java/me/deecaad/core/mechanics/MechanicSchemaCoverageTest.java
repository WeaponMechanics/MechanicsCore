package me.deecaad.core.mechanics;

import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.targeters.Targeter;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every registered mechanic/targeter/condition must declare a {@link ConfigSchema} that builds
 * headlessly. This catches a missing schema (null) and a schema that crashes to build.
 * Registries populate without a live plugin because getKey() uses {@code MechanicsCore.NAMESPACE};
 * MockBukkit backs the Bukkit registries some schemas reference (e.g. DropItem's embedded item).
 */
class MechanicSchemaCoverageTest {

    @BeforeAll
    static void boot() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void everyMechanicHasABuildableSchema() {
        for (Mechanic mechanic : Mechanics.REGISTRY) {
            ConfigSchema schema = mechanic.schema();
            assertNotNull(schema, () -> "mechanic '" + mechanic.getKey() + "' has no schema()");
        }
    }

    @Test
    void everyTargeterHasABuildableSchema() {
        for (Targeter targeter : Targeters.REGISTRY) {
            ConfigSchema schema = targeter.schema();
            assertNotNull(schema, () -> "targeter '" + targeter.getKey() + "' has no schema()");
        }
    }

    @Test
    void everyConditionHasABuildableSchema() {
        for (Condition condition : Conditions.REGISTRY) {
            ConfigSchema schema = condition.schema();
            assertNotNull(schema, () -> "condition '" + condition.getKey() + "' has no schema()");
        }
    }
}
