package me.deecaad.core.file.verify;

import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SimpleSerializer;
import me.deecaad.core.utils.MutableRegistry;
import org.bukkit.Keyed;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A serializer's declaration of every key it accepts. Built with {@link #builder()}. The keys are
 * declared statically (independent of control flow), which is what makes unknown-key detection
 * exact and JSON Schema export complete.
 *
 * <p>Modifier methods ({@code required}, {@code range}, {@code condition}, {@code pathTo}) apply to
 * the most recently added key, allowing fluent one-liners:
 * <pre>
 * ConfigSchema.builder()
 *     .nested("Offset", Vec2Serializer.class)
 *     .intKey("Length").required().range(0, null)
 *     .doubleKey("R").range(0.0, 1.0)
 *     .build();
 * </pre>
 */
public record ConfigSchema(@NotNull List<KeySpec> keys, boolean allowUnknown) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<MutableSpec> specs = new ArrayList<>();
        private final List<KeySpec> includedSpecs = new ArrayList<>();
        private boolean allowUnknown = false;

        private MutableSpec last() {
            if (specs.isEmpty())
                throw new IllegalStateException("No key added yet; call a *Key/nested method first");
            return specs.get(specs.size() - 1);
        }

        private Builder add(String name, KeyType type) {
            specs.add(new MutableSpec(name, type));
            return this;
        }

        public Builder intKey(String name) {
            return add(name, KeyType.INT);
        }

        public Builder doubleKey(String name) {
            return add(name, KeyType.DOUBLE);
        }

        public Builder boolKey(String name) {
            return add(name, KeyType.BOOL);
        }

        public Builder stringKey(String name) {
            return add(name, KeyType.STRING);
        }

        public Builder colorKey(String name) {
            return add(name, KeyType.COLOR);
        }

        public Builder materialKey(String name) {
            return add(name, KeyType.MATERIAL);
        }

        public Builder entityKey(String name) {
            return add(name, KeyType.ENTITY);
        }

        public Builder enumKey(String name, Class<? extends Enum<?>> enumType) {
            add(name, KeyType.ENUM);
            last().enumType = enumType;
            return this;
        }

        public Builder registryKey(String name, Class<? extends Keyed> registryClass) {
            add(name, KeyType.REGISTRY);
            last().registryClass = registryClass;
            return this;
        }

        /**
         * Declares a key whose value is a single inline serializer chosen from a serializer registry
         * (e.g. a targeter from {@code Targeters.REGISTRY}). The validator picks the serializer by
         * name and recurses into its {@link Serializer#schema()} to catch hallucinated nested keys.
         */
        public Builder registrySerializerKey(String name, MutableRegistry<? extends Keyed> registry) {
            add(name, KeyType.REGISTRY_SERIALIZER);
            last().registry = registry;
            return this;
        }

        /**
         * Declares a key whose value is a list of inline serializers chosen from a serializer
         * registry (e.g. conditions from {@code Conditions.REGISTRY}). Each element is resolved by
         * name and recursed into.
         */
        public Builder registrySerializerListKey(String name, MutableRegistry<? extends Keyed> registry) {
            add(name, KeyType.REGISTRY_SERIALIZER_LIST);
            last().registry = registry;
            return this;
        }

        public Builder nested(String name, Class<? extends Serializer<?>> serializer) {
            add(name, KeyType.NESTED);
            last().nested = serializer;
            return this;
        }

        /**
         * Declares a list key whose elements are parsed by the given ordered {@code args}. This
         * mirrors {@code data.ofList(name).addArgument(arg0).addArgument(arg1)...assertList()}.
         *
         * @param requiredArgs The number of leading arguments that must be present in each element.
         */
        public Builder listKey(String name, int requiredArgs, SimpleSerializer<?>... args) {
            add(name, KeyType.LIST);
            last().listArgs = List.of(args);
            last().requiredArgs = requiredArgs;
            return this;
        }

        /**
         * Declares a list key whose elements are NOT parsed or split (e.g. lore lines, recipe shape
         * rows). Only the "is a list" type is validated; elements are accepted as-is.
         */
        public Builder rawListKey(String name) {
            add(name, KeyType.LIST);
            last().listArgs = List.of();
            return this;
        }

        public Builder required() {
            last().required = true;
            return this;
        }

        public Builder range(Integer min, Integer max) {
            last().range = Range.of(min, max);
            return this;
        }

        public Builder range(Double min, Double max) {
            last().range = Range.of(min, max);
            return this;
        }

        public Builder condition(Condition condition) {
            last().condition = condition;
            return this;
        }

        public Builder activeWhen(String siblingKey, Object expected) {
            return condition(Condition.activeWhen(siblingKey, expected));
        }

        public Builder pathTo() {
            last().pathTo = true;
            return this;
        }

        public Builder allowUnknown() {
            this.allowUnknown = true;
            return this;
        }

        /**
         * Flattens another schema's keys into this one. Used when a serializer embeds another
         * serializer's keys at the SAME config level (e.g. DropItem reads a full item inline).
         */
        public Builder include(ConfigSchema other) {
            includedSpecs.addAll(other.keys());
            return this;
        }

        public ConfigSchema build() {
            List<KeySpec> keys = new ArrayList<>(specs.size() + includedSpecs.size());
            for (MutableSpec s : specs)
                keys.add(s.toSpec());
            keys.addAll(includedSpecs);
            return new ConfigSchema(List.copyOf(keys), allowUnknown);
        }

        private static final class MutableSpec {
            private final String name;
            private final KeyType type;
            private boolean required;
            private Condition condition;
            private Range range;
            private Class<? extends Enum<?>> enumType;
            private Class<? extends Serializer<?>> nested;
            private Class<? extends Keyed> registryClass;
            private MutableRegistry<? extends Keyed> registry;
            private List<SimpleSerializer<?>> listArgs;
            private int requiredArgs;
            private boolean pathTo;

            private MutableSpec(String name, KeyType type) {
                this.name = name;
                this.type = type;
            }

            private KeySpec toSpec() {
                return new KeySpec(name, type, required, condition, range, enumType, nested, registryClass, registry, listArgs, requiredArgs, pathTo);
            }
        }
    }
}
