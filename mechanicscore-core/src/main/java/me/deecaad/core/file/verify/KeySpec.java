package me.deecaad.core.file.verify;

import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SimpleSerializer;
import me.deecaad.core.utils.MutableRegistry;
import org.bukkit.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Declares a single accepted config key: its name, type, and any constraints. Built via
 * {@link ConfigSchema.Builder}; immutable once constructed.
 *
 * <p>LIST keys carry the ordered {@link SimpleSerializer} arguments that parse each list element
 * (mirroring {@code data.ofList(...).addArgument(...)}), plus {@code requiredArgs} - the number of
 * leading arguments that must be present. REGISTRY keys carry the {@link Keyed} class of the bukkit
 * registry to look up. REGISTRY_SERIALIZER(_LIST) keys carry the {@link MutableRegistry} of inline
 * serializers (e.g. targeters/conditions) to pick from and recurse into.
 */
public record KeySpec(
    @NotNull String name,
    @NotNull KeyType type,
    boolean required,
    @Nullable Condition condition,
    @Nullable Range range,
    @Nullable Class<? extends Enum<?>> enumType,
    @Nullable Class<? extends Serializer<?>> nested,
    @Nullable Class<? extends Keyed> registryClass,
    @Nullable MutableRegistry<? extends Keyed> registry,
    @Nullable List<SimpleSerializer<?>> listArgs,
    int requiredArgs,
    boolean pathTo) {

    /**
     * @return The valid enum constant names, or an empty list when this is not an enum key.
     */
    public List<String> enumValues() {
        if (enumType == null)
            return List.of();
        Enum<?>[] constants = enumType.getEnumConstants();
        if (constants == null)
            return List.of();
        return java.util.Arrays.stream(constants).map(Enum::name).toList();
    }
}
