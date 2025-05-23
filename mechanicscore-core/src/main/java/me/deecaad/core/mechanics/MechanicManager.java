package me.deecaad.core.mechanics;

import me.deecaad.core.file.*;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.mechanics.targeters.*;
import me.deecaad.core.utils.RegistryUtil;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.deecaad.core.file.InlineSerializer.NAME_FINDER;

public class MechanicManager implements Serializer<MechanicManager> {

    private List<Mechanic> mechanics;

    // WMP modifiers
    private List<Mechanic> dirty;

    /**
     * Default constructor for serializer.
     */
    public MechanicManager() {
    }

    public MechanicManager(List<Mechanic> mechanics) {
        this.mechanics = mechanics;
        this.dirty = new LinkedList<>();
    }

    public List<Mechanic> getMechanics() {
        return mechanics;
    }

    /**
     * WeaponMechanicsPlus modifies this to temporarily append mechanics.
     *
     * @param mechanics The list of mechanics to temporarily add.
     */
    public void addDirty(List<Mechanic> mechanics) {
        dirty.addAll(mechanics);
    }

    /**
     * @return true if there are temporary mechanics.
     */
    public boolean isDirty() {
        return !dirty.isEmpty();
    }

    /**
     * When using {@link #addDirty(List)}, after casting the mechanics ({@link #use(CastData)}), you
     * should clear the dirty mechanics.
     *
     * <p>
     * This method is automatically called after using {@link #use(CastData)}.
     */
    public void clearDirty() {
        dirty.clear();
    }

    @Override
    public String getKeyword() {
        return "Mechanics";
    }

