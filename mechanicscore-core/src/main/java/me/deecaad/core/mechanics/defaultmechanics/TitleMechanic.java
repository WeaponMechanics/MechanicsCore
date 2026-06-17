package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.placeholder.PlaceholderMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleMechanic extends Mechanic {

    private @Nullable PlaceholderMessage title;
    private @Nullable PlaceholderMessage subtitle;
    private Title.Times times;

    public TitleMechanic() {
    }

    public TitleMechanic(@Nullable String title, @Nullable String subtitle, @NotNull Title.Times times) {
        this.title = title == null ? null : new PlaceholderMessage(title);
        this.subtitle = subtitle == null ? null : new PlaceholderMessage(subtitle);
        this.times = times;
    }

    public @Nullable PlaceholderMessage getTitle() {
        return title;
    }

    public @Nullable PlaceholderMessage getSubtitle() {
        return subtitle;
    }

    public @NotNull Title.Times getTimes() {
        return times;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || !(subject.entity() instanceof Player player))
            return;

        Component titleComponent = title == null ? Component.empty() : title.replaceAndDeserialize(scope);
        Component subtitleComponent = subtitle == null ? Component.empty() : subtitle.replaceAndDeserialize(scope);
        Title title = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(title);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "title");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/title";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().colorKey("Title").colorKey("Subtitle").intKey("Fade_In").range(0, null).intKey("Stay").range(0, null).intKey("Fade_Out").range(0, null);
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        String title = data.of("Title").getAdventure().orElse(null);
        String subtitle = data.of("Subtitle").getAdventure().orElse(null);
        int fadeIn = data.of("Fade_In").assertRange(0, null).getInt().orElse(10);
        int stay = data.of("Stay").assertRange(0, null).getInt().orElse(70);
        int fadeOut = data.of("Fade_Out").assertRange(0, null).getInt().orElse(20);

        if (title == null && subtitle == null)
            throw data.exception(null, "Missing both 'title' and 'subtitle' options");

        Title.Times times = Title.Times.times(Ticks.duration(fadeIn), Ticks.duration(stay), Ticks.duration(fadeOut));
        return applyParentArgs(data, new TitleMechanic(title, subtitle, times));
    }

    @Override
    public me.deecaad.core.mechanics.scope.TargetKind requiredTarget() {
        return me.deecaad.core.mechanics.scope.TargetKind.PLAYER;
    }

    @Override
    public boolean isBatchablePlayerEffect() {
        return true;
    }
}
