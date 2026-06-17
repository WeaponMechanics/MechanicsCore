package me.deecaad.core.mechanics.scope;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // Attachments

    private record Marker(String id) {
    }

    private static class Super {
    }

    private static final class Sub extends Super {
    }

    private record TitleAttachment(String title) implements CastAttachment {
        @Override
        public Map<String, String> placeholders() {
            return Map.of("shooter_weapon_title", title);
        }
    }

    @Test
    void attachmentRoundTrips() {
        CastScope scope = newScope();
        Marker marker = new Marker("a");
        scope.setAttachment(Marker.class, marker);
        assertSame(marker, scope.getAttachment(Marker.class));
    }

    @Test
    void attachmentAbsentReturnsNullAndHasFalse() {
        CastScope scope = newScope();
        assertNull(scope.getAttachment(Marker.class));
        assertFalse(scope.hasAttachment(Marker.class));
    }

    @Test
    void hasAttachmentReflectsPresence() {
        CastScope scope = newScope();
        assertFalse(scope.hasAttachment(Marker.class));
        scope.setAttachment(Marker.class, new Marker("a"));
        assertTrue(scope.hasAttachment(Marker.class));
    }

    @Test
    void setAttachmentOverwritesSameType() {
        CastScope scope = newScope();
        scope.setAttachment(Marker.class, new Marker("a"));
        Marker second = new Marker("b");
        scope.setAttachment(Marker.class, second);
        assertSame(second, scope.getAttachment(Marker.class));
    }

    @Test
    void getAttachmentIsExactTypeMatch() {
        CastScope scope = newScope();
        scope.setAttachment(Sub.class, new Sub());
        assertNull(scope.getAttachment(Super.class), "supertype key must not resolve a subtype value");

        CastScope other = newScope();
        Super stored = new Super();
        other.setAttachment(Super.class, stored);
        assertNull(other.getAttachment(Sub.class), "subtype key must not resolve a supertype value");
    }

    @Test
    void requireAttachmentReturnsValueWhenPresent() {
        CastScope scope = newScope();
        Marker marker = new Marker("a");
        scope.setAttachment(Marker.class, marker);
        assertSame(marker, scope.requireAttachment(Marker.class));
    }

    @Test
    void requireAttachmentThrowsWhenAbsent() {
        CastScope scope = newScope();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scope.requireAttachment(Marker.class));
        assertTrue(ex.getMessage().contains(Marker.class.getName()), ex.getMessage());
    }

    @Test
    void setAttachmentRejectsNull() {
        CastScope scope = newScope();
        assertThrows(NullPointerException.class, () -> scope.setAttachment(Marker.class, null));
        assertThrows(NullPointerException.class, () -> scope.setAttachment(null, new Marker("a")));
    }

    @Test
    void builderAttachmentVisibleOnBuiltScope() {
        Marker marker = new Marker("a");
        CastScope scope = CastScope.builder(mockEntity("Bob")).attachment(Marker.class, marker).build();
        assertSame(marker, scope.getAttachment(Marker.class));
    }

    @Test
    void castAttachmentContributesPlaceholders() {
        CastScope scope = newScope();
        scope.setAttachment(TitleAttachment.class, new TitleAttachment("AK-47"));
        assertEquals("AK-47", scope.placeholders().get("shooter_weapon_title"));
    }

    @Test
    void plainAttachmentContributesNoPlaceholders() {
        CastScope scope = newScope();
        int before = scope.placeholders().size();
        scope.setAttachment(Marker.class, new Marker("a"));
        assertEquals(before, scope.placeholders().size());
    }

    @Test
    void overwriteRetractsOldPlaceholderKeys() {
        CastScope scope = newScope();
        scope.setAttachment(TitleAttachment.class, new TitleAttachment("AK-47"));
        // A replacement carrying the same key updates it; a different-key replacement retracts the old.
        scope.setAttachment(TitleAttachment.class, new TitleAttachment("M4"));
        assertEquals("M4", scope.placeholders().get("shooter_weapon_title"));
    }

    @Test
    void itemDataBacksPlaceholderAccessors() {
        CastScope absent = newScope();
        assertNull(absent.item());
        assertNull(absent.itemTitle());
        assertNull(absent.slot());

        ItemStack item = Mockito.mock(ItemStack.class);
        CastScope scope = CastScope.builder(mockEntity("Bob"))
            .attachment(ItemData.class, new ItemData(item, "AK-47", EquipmentSlot.OFF_HAND))
            .build();
        assertSame(item, scope.item());
        assertEquals("AK-47", scope.itemTitle());
        assertEquals(EquipmentSlot.OFF_HAND, scope.slot());
    }
}
