package me.deecaad.core.mechanics.scope;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.placeholder.PlaceholderData;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The runtime state of a cast. Instead of a fixed source/target pair, a scope holds a map of named
 * {@link Context contexts} and a map of {@code $variables}. {@code source} and {@code target} are
 * the two built-in contexts.
 *
 * <p>
 * A single scope is shared across the whole cast, including block calls. Blocks
 * are macros over this shared state: variables and bindings a block sets persist
 * after it returns (so a block "returns" by setting a variable the caller reads).
 * The {@code target} context is the exception, saved/restored around each
 * invocation's per-target loop. Recursion is bounded by {@link CastBudget}, not by
 * copying state.
 */
public final class CastScope implements PlaceholderData {

    public static final String SOURCE = "source";
    public static final String TARGET = "target";

    private final Map<String, Context> contexts;
    private final Map<String, Value> variables;
    private final Map<String, String> placeholders;

    // Lazily allocated; most casts attach nothing.
    private @Nullable Map<Class<?>, Object> attachments;
    private @Nullable Consumer<TaskImplementation<Void>> taskConsumer;
    private CastBudget budget;

    private CastScope() {
        this.contexts = new LinkedHashMap<>();
        this.variables = new LinkedHashMap<>();
        this.placeholders = new HashMap<>();
    }

    // Contexts

    public @Nullable Context getContext(@NotNull String name) {
        return contexts.get(name);
    }

    public boolean hasContext(@NotNull String name) {
        return contexts.containsKey(name);
    }

    public void setContext(@NotNull String name, @NotNull Context context) {
        contexts.put(name, context);
        writeContextPlaceholders(name, context);
    }

    public @NotNull Context source() {
        Context source = contexts.get(SOURCE);
        return source == null ? Context.empty() : source;
    }

    public @NotNull Context target() {
        Context target = contexts.get(TARGET);
        return target == null ? Context.empty() : target;
    }

    /**
     * The single source entity, for mechanics that need the caster directly.
     */
    public @Nullable LivingEntity sourceEntity() {
        Target first = source().first();
        return first == null ? null : first.entity();
    }

    // Variables

    public @Nullable Value getVariable(@NotNull String name) {
        return variables.get(name);
    }

    public void setVariable(@NotNull String name, @NotNull Value value) {
        variables.put(name, value);
        placeholders.put(name, value.asString());
    }

    public @NotNull Map<String, Value> variables() {
        return variables;
    }

    // Attachments
    //
    // Plugin-specific typed state keyed by exact Class. A scope is single-threaded for the duration
    // of a cast, so no synchronization. Storing under a subclass is not retrievable via a supertype
    // key; store under the supertype deliberately if polymorphic lookup is wanted.

    public <T> @Nullable T getAttachment(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        return attachments == null ? null : type.cast(attachments.get(type));
    }

    public boolean hasAttachment(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "type");
        return attachments != null && attachments.containsKey(type);
    }

    public <T> void setAttachment(@NotNull Class<T> type, @NotNull T value) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        if (attachments == null)
            attachments = new HashMap<>();

        Object old = attachments.put(type, value);
        if (old instanceof CastAttachment oldAttachment)
            placeholders.keySet().removeAll(oldAttachment.placeholders().keySet());
        if (value instanceof CastAttachment attachment)
            placeholders.putAll(attachment.placeholders());
    }

    public <T> @NotNull T requireAttachment(@NotNull Class<T> type) {
        T value = getAttachment(type);
        if (value == null)
            throw new IllegalStateException("No attachment of type " + type.getName() + " on this cast");
        return value;
    }

    // Misc carried state

    public @Nullable Consumer<TaskImplementation<Void>> getTaskConsumer() {
        return taskConsumer;
    }

    public @NotNull CastBudget getBudget() {
        return budget;
    }

    private void writeContextPlaceholders(@NotNull String name, @NotNull Context context) {
        placeholders.put(name + "_size", String.valueOf(context.size()));
        Target first = context.first();
        if (first == null)
            return;

        Location location = first.location();
        placeholders.put(name + "_x", String.valueOf(location.getX()));
        placeholders.put(name + "_y", String.valueOf(location.getY()));
        placeholders.put(name + "_z", String.valueOf(location.getZ()));
        if (first.entity() != null)
            placeholders.put(name + "_name", getName(first.entity()));
    }

    private static String getName(@NotNull LivingEntity entity) {
        TextComponent component = LegacyComponentSerializer.legacySection().deserialize(entity.getName());
        return MiniMessage.miniMessage().serialize(component);
    }

    // PlaceholderData

    @Override
    public @Nullable Player player() {
        LivingEntity source = sourceEntity();
        return source instanceof Player player ? player : null;
    }

    @Override
    public @Nullable ItemStack item() {
        ItemData data = getAttachment(ItemData.class);
        return data == null ? null : data.item();
    }

    @Override
    public @Nullable String itemTitle() {
        ItemData data = getAttachment(ItemData.class);
        return data == null ? null : data.title();
    }

    @Override
    public @Nullable EquipmentSlot slot() {
        ItemData data = getAttachment(ItemData.class);
        return data == null ? null : data.slot();
    }

    @Override
    public @NotNull Map<String, String> placeholders() {
        return placeholders;
    }

    public static @NotNull Builder builder(@NotNull LivingEntity source) {
        return new Builder(source);
    }

    /**
     * Builds the initial scope at a trigger entry point. {@code target} defaults
     * to {@code source} so {@code @target} always resolves to a self-cast unless
     * the trigger binds a different target.
     */
    public static final class Builder {

        private final CastScope scope = new CastScope();
        private final Context sourceContext;

        private Builder(@NotNull LivingEntity source) {
            this.sourceContext = Context.of(new EntityTarget(source));
        }

        public <T> @NotNull Builder attachment(@NotNull Class<T> type, @NotNull T value) {
            scope.setAttachment(type, value);
            return this;
        }

        public @NotNull Builder taskConsumer(@Nullable Consumer<TaskImplementation<Void>> taskConsumer) {
            scope.taskConsumer = taskConsumer;
            return this;
        }

        public @NotNull Builder budget(@NotNull CastBudget budget) {
            scope.budget = budget;
            return this;
        }

        public @NotNull Builder context(@NotNull String name, @NotNull Context context) {
            scope.setContext(name, context);
            return this;
        }

        public @NotNull Builder variable(@NotNull String name, @NotNull Value value) {
            scope.setVariable(name, value);
            return this;
        }

        public @NotNull CastScope build() {
            if (scope.budget == null)
                scope.budget = CastBudget.defaults();
            scope.setContext(SOURCE, sourceContext);
            if (!scope.hasContext(TARGET))
                scope.setContext(TARGET, sourceContext);
            return scope;
        }
    }
}
