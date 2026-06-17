package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightningMechanic extends Mechanic {

    private boolean isEffect;

    public LightningMechanic() {
    }

    public LightningMechanic(boolean isEffect) {
        this.isEffect = isEffect;
    }

    public boolean isEffect() {
        return isEffect;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "lightning");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/lightning";
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null)
            return;
        Location strikeLocation = subject.location();
        World world = subject.world();
        if (world == null)
            return;

        if (isEffect)
            world.strikeLightningEffect(strikeLocation);
        else
            world.strikeLightning(strikeLocation);
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().boolKey("Effect");
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        boolean isEffect = data.of("Effect").getBool().orElse(false);
        return applyParentArgs(data, new LightningMechanic(isEffect));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
