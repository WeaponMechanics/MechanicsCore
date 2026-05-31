package me.deecaad.core.mechanics.scope;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An immutable, ordered set of {@link Target targets} bound to a name. Replaces
 * the single mutable target slot of the old CastData. {@code source} and
 * {@code target} are just two built-in contexts; users can create more.
 */
public final class Context implements Iterable<Target> {

    private static final Context EMPTY = new Context(List.of());

    private final List<Target> targets;

    private Context(@NotNull List<Target> targets) {
        this.targets = targets;
    }

    public static Context empty() {
        return EMPTY;
    }

    public static Context of(@NotNull Target... targets) {
        return targets.length == 0 ? EMPTY : new Context(List.of(targets));
    }

    public static Context of(@NotNull Collection<Target> targets) {
        return targets.isEmpty() ? EMPTY : new Context(List.copyOf(targets));
    }

    public static Context ofEntities(@NotNull Collection<? extends LivingEntity> entities) {
        if (entities.isEmpty())
            return EMPTY;
        List<Target> targets = new ArrayList<>(entities.size());
        for (LivingEntity entity : entities)
            targets.add(new EntityTarget(entity));
        return new Context(List.copyOf(targets));
    }

    public @NotNull List<Target> targets() {
        return targets;
    }

    public int size() {
        return targets.size();
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    /**
     * The first target, for consumers that only need a single point/entity.
     */
    public @Nullable Target first() {
        return targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * The entities in this context, skipping pure-location targets.
     */
    public @NotNull Stream<LivingEntity> entities() {
        return targets.stream().map(Target::entity).filter(Objects::nonNull);
    }

    @Override
    public @NotNull Iterator<Target> iterator() {
        return targets.iterator();
    }
}
