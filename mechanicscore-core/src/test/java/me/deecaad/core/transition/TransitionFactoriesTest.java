package me.deecaad.core.transition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransitionFactoriesTest {

    @Test
    void constant_returnsValueAtEveryTick() {
        Transition<Double> t = Transition.constant(7.5, Interpolators.DOUBLE, 20);
        assertEquals(7.5, t.evaluateAtTick(0));
        assertEquals(7.5, t.evaluateAtTick(10));
        assertEquals(7.5, t.evaluateAtTick(20));
    }

    @Test
    void lerp_midpointIsHalfway() {
        Transition<Double> t = Transition.lerp(0.0, 1.0, Interpolators.DOUBLE, 20);
        assertEquals(0.0, t.evaluateAtTick(0));
        assertEquals(0.5, t.evaluateAtTick(10), 1e-9);
        assertEquals(1.0, t.evaluateAtTick(20));
    }

    @Test
    void lerp_easeIn_midpointBelowHalfway() {
        Transition<Double> t = Transition.lerp(0.0, 1.0, Interpolators.DOUBLE, 20, Easing.EASE_IN);
        // EASE_IN squares t before lerp: at t=0.5 the eased value is 0.25.
        assertEquals(0.25, t.evaluateAtTick(10), 1e-9);
    }

    @Test
    void pulse_peaksAtMidpoint() {
        Transition<Double> t = Transition.pulse(0.0, 10.0, 0.0, Interpolators.DOUBLE, 20, Easing.LINEAR, Easing.LINEAR);
        assertEquals(0.0, t.evaluateAtTick(0));
        assertEquals(10.0, t.evaluateAtTick(10), 1e-9);
        assertEquals(0.0, t.evaluateAtTick(20));
        // Quarter-way to peak: linear LERP from 0 to 10 over [0, 0.5] hits 5.0 at t=0.25.
        assertEquals(5.0, t.evaluateAtTick(5), 1e-9);
    }

    @Test
    void invalidDuration_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> Transition.lerp(0.0, 1.0, Interpolators.DOUBLE, 0));
    }
}
