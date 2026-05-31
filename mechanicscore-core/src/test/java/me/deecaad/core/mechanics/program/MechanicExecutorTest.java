package me.deecaad.core.mechanics.program;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.conditions.CheckCondition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.expression.ExpressionParser;
import me.deecaad.core.mechanics.scope.CastBudget;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.Value;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanicExecutorTest {

    /** A mechanic that counts how many times it runs. */
    static final class CountingMechanic extends Mechanic {
        int count = 0;

        @Override
        public void use0(CastScope scope, Target subject) {
            count++;
        }

        @Override
        public NamespacedKey getKey() {
            return new NamespacedKey("test", "count");
        }

        @Override
        public Mechanic serialize(SerializeData data) throws SerializerException {
            return this;
        }
    }

    private static CastScope scope(CastBudget budget) {
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        Mockito.when(entity.getName()).thenReturn("Bob");
        Location location = new Location(null, 0, 0, 0);
        Mockito.when(entity.getLocation()).thenReturn(location);
        Mockito.when(entity.getEyeLocation()).thenReturn(location);
        return CastScope.builder(entity).budget(budget).build();
    }

    @Test
    void recursiveBlockTerminatesViaDecrementingVariable() {
        CountingMechanic counter = new CountingMechanic();

        // recurse: count; $jumps = $jumps - 1; recurse @source ?Check{If=$jumps > 0}
        Statement count = new Statement.BuiltinInvocation(counter, new Subject.Reference(CastScope.SOURCE), List.of());
        Statement decrement = new Statement.Assignment("jumps", ExpressionParser.parse("$jumps - 1"));
        Statement recurse = new Statement.BlockInvocation("recurse", new Subject.Reference(CastScope.SOURCE),
            List.of(new CheckCondition(ExpressionParser.parse("$jumps > 0"))));
        MechanicBlock recurseBlock = new MechanicBlock("recurse", List.of(count, decrement, recurse));

        // Main: $jumps = 3; recurse @source
        Statement init = new Statement.Assignment("jumps", ExpressionParser.parse("3"));
        Statement firstCall = new Statement.BlockInvocation("recurse", new Subject.Reference(CastScope.SOURCE), List.of());
        MechanicBlock main = new MechanicBlock("Main", List.of(init, firstCall));

        Program program = new Program(Map.of("Main", main, "recurse", recurseBlock), "Main");
        program.run(scope(CastBudget.defaults()));

        // $jumps is shared (no fork): the same variable counts 3..0 down to the guard.
        assertEquals(3, counter.count, "should run once per jump until jumps hits 0");
    }

    @Test
    void blockWritesShareUpToTheCaller() {
        // compute: $result = $input * 2   (a "return value" via the shared scope)
        Statement compute = new Statement.Assignment("result", ExpressionParser.parse("$input * 2"));
        MechanicBlock computeBlock = new MechanicBlock("compute", List.of(compute));

        // Main: $input = 5; compute @source
        Statement init = new Statement.Assignment("input", ExpressionParser.parse("5"));
        Statement call = new Statement.BlockInvocation("compute", new Subject.Reference(CastScope.SOURCE), List.of());
        MechanicBlock main = new MechanicBlock("Main", List.of(init, call));

        CastScope scope = scope(CastBudget.defaults());
        new Program(Map.of("Main", main, "compute", computeBlock), "Main").run(scope);

        Value result = scope.getVariable("result");
        assertEquals(10.0, result.asNumber(), "callee's write must be visible to the caller (shared scope, no fork)");
    }

    @Test
    void infiniteRecursionAbortsWithoutThrowing() {
        CountingMechanic counter = new CountingMechanic();

        // recurse: count; recurse @source  (no base case)
        Statement count = new Statement.BuiltinInvocation(counter, new Subject.Reference(CastScope.SOURCE), List.of());
        Statement recurse = new Statement.BlockInvocation("recurse", new Subject.Reference(CastScope.SOURCE), List.of());
        MechanicBlock recurseBlock = new MechanicBlock("recurse", List.of(count, recurse));

        Statement firstCall = new Statement.BlockInvocation("recurse", new Subject.Reference(CastScope.SOURCE), List.of());
        MechanicBlock main = new MechanicBlock("Main", List.of(firstCall));

        Program program = new Program(Map.of("Main", main, "recurse", recurseBlock), "Main");
        // Must not throw (abort is caught at the top) and must be bounded by the depth cap.
        program.run(scope(new CastBudget(10, 100_000)));

        assertTrue(counter.count <= 11, "depth cap should bound execution, got " + counter.count);
    }
}
