package me.deecaad.core.mechanics.defaultmechanics;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.entity.FakeEntity;
import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.file.SimpleSerializer;
import me.deecaad.core.file.serializers.ColorSerializer;
import me.deecaad.core.mechanics.Conditions;
import me.deecaad.core.mechanics.Targeters;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Context;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.mechanics.targeters.WorldTargeter;
import me.deecaad.core.utils.DistanceUtil;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FireworkMechanic extends Mechanic {

    public static class FireworkData implements InlineSerializer<FireworkData> {

        private FireworkEffect effect;

        public FireworkData() {
        }

        public FireworkData(FireworkEffect effect) {
            this.effect = effect;
        }

        public FireworkEffect getEffect() {
            return effect;
        }

        @Override
        public @NotNull NamespacedKey getKey() {
            return new NamespacedKey(MechanicsCore.NAMESPACE, "firework_effect");
        }

        @NotNull @Override
        public FireworkData serialize(@NotNull SerializeData data) throws SerializerException {
            SimpleSerializer<Color> colorSerializer = new ColorSerializer();

            FireworkEffect.Type type = data.of("Shape").getEnum(FireworkEffect.Type.class).orElse(FireworkEffect.Type.BALL);
            boolean trail = data.of("Trail").getBool().orElse(false);
            boolean flicker = data.of("Flicker").getBool().orElse(false);

            List<Color> colors = new ArrayList<>();
            List<Color> fadeColors = new ArrayList<>();

            Object colorData = data.of("Color").assertExists().get(Object.class).get();
            if (colorData instanceof List<?> temp) {
                for (MapConfigLike.Holder holder : (List<MapConfigLike.Holder>) temp)
                    colors.add(colorSerializer.deserialize(holder.value().toString(), data.of("Color").errorLocation()));
            } else {
                Color color = colorSerializer.deserialize(colorData.toString(), data.of("Color").errorLocation());
                colors = List.of(color);
            }

            Object fadeData = data.of("Fade_Color").assertExists().get(Object.class).get();
            if (fadeData instanceof List<?> temp) {
                for (MapConfigLike.Holder holder : (List<MapConfigLike.Holder>) temp)
                    fadeColors.add(colorSerializer.deserialize(holder.value().toString(), data.of("Fade_Color").errorLocation()));
            } else {
                Color color = colorSerializer.deserialize(fadeData.toString(), data.of("Fade_Color").errorLocation());
                fadeColors = List.of(color);
            }

            FireworkEffect effect = FireworkEffect.builder().with(type).withColor(colors).trail(trail).flicker(flicker).withFade(fadeColors).build();
            return new FireworkData(effect);
        }
    }

    private ItemStack fireworkItem;
    private int flightTime;
    private Targeter viewers;
    private List<Condition> viewerConditions;

    public FireworkMechanic() {
    }

    public FireworkMechanic(ItemStack fireworkItem, int flightTime, Targeter viewers, List<Condition> viewerConditions) {
        this.fireworkItem = fireworkItem;
        this.flightTime = flightTime;
        this.viewers = viewers;
        this.viewerConditions = viewerConditions;
    }

    public ItemStack getFireworkItem() {
        return fireworkItem;
    }

    public int getFlightTime() {
        return flightTime;
    }

    public Targeter getViewers() {
        return viewers;
    }

    public List<Condition> getViewerConditions() {
        return viewerConditions;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null)
            return;
        Location targetLoc = subject.location();

        List<Player> players;
        if (viewers == null) {
            players = DistanceUtil.getPlayersInRange(targetLoc);
        } else {
            players = new LinkedList<>();
            Context viewerContext = viewers.target(scope);
            OUTER : for (Target viewer : viewerContext) {
                if (!(viewer.entity() instanceof Player player))
                    continue;
                for (Condition condition : viewerConditions)
                    if (!condition.isAllowed(scope, viewer))
                        continue OUTER;
                players.add(player);
            }
        }

        if (players.isEmpty())
            return;

        spawn(targetLoc, players);
    }

    private void spawn(Location targetLoc, List<Player> viewers) {
        FakeEntity fakeEntity = CompatibilityAPI.getCompatibility().getEntityCompatibility().generateFakeEntity(targetLoc, EntityType.FIREWORK_ROCKET, fireworkItem);
        if (flightTime > 1)
            fakeEntity.setMotion(0.001, 0.3, -0.001);

        for (Player player : viewers)
            fakeEntity.show(player);

        if (flightTime <= 0) {
            fakeEntity.playEffect(EntityEffect.FIREWORK_EXPLODE);
            fakeEntity.remove();
            return;
        }

        MechanicsCore.getInstance().getFoliaScheduler().region(targetLoc).runDelayed(() -> {
            fakeEntity.playEffect(EntityEffect.FIREWORK_EXPLODE);
            fakeEntity.remove();
        }, flightTime);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "firework");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/firework";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder()
            .rawListKey("Effects")
            .intKey("Flight_Time")
            .registrySerializerKey("Viewers", Targeters.REGISTRY)
            .registrySerializerListKey("Viewer_Conditions", Conditions.REGISTRY);
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        ItemStack fireworkItem = new ItemStack(MinecraftVersions.UPDATE_AQUATIC.isAtLeast() ? Material.FIREWORK_ROCKET : Material.valueOf("FIREWORK"));
        FireworkMeta meta = (FireworkMeta) fireworkItem.getItemMeta();
        List<FireworkEffect> effects = data.of("Effects").getImpliedList(new FireworkData()).stream().map(FireworkData::getEffect).toList();
        int flightTime = data.of("Flight_Time").getInt().orElse(1);
        meta.addEffects(effects);
        meta.setPower(flightTime);
        fireworkItem.setItemMeta(meta);

        Targeter viewers = data.of("Viewers").serializeRegistry(Targeters.REGISTRY).orElse(null);
        List<Condition> viewerConditions = data.of("Viewer_Conditions").getRegistryList(Conditions.REGISTRY);
        if (!viewerConditions.isEmpty() && viewers == null)
            viewers = new WorldTargeter();

        return applyParentArgs(data, new FireworkMechanic(fireworkItem, flightTime, viewers, viewerConditions));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.LOCATION;
    }

    @Override
    public boolean isBatchablePlayerEffect() {
        return true;
    }
}
