package me.deecaad.core.emitter;

import me.deecaad.core.file.serializers.Direction;
import me.deecaad.core.transition.Interpolators;
import me.deecaad.core.transition.Transition;
import me.deecaad.core.utils.shape.PointShape;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisplayEntityEmitterSettingsValidationTest {

    private static DisplayEntityEmitterSettings.Builder baseBuilder(int lifetime) {
        return DisplayEntityEmitterSettings.builder()
            .displayType(EntityType.BLOCK_DISPLAY)
            .shape(new PointShape())
            .direction(Direction.UP)
            .emittedLifetimeTicks(lifetime);
    }

    @Test
    void happyPath_builds() {
        assertDoesNotThrow(() -> baseBuilder(20).build());
    }

    @Test
    void velocityTransitionDurationMustMatchLifetime() {
        Transition<Vector> v = Transition.lerp(new Vector(), new Vector(1, 0, 0), Interpolators.VECTOR, 30);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).velocity(v).build());
        assertEquals(true, ex.getMessage().contains("velocity"));
    }

    @Test
    void scaleTransitionDurationMustMatchLifetime() {
        Transition<Vector3f> s = Transition.lerp(new Vector3f(), new Vector3f(1, 1, 1), Interpolators.VECTOR3F, 40);
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).scale(s).build());
    }

    @Test
    void translationTransitionDurationMustMatchLifetime() {
        Transition<Vector3f> tr = Transition.lerp(new Vector3f(), new Vector3f(0, 1, 0), Interpolators.VECTOR3F, 10);
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).translation(tr).build());
    }

    @Test
    void rotationTransitionDurationMustMatchLifetime() {
        Transition<Quaterniond> r = Transition.constant(new Quaterniond(), Interpolators.QUATERNIOND, 10);
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).rotation(r).build());
    }

    @Test
    void zeroSpinAxisRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).spinAxis(new Vector3f(0, 0, 0)).build());
    }

    @Test
    void dragOutOfRangeRejected() {
        assertThrows(IllegalArgumentException.class, () -> baseBuilder(20).drag(-0.1).build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder(20).drag(1.1).build());
        assertDoesNotThrow(() -> baseBuilder(20).drag(0.0).build());
        assertDoesNotThrow(() -> baseBuilder(20).drag(1.0).build());
    }

    @Test
    void negativeLifetimeJitterRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).lifetimeJitterTicks(-1).build());
    }

    @Test
    void lifetimeJitterAtOrAboveLifetimeRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).lifetimeJitterTicks(20).build());
        assertDoesNotThrow(() -> baseBuilder(20).lifetimeJitterTicks(19).build());
    }

    @Test
    void negativeCyclePhaseJitterRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> baseBuilder(20).cyclePhaseJitterTicks(-1).build());
    }

    @Test
    void nonDisplayEntityTypeRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            DisplayEntityEmitterSettings.builder()
                .displayType(EntityType.ARMOR_STAND)
                .shape(new PointShape())
                .direction(Direction.UP)
                .build());
    }

    @Test
    void spinAxisIsNormalizedAtBuildTime() {
        DisplayEntityEmitterSettings settings = baseBuilder(20)
            .spinAxis(new Vector3f(3, 0, 0))
            .spinRadiansPerTick(0.1)
            .build();
        // Axis should be unit length after build-time normalization.
        assertEquals(1.0f, settings.getSpinAxis().lengthSquared(), 1e-6f);
    }
}
