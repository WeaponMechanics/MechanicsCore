package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.TargetKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A condition is a true/false statement that decides whether a mechanic line is
 * allowed to run on a given subject {@link Target}. Conditions may also read
 * named contexts and variables off the {@link CastScope}.
 */
public abstract class Condition implements InlineSerializer<Condition> {

    private boolean isInverted;

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/#conditions";
    }

    /**
     * Returns {@code true} if the mechanic line holding this condition is allowed
     * to run on the given subject.
     *
     * @param scope the cast scope.
     * @param subject the current subject target (may be null when targeting nothing).
     * @return true if the mechanic can be used.
     */
    public final boolean isAllowed(@NotNull CastScope scope, @Nullable Target subject) {
        return isInverted != isAllowed0(scope, subject);
    }

    protected abstract boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject);

    /**
     * The narrowest target kind this condition needs (optimizer hint). Defaults
     * to {@code LIVING_ENTITY}; location-only conditions should return {@code LOCATION}.
     */
    public TargetKind requiredTarget() {
        return TargetKind.LIVING_ENTITY;
    }

    /**
     * Contributes the parent-arg keys every condition accepts (read in {@link #applyParentArgs}).
     * Subclasses override and append via {@code super.schemaBuilder()}.
     */
    protected ConfigSchema.Builder schemaBuilder() {
        return ConfigSchema.builder().boolKey("Inverted");
    }

    @Override
    public final @NotNull ConfigSchema schema() {
        return schemaBuilder().build();
    }

    protected Condition applyParentArgs(SerializeData data, Condition condition) throws SerializerException {
        condition.isInverted = data.of("Inverted").getBool().orElse(false);
        return condition;
    }
}
