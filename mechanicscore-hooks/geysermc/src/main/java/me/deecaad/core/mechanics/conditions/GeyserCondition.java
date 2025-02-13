package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import org.bukkit.NamespacedKey;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GeyserCondition extends Condition {

    /**
     * Default constructor for serializer.
     */
    public GeyserCondition() {
    }

    @Override
    public boolean isAllowed0(CastData cast) {
        if (cast.getTarget() == null)
            return false;

        try {
            return GeyserApi.api().isBedrockPlayer(cast.getTarget().getUniqueId());
        } catch (Throwable ex) {
            MechanicsCore.getInstance().getDebugger().severe("Tried to use GeyserCondition but Geyser is not installed!", ex);
            return false;
        }
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("mechanicscore", "geyser");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/integrations/geysermc#geyser-condition";
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        return applyParentArgs(data, new GeyserCondition());
    }
}
