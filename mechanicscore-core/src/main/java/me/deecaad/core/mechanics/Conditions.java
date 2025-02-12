package me.deecaad.core.mechanics;

import me.deecaad.core.mechanics.conditions.BiomeCondition;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.conditions.EntityTypeCondition;
import me.deecaad.core.mechanics.conditions.GlidingCondition;
import me.deecaad.core.mechanics.conditions.HasPermissionCondition;
import me.deecaad.core.mechanics.conditions.InConeCondition;
import me.deecaad.core.mechanics.conditions.LightLevelCondition;
import me.deecaad.core.mechanics.conditions.MaterialCategoryCondition;
import me.deecaad.core.mechanics.conditions.OnGroundCondition;
import me.deecaad.core.mechanics.conditions.RangeCondition;
import me.deecaad.core.mechanics.conditions.RidingCondition;
import me.deecaad.core.mechanics.conditions.SneakingCondition;
import me.deecaad.core.mechanics.conditions.SprintingCondition;
import me.deecaad.core.utils.MutableRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Utility class holding all built-in conditions, and the registry for all
 * conditions. This class is not meant to be instantiated.
 *
 * <p>The registry {@link Conditions#REGISTRY} is a mutable registry, meaning
 * that you can external sources may add/overwrite conditions.
 */
public final class Conditions {

    /**
     * The registry for all globally registered conditions.
     */
    public static final @NotNull MutableRegistry<Condition> REGISTRY
        = new MutableRegistry.SimpleMutableRegistry<>(new HashMap<>());

    public static final @NotNull Condition BIOME = register(new BiomeCondition());
    public static final @NotNull Condition ENTITY_TYPE = register(new EntityTypeCondition());
    public static final @NotNull Condition GLIDING = register(new GlidingCondition());
    public static final @NotNull Condition HAS_PERMISSION = register(new HasPermissionCondition());
    public static final @NotNull Condition IN_CONE = register(new InConeCondition());
    public static final @NotNull Condition LIGHT_LEVEL = register(new LightLevelCondition());
    public static final @NotNull Condition MATERIAL_CATEGORY = register(new MaterialCategoryCondition());
    public static final @NotNull Condition ON_GROUND = register(new OnGroundCondition());
    public static final @NotNull Condition RANGE = register(new RangeCondition());
    public static final @NotNull Condition RIDING = register(new RidingCondition());
    public static final @NotNull Condition SNEAKING = register(new SneakingCondition());
    public static final @NotNull Condition SPRINTING = register(new SprintingCondition());


    private Conditions() {
    }

    private static @NotNull Condition register(@NotNull Condition condition) {
        REGISTRY.add(condition);
        return condition;
    }
}