    @Nullable @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/mechanics/";
    }

    public void use(CastData cast) {
        for (Mechanic mechanic : mechanics)
            mechanic.use(cast);
        for (Mechanic mechanic : dirty)
            mechanic.use(cast);

        clearDirty();
    }

    @NotNull @Override
    public MechanicManager serialize(@NotNull SerializeData data) throws SerializerException {
        List<?> list = data.getConfig().getList(data.getKey());
        List<Mechanic> mechanics = new ArrayList<>();

        // The old mechanic format used nested memory sections, so the list will be null.
        if (list == null) {
            throw data.exception(null, "Could not find any list... Are you still using the outdated Mechanics format?",
                "Need help? https://youtu.be/q8Oh2qsiCH0");
        }

        // Store cacheable mechanics into this list to improve performance.
        PlayerEffectMechanicList cacheList = new PlayerEffectMechanicList();

        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (obj == null) {
                throw data.listException(null, i, "Found a null/empty mechanic", "This happens when you have an empty line in your list");
            }

            Mechanic mechanic = serializeOne(data, obj.toString());

            if (mechanic instanceof PlayerEffectMechanic playerMechanic && playerMechanic.getViewerTargeter() instanceof WorldTargeter worldTargeter) {
                if (worldTargeter.isDefaultValues()) {
                    cacheList.addMechanic(playerMechanic);
                } else {
                    // Well, at least it is a player mechanic, no need to loop through the whole world!
                    playerMechanic.targeter = new WorldPlayersTargeter(worldTargeter.getWorldName());
                }
            } else {
                mechanics.add(mechanic);
            }
        }

        // Only store the cache list if it contains mechanics
        if (!cacheList.isEmpty()) {
            mechanics.add(cacheList);
        }

        return new MechanicManager(mechanics);
    }

    public Mechanic serializeOne(SerializeData data, String line) throws SerializerException {

        // So here is the problem:
        // instead of just 'foo(arg1=hello)', we have 'foo(...) @... ?...',
        // and we need to use regex to find the targeter and the conditions.

        // This pattern groups everything and stops whenever it hits a '?', '@',
        // or the end of the string. This perfectly separates each section.
        // All the backslash stuff is for handling escaped \? and \@
        Pattern pattern = Pattern.compile(".+?(?=(?<!\\\\)(?:\\\\\\\\)*[?@]|$)");
        Matcher matcher = pattern.matcher(line);

        Mechanic mechanic = null;
        Targeter targeter = null;
        List<Condition> conditions = new LinkedList<>();

        while (matcher.find()) {
            int index = matcher.start();
            String group = matcher.group();

            try {
                switch (group.charAt(0)) {
                    case '@' -> {
                        // Already have a targeter, user shouldn't have multiple
                        if (targeter != null)
                            throw data.exception(null, "Found multiple targeters with '@'", "Instead of using multiple targeters on the same line, try putting your mechanics on separate lines");

                        // Try to figure out the name of the target. For example:
                        // '@EntitiesInRadius(radius=10)' => '@EntitiesInRadius'
                        Matcher nameMatcher = NAME_FINDER.matcher(group);
                        if (!nameMatcher.find()) {
                            throw data.exception(null, "Could not determine the name of the targeter",
                                "Make sure you included the {} after your targeter",
                                "Found value: " + group);
                        }

                        String temp = nameMatcher.group().substring(1);
                        targeter = RegistryUtil.matchAny(Targeters.REGISTRY, temp);
                        if (targeter == null) {
                            throw SerializerException.builder()
                                .locationRaw(data.of().getLocation())
                                .buildInvalidRegistryOption(temp, Targeters.REGISTRY);
                        }

                        // We need to call the 'inlineFormat' method since the
                        // current targeter object is just an empty serializer.
                        Map<String, MapConfigLike.Holder> args = InlineSerializer.inlineFormat(group.substring(1));
                        SerializeData nested = new SerializeData(data.getFile(), null, new MapConfigLike(args).setDebugInfo(data.getFile(), data.getKey(), line));
                        targeter = targeter.serialize(nested);
                    }

                    case '?' -> {
                        // Try to figure out the name of the condition. For example:
                        // '?entity(PLAYER)' => '?entity'
                        Matcher nameMatcher = NAME_FINDER.matcher(group);
                        if (!nameMatcher.find())
                            throw new InlineSerializer.FormatException(0, "Could not determine the name of the condition");

                        String temp = nameMatcher.group().substring(1);
                        Condition condition = RegistryUtil.matchAny(Conditions.REGISTRY, temp);
                        if (condition == null) {
                            throw SerializerException.builder()
                                .locationRaw(data.of().getLocation())
                                .buildInvalidRegistryOption(temp, Conditions.REGISTRY);
                        }

                        // We need to call the 'inlineFormat' method since the
                        // current condition object is just an empty serializer.
                        Map<String, MapConfigLike.Holder> args = InlineSerializer.inlineFormat(group.substring(1));
                        SerializeData nested = new SerializeData(data.getFile(), null, new MapConfigLike(args).setDebugInfo(data.getFile(), data.getKey(), line));
                        conditions.add(condition.serialize(nested));
                    }

                    default -> {
                        // Already have a mechanic, so the user probably mis-labeled something
                        if (mechanic != null)
                            throw data.exception(null, "Found multiple mechanics on the same line",
                                "Mechanic 1: " + mechanic.getInlineKeyword(), "Mechanic 2: " + group,
                                "If you are trying to use @targeters and ?conditions, make sure to use @ and ?");

                        // Try to figure out the name of the target. For example:
                        // '@EntitiesInRadius(radius=10)' => '@EntitiesInRadius'
                        Matcher nameMatcher = NAME_FINDER.matcher(group);
                        if (!nameMatcher.find()) {
                            throw data.exception(null, "Could not determine the name of the mechanic",
                                "Make sure you included the {} after your mechanic!",
                                "For value: " + group);
                        }

                        String temp = nameMatcher.group();
                        mechanic = RegistryUtil.matchAny(Mechanics.REGISTRY, temp);
                        if (mechanic == null) {
                            throw SerializerException.builder()
                                .locationRaw(data.of().getLocation())
                                .buildInvalidRegistryOption(temp, Mechanics.REGISTRY);
                        }

                        // We need to call the 'inlineFormat' method since the
                        // current mechanic object is just an empty serializer.
                        Map<String, MapConfigLike.Holder> args = InlineSerializer.inlineFormat(group.substring(1));
                        SerializeData nested = new SerializeData(data.getFile(), null, new MapConfigLike(args).setDebugInfo(data.getFile(), data.getKey(), line));
                        mechanic = mechanic.serialize(nested);
                    }
                }

                if (mechanic == null)
                    throw new InlineSerializer.FormatException(0, "Could not determine mechanic");

            } catch (InlineSerializer.FormatException ex) {
                String indent = "    ";
                throw data.exception(null, ex.getMessage(), indent + line,
                    StringUtil.repeat(" ", index + ex.getIndex() + indent.length() - 1) + "^");
            }
        }

        // Always default to the source targeter
        if (targeter == null)
            targeter = new SourceTargeter();

        mechanic.targeter = targeter;
        mechanic.conditions = conditions;
        return mechanic;
    }
}