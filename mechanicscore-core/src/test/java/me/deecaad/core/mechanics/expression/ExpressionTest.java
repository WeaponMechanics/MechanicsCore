package me.deecaad.core.mechanics.expression;

import me.deecaad.core.mechanics.scope.CastAbortException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.Value;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionTest {

    private static LivingEntity mockEntity() {
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        Mockito.when(entity.getName()).thenReturn("Bob");
        Location location = new Location(null, 0, 0, 0);
        Mockito.when(entity.getLocation()).thenReturn(location);
        Mockito.when(entity.getEyeLocation()).thenReturn(location);
        return entity;
    }

    private static CastScope scope() {
        return CastScope.builder(mockEntity()).build();
    }

    private static double eval(String expression, CastScope scope) {
        return ExpressionParser.parse(expression).eval(scope).asNumber();
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
        "2 + 3 * 4 ; 14",
        "(2 + 3) * 4 ; 20",
        "-2 + 3 ; 1",
        "10 % 3 ; 1",
        "10 / 4 ; 2.5",
        "3 > 2 ; 1",
        "2 > 3 ; 0",
        "3 >= 3 ; 1",
        "3 == 3 ; 1",
        "3 != 3 ; 0",
        "1 < 2 && 2 < 3 ; 1",
        "1 < 2 && 2 > 3 ; 0",
        "1 > 2 || 2 < 3 ; 1",
        "!0 ; 1",
        "!5 ; 0",
        "max(1, 5, 3) ; 5",
        "min(1, 5, 3) ; 1",
        "clamp(15, 0, 10) ; 10",
        "clamp(-5, 0, 10) ; 0",
        "abs(-5) ; 5",
        "sqrt(16) ; 4",
        "round(2.6) ; 3",
        "floor(2.9) ; 2",
        "ceil(2.1) ; 3",
    })
    void evaluatesArithmeticAndFunctions(String expression, double expected) {
        assertEquals(expected, eval(expression, scope()));
    }

    @Test
    void randomStaysWithinRange() {
        assertEquals(5.0, eval("random(5, 5)", scope()));
        double value = eval("random(2, 4)", scope());
        org.junit.jupiter.api.Assertions.assertTrue(value >= 2 && value < 4);
    }

    @Test
    void variablesResolveAndDefaultToZero() {
        CastScope scope = scope();
        scope.setVariable("dmg", Value.of(10));
        assertEquals(20.0, eval("$dmg * 2", scope));
        assertEquals(0.0, eval("$missing + 0", scope));
    }

    @Test
    void stringLiteralEvaluates() {
        assertEquals("hello", ExpressionParser.parse("'hello'").eval(scope()).asString());
    }

    @Test
    void contextSizeProperty() {
        CastScope scope = scope();
        scope.setContext("enemies", Context.of(
            Target.of(new Location(null, 0, 0, 0)),
            Target.of(new Location(null, 1, 1, 1)),
            Target.of(new Location(null, 2, 2, 2))));
        assertEquals(3.0, eval("enemies.size", scope));
    }

    @Test
    void locationProperty() {
        CastScope scope = scope();
        scope.setContext("spot", Context.of(Target.of(new Location(null, 7, 8, 9))));
        assertEquals(7.0, eval("spot.x", scope));
        assertEquals(8.0, eval("spot.y", scope));
        assertEquals(9.0, eval("spot.z", scope));
    }

    @Test
    void entityVelocityLengthProperty() {
        LivingEntity entity = mockEntity();
        Mockito.when(entity.getVelocity()).thenReturn(new Vector(3, 4, 0));
        CastScope scope = scope();
        scope.setContext("mob", Context.ofEntities(List.of(entity)));
        assertEquals(5.0, eval("mob.velocity.length", scope));
    }

    @Test
    void unknownContextAbortsAtEval() {
        assertThrows(CastAbortException.class, () -> eval("ghost.size", scope()));
    }

    @ParameterizedTest
    @CsvSource({
        "max()",
        "clamp(1, 2)",
        "foo(1)",
        "source.bogus",
        "3 +",
        "(3",
        "3 = 4",
        "$",
    })
    void rejectsMalformedExpressions(String expression) {
        assertThrows(ExpressionException.class, () -> ExpressionParser.parse(expression));
    }
}
