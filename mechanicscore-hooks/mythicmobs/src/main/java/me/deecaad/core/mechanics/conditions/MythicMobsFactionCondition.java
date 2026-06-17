package me.deecaad.core.mechanics.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class MythicMobsFactionCondition extends Condition {

    private String faction;

    public MythicMobsFactionCondition() {
    }

    public MythicMobsFactionCondition(String faction) {
        this.faction = faction;
    }

    @Override
    protected boolean isAllowed0(@NotNull CastScope scope, @Nullable Target subject) {
        LivingEntity target = subject == null ? null : subject.entity();
        if (target == null)
            return false;

        if (target instanceof Player player) {
            AbstractEntity abstractPlayer = BukkitAdapter.adapt(player);
            Optional<String> maybeFaction = MythicBukkit.inst().getPlayerManager().getFactionProvider().getFaction(abstractPlayer.asPlayer());
            return Objects.equals(maybeFaction.orElse(null), faction);
        }

        ActiveMob activeMob = MythicBukkit.inst().getMobManager().getMythicMobInstance(target);
        if (activeMob == null || !activeMob.hasFaction())
            return false;

        return Objects.equals(activeMob.getFaction(), faction);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "mythicmobs_faction");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/integrations/mythicmobs#mythic-mobs-faction-condition";
    }

    @NotNull @Override
    public Condition serialize(@NotNull SerializeData data) throws SerializerException {
        String faction = data.of("Faction").get(String.class).orElse(null);
        return applyParentArgs(data, new MythicMobsFactionCondition(faction));
    }
}
