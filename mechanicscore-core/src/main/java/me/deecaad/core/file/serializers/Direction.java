package me.deecaad.core.file.serializers;

import me.deecaad.core.utils.ImmutableVector;
import me.deecaad.core.utils.Transform;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

/**
 * Enum holding the basic cardinal directions for simple user input.
 */
public enum Direction implements VectorProvider {

    UP(0, 1, 0),
    DOWN(0, -1, 0),
    RIGHT(-1, 0, 0),
    LEFT(1, 0, 0),
    FORWARD(0, 0, 1),
    BACKWARD(0, 0, -1);

    private final ImmutableVector raw;

    Direction(double x, double y, double z) {
        this.raw = new ImmutableVector(x, y, z);
    }

    @Override
    public @NotNull Vector provide(@Nullable Quaterniond localTransform) {
        if (localTransform == null) {
            return raw;
        }

        return Transform.rotate(localTransform, raw);
    }
}
