package me.deecaad.core.mechanics.defaultmechanics;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.scope.Target;
import me.deecaad.core.placeholder.PlaceholderMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageMechanic extends Mechanic {

    private PlaceholderMessage message;

    public MessageMechanic() {
    }

    public MessageMechanic(String message) {
        this.message = new PlaceholderMessage(message);
    }

    public String getMessage() {
        return message.getTemplate();
    }

    @Override
    public void use0(CastScope scope, Target subject) {
        if (subject == null || !(subject.entity() instanceof Player player))
            return;

        Component chat = message.replaceAndDeserialize(scope);
        player.sendMessage(chat);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(MechanicsCore.NAMESPACE, "message");
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/mechanics/message";
    }

    @Override
    protected @NotNull ConfigSchema.Builder schemaBuilder() {
        return super.schemaBuilder().colorKey("Message").required();
    }

    @NotNull @Override
    public Mechanic serialize(@NotNull SerializeData data) throws SerializerException {
        String message = data.of("Message").assertExists().getAdventure().get();
        return applyParentArgs(data, new MessageMechanic(message));
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
