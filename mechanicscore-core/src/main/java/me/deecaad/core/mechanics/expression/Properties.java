package me.deecaad.core.mechanics.expression;

import me.deecaad.core.mechanics.scope.CastAbortException;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.Value;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link Property} resolvers keyed by the dotted path after the
 * context name. Property names are validated at parse time and resolved at eval
 * time. Extendable so downstream plugins can register more entity properties.
 */
public final class Properties {

    private static final Map<String, Property> REGISTRY = new HashMap<>();

    static {
        register("size", context -> Value.of(context.size()));

        register("x", context -> Value.of(firstLocation(context).getX()));
        register("y", context -> Value.of(firstLocation(context).getY()));
        register("z", context -> Value.of(firstLocation(context).getZ()));
        register("yaw", context -> Value.of(firstLocation(context).getYaw()));
        register("pitch", context -> Value.of(firstLocation(context).getPitch()));

        register("health", context -> Value.of(firstEntity(context).getHealth()));
        register("name", context -> Value.of(firstEntity(context).getName()));

        register("velocity.x", context -> Value.of(firstEntity(context).getVelocity().getX()));
        register("velocity.y", context -> Value.of(firstEntity(context).getVelocity().getY()));
        register("velocity.z", context -> Value.of(firstEntity(context).getVelocity().getZ()));
        register("velocity.length", context -> {
            Vector velocity = firstEntity(context).getVelocity();
            return Value.of(velocity.length());
        });
    }

    private Properties() {
    }

    public static void register(@NotNull String path, @NotNull Property property) {
        REGISTRY.put(path, property);
    }

    public static @Nullable Property get(@NotNull String path) {
        return REGISTRY.get(path);
    }

    public static boolean exists(@NotNull String path) {
        return REGISTRY.containsKey(path);
    }

    public static @NotNull java.util.Set<String> names() {
        return REGISTRY.keySet();
    }

    private static @NotNull Location firstLocation(@NotNull Context context) {
        Target first = context.first();
        if (first == null)
            throw new CastAbortException("Tried to read a location property of an empty context");
        return first.location();
    }

    private static @NotNull LivingEntity firstEntity(@NotNull Context context) {
        Target first = context.first();
        if (first == null || first.entity() == null)
            throw new CastAbortException("Tried to read an entity property of a context with no entity");
        return first.entity();
    }
}
