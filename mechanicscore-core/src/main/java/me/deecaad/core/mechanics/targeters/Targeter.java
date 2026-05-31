package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.AnyVectorProvider;
import me.deecaad.core.file.serializers.VectorProvider;
import me.deecaad.core.file.serializers.VectorSerializer;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.EntityTarget;
import me.deecaad.core.mechanics.scope.PointTarget;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.scope.TargetKind;
import me.deecaad.core.utils.EntityTransform;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A targeter resolves a {@link CastScope} into a {@link Context} (a set of
 * targets). Targeters are used to bind named contexts ({@code @enemies = ...})
 * and as the inline subject of a mechanic line ({@code Damage{} @Nearby{...}}).
 */
public abstract class Targeter implements InlineSerializer<Targeter> {

    private boolean eye;
    private @Nullable VectorProvider offset;
    private @NotNull String from = CastScope.SOURCE;

    public Targeter() {
    }

    @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/#targeter";
    }

    public boolean isEye() {
        return eye;
    }

    public @Nullable VectorProvider getOffset() {
        return offset;
    }

    /**
     * The name of the context this targeter takes its origin from (defaults to
     * {@code source}). Used by origin-based targeters like World and Nearby.
     */
    public @NotNull String getFrom() {
        return from;
    }

    /**
     * Returns {@code true} if this targeter specifically targets entities.
     */
    public abstract boolean isEntity();

    /**
     * Resolves the targets for this targeter against the given scope.
     */
    public abstract @NotNull Context target(@NotNull CastScope scope);

    /**
     * Returns a (possibly cheaper) equivalent targeter given that every consumer
     * only needs the given target kind. Default returns {@code this}. Origin-based
     * targeters override this to narrow their query (e.g. players only).
     */
    public @NotNull Targeter specialize(@NotNull TargetKind demand) {
        return this;
    }

    /**
     * A key identifying targeters that resolve the identical query, used by the
     * optimizer to group statements that share a subject. {@code null} (default)
     * means this targeter is not groupable (e.g. random/relative targeters).
     */
    public @Nullable Object groupKey() {
        return null;
    }

    /**
     * Builds an entity target, applying this targeter's eye and offset settings.
     */
    protected @NotNull Target entityTarget(@NotNull LivingEntity entity) {
        Vector resolved = null;
        if (offset != null) {
            EntityTransform transform = new EntityTransform(entity);
            Quaterniond rotation = transform.getLocalRotation();
            resolved = offset.provide(rotation);
        }
        return new EntityTarget(entity, eye, resolved);
    }

    /**
     * Builds a location target, applying this targeter's offset. Eye is ignored
     * for pure-location targets.
     */
    protected @NotNull Target pointTarget(@NotNull Location location) {
        if (offset != null)
            location.add(offset.provide((Quaterniond) null));
        return new PointTarget(location);
    }

    /**
     * Re-wraps an existing context so this targeter's eye/offset settings apply.
     * Returns the context unchanged when there are no modifiers.
     */
    protected @NotNull Context wrap(@NotNull Context context) {
        if (!eye && offset == null)
            return context;
        java.util.List<Target> out = new java.util.ArrayList<>(context.size());
        for (Target target : context) {
            if (target.entity() != null)
                out.add(entityTarget(target.entity()));
            else
                out.add(pointTarget(target.location()));
        }
        return Context.of(out);
    }

    protected Targeter applyParentArgs(SerializeData data, Targeter targeter) throws SerializerException {
        VectorProvider offset = data.of("Offset").serialize(VectorSerializer.class).orElse(null);
        if (!isEntity() && offset instanceof AnyVectorProvider any && any.isRelative()) {
            throw data.exception("offset", "Did you try to use relative locations ('~') with '" + getInlineKeyword() + "'?",
                getInlineKeyword() + " is a LOCATION targeter, so it cannot use relative locations.");
        }

        targeter.offset = offset;
        targeter.eye = data.of("Eye").getBool().orElse(false);
        targeter.from = data.of("From").get(String.class).orElse(CastScope.SOURCE);
        return targeter;
    }
}
