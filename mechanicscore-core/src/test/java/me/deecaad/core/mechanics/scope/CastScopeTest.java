package me.deecaad.core.mechanics.scope;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastScopeTest {

    private static LivingEntity mockEntity(String name) {
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        Mockito.when(entity.getName()).thenReturn(name);
        Location location = new Location(null, 1, 2, 3);
        Mockito.when(entity.getLocation()).thenReturn(location);
        Mockito.when(entity.getEyeLocation()).thenReturn(location);
        return entity;
    }

    private static CastScope newScope() {
        return CastScope.builder(mockEntity("Bob")).build();
    }

    @Test
    void numberValueFormatsWholeNumbersWithoutDecimal() {
        assertEquals("5", Value.of(5.0).asString());
        assertEquals("5.5", Value.of(5.5).asString());
        assertEquals(5.0, Value.of(5.0).asNumber());
        assertTrue(Value.of(1.0).asBoolean());
        assertFalse(Value.of(0.0).asBoolean());
    }

    @Test
    void stringValueParsesNumberAndBoolean() {
        assertEquals(3.5, Value.of("3.5").asNumber());
        assertEquals(0.0, Value.of("nope").asNumber());
        assertTrue(Value.of("x").asBoolean());
        assertFalse(Value.of("").asBoolean());
    }

    @Test
    void contextBasics() {
        assertTrue(Context.empty().isEmpty());
        Target a = Target.of(new Location(null, 0, 0, 0));
        Target b = Target.of(new Location(null, 1, 1, 1));
        Context context = Context.of(a, b);
        assertEquals(2, context.size());
        assertSame(a, context.first());
    }

    @Test
    void builderDefaultsTargetToSource() {
        CastScope scope = newScope();
        assertNotNull(scope.getContext(CastScope.SOURCE));
        assertNotNull(scope.getContext(CastScope.TARGET));
    }

    @Test
    void variablesAreSharedAndMutableInPlace() {
        CastScope scope = newScope();
        scope.setVariable("dmg", Value.of(10));
        assertEquals(10.0, scope.getVariable("dmg").asNumber());

        // One scope per cast: a later write overwrites in place (no fork/isolation).
        scope.setVariable("dmg", Value.of(1));
        assertEquals(1.0, scope.getVariable("dmg").asNumber());
    }

    @Test
    void budgetEnforcesDepthCap() {
        CastBudget budget = new CastBudget(2, 100);
        budget.checkDepth(2);
        assertThrows(CastAbortException.class, () -> budget.checkDepth(3));
    }

    @Test
    void budgetEnforcesActionCap() {
        CastBudget budget = new CastBudget(100, 3);
        budget.chargeAction();
        budget.chargeAction();
        budget.chargeAction();
        assertThrows(CastAbortException.class, budget::chargeAction);
    }
}
