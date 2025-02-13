package me.deecaad.core.placeholder;

import me.deecaad.core.utils.MutableRegistry;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Utility class holding all built-in placeholder handlers, and the registry
 * for all placeholder handlers. This class is not meant to be instantiated.
 */
public final class PlaceholderHandlers {

    /**
     * The registry for all globally registered placeholder handlers.
     */
    public static final @NotNull MutableRegistry<PlaceholderHandler> REGISTRY
            = new MutableRegistry.SimpleMutableRegistry<>(new HashMap<>());

    public static final @NotNull PlaceholderHandler ARMOR_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.ARMOR));
    public static final @NotNull PlaceholderHandler ARMOR_TOUGHNESS_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.ARMOR_TOUGHNESS));
    public static final @NotNull PlaceholderHandler ATTACK_DAMAGE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.ATTACK_DAMAGE));
    public static final @NotNull PlaceholderHandler ATTACK_KNOCKBACK_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.ATTACK_KNOCKBACK));
    public static final @NotNull PlaceholderHandler ATTACK_SPEED_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.ATTACK_SPEED));
    public static final @NotNull PlaceholderHandler BLOCK_BREAK_SPEED_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.BLOCK_BREAK_SPEED));
    public static final @NotNull PlaceholderHandler BLOCK_INTERACTION_RANGE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.BLOCK_INTERACTION_RANGE));
    public static final @NotNull PlaceholderHandler BURNING_TIME_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.BURNING_TIME));
    public static final @NotNull PlaceholderHandler ENTITY_INTERACTION_RANGE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.ENTITY_INTERACTION_RANGE));
    public static final @NotNull PlaceholderHandler EXPLOSION_KNOCKBACK_RESISTANCE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE));
    public static final @NotNull PlaceholderHandler FALL_DAMAGE_MULTIPLIER_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.FALL_DAMAGE_MULTIPLIER));
    public static final @NotNull PlaceholderHandler FOLLOW_RANGE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.FOLLOW_RANGE));
    public static final @NotNull PlaceholderHandler FLYING_SPEED_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.FLYING_SPEED));
    public static final @NotNull PlaceholderHandler GRAVITY_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.GRAVITY));
    public static final @NotNull PlaceholderHandler JUMP_STRENGTH_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.JUMP_STRENGTH));
    public static final @NotNull PlaceholderHandler KNOCKBACK_RESISTANCE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.KNOCKBACK_RESISTANCE));
    public static final @NotNull PlaceholderHandler LUCK_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.LUCK));
    public static final @NotNull PlaceholderHandler MAX_ABSORPTION_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.MAX_ABSORPTION));
    public static final @NotNull PlaceholderHandler MAX_HEALTH_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.MAX_HEALTH));
    public static final @NotNull PlaceholderHandler MINING_EFFICIENCY_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.MINING_EFFICIENCY));
    public static final @NotNull PlaceholderHandler MOVEMENT_EFFICIENCY_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.MOVEMENT_EFFICIENCY));
    public static final @NotNull PlaceholderHandler MOVEMENT_SPEED_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.MOVEMENT_SPEED));
    public static final @NotNull PlaceholderHandler OXYGEN_BONUS_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.OXYGEN_BONUS));
    public static final @NotNull PlaceholderHandler SAFE_FALL_DISTANCE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.SAFE_FALL_DISTANCE));
    public static final @NotNull PlaceholderHandler SCALE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.SCALE));
    public static final @NotNull PlaceholderHandler SPAWN_REINFORCEMENTS_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.SPAWN_REINFORCEMENTS));
    public static final @NotNull PlaceholderHandler STEP_HEIGHT_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.STEP_HEIGHT));
    public static final @NotNull PlaceholderHandler SUBMERGED_MINING_SPEED_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.SUBMERGED_MINING_SPEED));
    public static final @NotNull PlaceholderHandler SNEAKING_SPEED_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.SNEAKING_SPEED));
    public static final @NotNull PlaceholderHandler SWEEPING_DAMAGE_RATIO_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.SWEEPING_DAMAGE_RATIO));
    public static final @NotNull PlaceholderHandler TEMPT_RANGE_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.TEMPT_RANGE));
    public static final @NotNull PlaceholderHandler WATER_MOVEMENT_EFFICIENCY_ATTRIBUTE = register(new AttributePlaceholderHandler(Attribute.WATER_MOVEMENT_EFFICIENCY));

    private PlaceholderHandlers() {
    }

    private static @NotNull PlaceholderHandler register(@NotNull PlaceholderHandler handler) {
        REGISTRY.add(handler);
        return handler;
    }
}
