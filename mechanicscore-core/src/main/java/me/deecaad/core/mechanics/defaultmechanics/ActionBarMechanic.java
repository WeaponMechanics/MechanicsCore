package me.deecaad.core.mechanics.defaultmechanics;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.placeholder.PlaceholderMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ActionBarMechanic extends Mechanic {

    private PlaceholderMessage message;
    private int time;

    public ActionBarMechanic() {
    }

    public ActionBarMechanic(String message, int time) {
        this.message = new PlaceholderMessage(message);
        this.time = time;
    }

    public String getMessage() {
        return message.getTemplate();
    }

    public PlaceholderMessage getPlaceholderMessage() {
        return message;
    }

    public int getTime() {
        return time;
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || !(subject.entity() instanceof Player player))
            return;

        Component component = message.replaceAndDeserialize(scope);
        player.sendActionBar(component);

        // Action bars are not timed in vanilla, so resend on a 40-tick interval.
        if (time > 40) {
            MechanicsCore.getInstance().getFoliaScheduler().entity(player).runAtFixedRate(new Consumer<>() {
                int ticker = 0;

                @Override
                public void accept(TaskImplementation task) {
                    ticker += 40;
                    if (ticker >= time) {
                        task.cancel();
                        return;
                    }
                    player.sendActionBar(component);
                }
            }, 40 - (time % 40), 40);
        }
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.getInstance(), "actionbar");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/action-bar";
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        String message = data.of("Message").assertExists().getAdventure().get();
        int time = data.of("Time").assertRange(40, null).getInt().orElse(40);
        return applyParentArgs(data, new ActionBarMechanic(message, time));
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
