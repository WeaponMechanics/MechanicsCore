package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BiomeCondition extends Condition {

    private Biome biome;

    public BiomeCondition() {
    }

    public BiomeCondition(Biome biome) {
        this.biome = biome;
    }

    @Override
    public boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        return subject != null && subject.location().getBlock().getBiome() == biome;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "biome");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/conditions/biome";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().registryKey("Biome", Biome.class).required();
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        Biome biome = data.of("Biome").assertExists().getBukkitRegistry(Biome.class).get();
        return applyParentArgs(data, new BiomeCondition(biome));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }
}
