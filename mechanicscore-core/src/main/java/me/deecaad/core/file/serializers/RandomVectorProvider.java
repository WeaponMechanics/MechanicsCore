package me.deecaad.core.file.serializers;

import me.deecaad.core.utils.RandomUtil;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

/**
 * Returns a random vector with a specified length between min and max.
 */
public class RandomVectorProvider implements VectorProvider {

    private final double min;
    private final double max;

    public RandomVectorProvider(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public @NotNull Vector provide(@Nullable Quaterniond localTransform) {
        double length = RandomUtil.range(min, max);
        return RandomUtil.onUnitSphere().multiply(length);
    }
}
