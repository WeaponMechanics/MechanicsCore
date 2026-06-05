package me.deecaad.core.mechanics.scope;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.placeholder.PlaceholderData;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private @Nullable ItemStack item;
    private @Nullable String itemTitle;
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

    // Misc carried state

    public @Nullable ItemStack getItem() {
        return item;
    }

    public @Nullable String getItemTitle() {
        return itemTitle;
    }

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
        return item;
    }

    @Override
    public @Nullable String itemTitle() {
        return itemTitle;
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

        public @NotNull Builder item(@Nullable ItemStack item) {
            scope.item = item;
            return this;
        }

        public @NotNull Builder itemTitle(@Nullable String itemTitle) {
            scope.itemTitle = itemTitle;
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
