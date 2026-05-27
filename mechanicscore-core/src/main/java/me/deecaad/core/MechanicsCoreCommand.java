package me.deecaad.core;

import com.cjcrafter.foliascheduler.TaskImplementation;
import dev.jorel.commandapi.CommandAPICommand;
import me.deecaad.core.commands.CommandHelpBuilder;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.entity.EntityCompatibility;
import me.deecaad.core.compatibility.entity.FakeEntity;
import me.deecaad.core.emitter.AbstractEmitter;
import me.deecaad.core.emitter.DisplayEntityEmitter;
import me.deecaad.core.emitter.DisplayEntityEmitterSettings;
import me.deecaad.core.emitter.ParticleEmitter;
import me.deecaad.core.emitter.ParticleEmitterSettings;
import me.deecaad.core.file.serializers.Direction;
import me.deecaad.core.transition.Easing;
import me.deecaad.core.transition.Interpolators;
import me.deecaad.core.transition.Keyframe;
import me.deecaad.core.transition.Transition;
import me.deecaad.core.utils.EntityTransform;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.core.utils.TableBuilder;
import me.deecaad.core.utils.shape.CircleShape;
import me.deecaad.core.utils.shape.ConeShape;
import me.deecaad.core.utils.shape.LineShape;
import me.deecaad.core.utils.shape.PointShape;
import me.deecaad.core.utils.shape.Shape;
import me.deecaad.core.utils.shape.SphereShape;
import me.deecaad.core.tick.TickManager;
import me.deecaad.core.tick.TransformTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

public final class MechanicsCoreCommand {

    /**
     * Don't let anyone instantiate this class
     */
    private MechanicsCoreCommand() {
    }

    public static void build() {
        CommandAPICommand command = new CommandAPICommand("mechanicscore")
            .withPermission("mechanicscore.admin")
            .withShortDescription("MechanicsCore debug/test commands")
            .withSubcommand(new CommandAPICommand("table")
                .withPermission("mechanicscore.commands.table")
                .withShortDescription("Helpful tables that are used on the wiki")
                .withSubcommand(new CommandAPICommand("colors")
                    .withPermission("mechanicscore.commands.table.colors")
                    .withShortDescription("Shows legacy color codes and the adventure version")
                    .executesPlayer((player, args) -> {
                        tableColors(player);
                    }))
                .withSubcommand(new CommandAPICommand("plugins")
                    .withPermission("mechanicscore.commands.plugins")
                    .withShortDescription("Shows all plugins currently using MechanicsCore")
                    .executesPlayer((player, args) -> {
                        listPlugins(player);
                    })))
            .withSubcommand(new CommandAPICommand("test")
                .withPermission("mechanicscore.commands.test")
                .withShortDescription("Spawns fake display entities to verify the transform API")
                .withSubcommand(new CommandAPICommand("display")
                    .withShortDescription("Spawns a block, item, and text display in front of you")
                    .executesPlayer((player, args) -> {
                        testDisplay(player);
                    }))
                .withSubcommand(new CommandAPICommand("orbit")
                    .withShortDescription("Spawns a parent display with child displays orbiting it")
                    .executesPlayer((player, args) -> {
                        testOrbit(player);
                    }))
                .withSubcommand(new CommandAPICommand("follow")
                    .withShortDescription("Spawns displays that follow you via an EntityTransform")
                    .executesPlayer((player, args) -> {
                        testFollow(player);
                    }))
                .withSubcommand(new CommandAPICommand("emitter")
                    .withShortDescription("Spawns emitters to verify the emitter system")
                    .withSubcommand(new CommandAPICommand("fountain")
                        .withShortDescription("Continuous flame fountain from a sphere shape")
                        .executesPlayer((player, args) -> {
                            testEmitterFountain(player);
                        }))
                    .withSubcommand(new CommandAPICommand("burst")
                        .withShortDescription("Burst-mode emitter with a particle-type transition")
                        .executesPlayer((player, args) -> {
                            testEmitterBurst(player);
                        }))
                    .withSubcommand(new CommandAPICommand("shapes")
                        .withShortDescription("One emitter per shape primitive, in a row")
                        .executesPlayer((player, args) -> {
                            testEmitterShapes(player);
                        }))
                    .withSubcommand(new CommandAPICommand("display")
                        .withShortDescription("Display entity emitter with scale + block-cycle transitions")
                        .executesPlayer((player, args) -> {
                            testEmitterDisplay(player);
                        }))
                    .withSubcommand(new CommandAPICommand("trail")
                        .withShortDescription("Particle trail emitter parented to your EntityTransform")
                        .executesPlayer((player, args) -> {
                            testEmitterTrail(player);
                        })))
                .withSubcommand(new CommandAPICommand("clear")
                    .withShortDescription("Removes all spawned test entities")
                    .executesPlayer((player, args) -> {
                        testClear(player);
                    })));

        CommandHelpBuilder helpBuilder = new CommandHelpBuilder(Style.style(NamedTextColor.GOLD), Style.style(NamedTextColor.GRAY));
        helpBuilder.register(command);

        command.register();
    }

    public static void listPlugins(CommandSender sender) {
        List<Plugin> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
            .filter(plugin -> {
                PluginDescriptionFile desc = plugin.getDescription();
                return desc.getDepend().contains("MechanicsCore") || desc.getSoftDepend().contains("MechanicsCore");
            }).toList();

        Style gold = Style.style(NamedTextColor.GOLD);
        Style gray = Style.style(NamedTextColor.GRAY);
        TextComponent table = new TableBuilder()
            .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS)
            .withElementChar('-')
            .withElementCharStyle(gold)
            .withFillChar('=')
            .withFillCharStyle(Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
            .withHeader("Plugins using MechanicsCore")
            .withHeaderStyle(gold)
            .withElementStyle(gray)
            .withAttemptSinglePixelFix()
            .withSupplier(i -> {
                if (plugins.size() <= i)
                    return empty();

                Plugin plugin = plugins.get(i);
                PluginDescriptionFile desc = plugin.getDescription();

                // When the player hovers over the plugin, we should see some
                // general plugin info (version, authors)
                // TODO All MechanicsCore plugins should inherit from a base class
                // TODO to provide more info, like commands and updates. "One stop shopping"
                TextComponent.Builder hover = text();
                hover.append(text("Version: ", gray)).append(text(desc.getVersion(), gold)).append(newline());
                hover.append(text("Authors: ", gray)).append(text(desc.getAuthors().toString(), gold))/* .append(newline()) */;

                return text().content(plugin.getName().toUpperCase(Locale.ROOT))
                    .hoverEvent(hover.build())
                    .build();
            })
            .build();

        sender.sendMessage(table);
    }

    public static void tableColors(CommandSender sender) {

        final List<ColorData> colors = new ArrayList<>();
        colors.add(new ColorData("&0", "<black>", NamedTextColor.BLACK));
        colors.add(new ColorData("&1", "<dark_blue>", NamedTextColor.DARK_BLUE));
        colors.add(new ColorData("&2", "<dark_green>", NamedTextColor.DARK_GREEN));
        colors.add(new ColorData("&3", "<dark_aqua>", NamedTextColor.DARK_AQUA));
        colors.add(new ColorData("&4", "<dark_red>", NamedTextColor.DARK_RED));
        colors.add(new ColorData("&5", "<dark_purple>", NamedTextColor.DARK_PURPLE));
        colors.add(new ColorData("&6", "<gold>", NamedTextColor.GOLD));
        colors.add(new ColorData("&7", "<gray>", NamedTextColor.GRAY));
        colors.add(new ColorData("&8", "<dark_gray>", NamedTextColor.DARK_GRAY));
        colors.add(new ColorData("&9", "<blue>", NamedTextColor.BLUE));
        colors.add(new ColorData("&a", "<green>", NamedTextColor.GREEN));
        colors.add(new ColorData("&b", "<aqua>", NamedTextColor.AQUA));
        colors.add(new ColorData("&c", "<red>", NamedTextColor.RED));
        colors.add(new ColorData("&d", "<light_purple>", NamedTextColor.LIGHT_PURPLE));
        colors.add(new ColorData("&e", "<yellow>", NamedTextColor.YELLOW));
        colors.add(new ColorData("&f", "<white>", NamedTextColor.WHITE));

        final List<ColorData> decorations = new ArrayList<>();
        decorations.add(new ColorData("&k", "<obfuscated>", TextDecoration.OBFUSCATED));
        decorations.add(new ColorData("&l", "<bold>", TextDecoration.BOLD));
        decorations.add(new ColorData("&m", "<strikethrough>", TextDecoration.STRIKETHROUGH));
        decorations.add(new ColorData("&n", "<underline>", TextDecoration.UNDERLINED));
        decorations.add(new ColorData("&o", "<italic>", TextDecoration.ITALIC));
        decorations.add(new ColorData("&r", "<reset>", NamedTextColor.WHITE));

        Component colorComponent = new TableBuilder()
            .withFillChar('=')
            .withFillCharStyle(Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
            .withHeader("COLORS")
            .withHeaderStyle(Style.style(NamedTextColor.GOLD))
            .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS.setRows(8))
            .withSupplier(i -> {
                return colors.get(i).build();
            })
            .build();

        Component decorationComponent = new TableBuilder()
            .withFillChar('=')
            .withFillCharStyle(Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
            .withHeader("DECORATIONS")
            .withHeaderStyle(Style.style(NamedTextColor.GOLD))
            .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS.setRows(3))
            .withSupplier(i -> {
                return decorations.get(i).build();
            })
            .build();

        Component miscComponent = new TableBuilder()
            .withFillChar('=')
            .withFillCharStyle(Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
            .withHeader("MISCELLANEOUS")
            .withHeaderStyle(Style.style(NamedTextColor.GOLD))
            .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS.setRows(3).setColumns(1))
            .withSupplier(i -> switch (i) {
                case 0 ->
                    new ColorData("&#7D5A2D", "<#7D5A2D>", TextColor.color(125, 90, 45)).alt("The five boxing wizards jump quickly").build();
                case 1 ->
                    text("<rainbow> = ").append(MiniMessage.miniMessage().deserialize("<rainbow>The quick brown fox jumps over the lazy dog"));
                case 2 ->
                    text("<gradient:green:#ff0000> = ").append(MiniMessage.miniMessage().deserialize("<gradient:green:#ff0000>A wizard's job is to vex chumps"));
                default -> throw new RuntimeException("unreachable code");
            })
            .build();

        sender.sendMessage(colorComponent.append(decorationComponent).append(miscComponent).append(new TableBuilder.Line('=', Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
            .build()));
    }

    private static final List<FakeEntity> testEntities = new ArrayList<>();
    private static final List<TransformTree> testTrees = new ArrayList<>();
    private static final List<TaskImplementation<Void>> testTasks = new ArrayList<>();
    private static final List<AbstractEmitter<?>> testEmitters = new ArrayList<>();

    private static void testDisplay(Player player) {
        EntityCompatibility compat = CompatibilityAPI.getEntityCompatibility();
        try {
            Location base = inFront(player, 3);
            Vector right = base.getDirection().crossProduct(new Vector(0, 1, 0)).normalize();

            var block = compat.generateFakeBlockDisplay(base.clone().subtract(right), Material.LODESTONE.createBlockData());
            block.setTransformation(scale(1.0f));
            block.setBillboard(Display.Billboard.FIXED);
            block.updateMeta();
            block.show();
            testEntities.add(block);

            var item = compat.generateFakeItemDisplay(base.clone(), new ItemStack(Material.DIAMOND_SWORD));
            item.setTransformation(scale(1.0f));
            item.setBillboard(Display.Billboard.VERTICAL);
            item.updateMeta();
            item.show();
            testEntities.add(item);

            var text = compat.generateFakeTextDisplay(base.clone().add(right), text("MechanicsCore", NamedTextColor.GOLD));
            text.setBillboard(Display.Billboard.CENTER);
            text.setSeeThrough(true);
            text.updateMeta();
            text.show();
            testEntities.add(text);

            player.sendMessage(text("Spawned 3 display entities. Use /mechanicscore test clear to remove.", NamedTextColor.GREEN));
        } catch (UnsupportedOperationException e) {
            player.sendMessage(text("Fake display entities are not supported on this server version.", NamedTextColor.RED));
        }
    }

    private static void testOrbit(Player player) {
        EntityCompatibility compat = CompatibilityAPI.getEntityCompatibility();
        try {
            Location center = inFront(player, 4);

            var parent = compat.generateFakeBlockDisplay(center, Material.SEA_LANTERN.createBlockData());
            parent.setTransformation(scale(0.5f));
            parent.setBillboard(Display.Billboard.FIXED);
            parent.updateMeta();

            int count = 6;
            double radius = 2.0;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                Location childLoc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                var child = compat.generateFakeItemDisplay(childLoc, new ItemStack(Material.AMETHYST_SHARD));
                child.setTransformation(scale(0.75f));
                child.setBillboard(Display.Billboard.FIXED);
                child.setTeleportDuration(3);
                child.updateMeta();
                parent.getTransform().addChild(child);
            }

            parent.show(); // cascades to children
            testEntities.add(parent); // remove() cascades to children

            Consumer<TaskImplementation<Void>> spin = task ->
                parent.getTransform().applyRotation(new Quaterniond().rotateY(Math.toRadians(4)));
            testTasks.add(MechanicsCore.getInstance().getFoliaScheduler().region(center).runAtFixedRate(spin, 1, 1));

            player.sendMessage(text("Spawned an orbiting hierarchy (1 parent + " + count + " children).", NamedTextColor.GREEN));
        } catch (UnsupportedOperationException e) {
            player.sendMessage(text("Fake display entities are not supported on this server version.", NamedTextColor.RED));
        }
    }

    private static void testFollow(Player player) {
        EntityCompatibility compat = CompatibilityAPI.getEntityCompatibility();
        try {
            EntityTransform playerTransform = new EntityTransform(player);
            Location loc = player.getLocation();

            var marker = compat.generateFakeBlockDisplay(loc.clone().add(0, 2.5, 0), Material.GLOWSTONE.createBlockData());
            marker.setTransformation(scale(0.4f));
            marker.setBillboard(Display.Billboard.FIXED);
            marker.setTeleportDuration(3);
            marker.updateMeta();

            var label = compat.generateFakeTextDisplay(loc.clone().add(0, 3.2, 0), text(player.getName(), NamedTextColor.AQUA));
            label.setBillboard(Display.Billboard.CENTER);
            label.setSeeThrough(true);
            label.setTeleportDuration(3);
            label.updateMeta();

            playerTransform.addChild(marker);
            playerTransform.addChild(label);
            marker.show();
            label.show();
            testEntities.add(marker);
            testEntities.add(label);

            TransformTree tree = new TransformTree(playerTransform, loc);
            MechanicsCore.getInstance().getTickManager().add(tree);
            testTrees.add(tree);

            player.sendMessage(text("Spawned displays following you. They orbit as you turn.", NamedTextColor.GREEN));
        } catch (UnsupportedOperationException e) {
            player.sendMessage(text("Fake display entities are not supported on this server version.", NamedTextColor.RED));
        }
    }

    private static void testEmitterFountain(Player player) {
        Location origin = inFront(player, 2.5).subtract(0, 1, 0);

        ParticleEmitterSettings settings = ParticleEmitterSettings.builder()
            .particle(Particle.FLAME)
            .countPerPoint(1)
            .particleSpeed(0.3)
            .shape(new SphereShape(0.4, true))
            .direction(Direction.UP)
            .speed(0.4)
            .rate(5.0)
            .durationTicks(200)
            .build();

        ParticleEmitter emitter = new ParticleEmitter(settings);
        emitter.spawnAt(origin);
        MechanicsCore.getInstance().getTickManager().add(emitter);
        testEmitters.add(emitter);

        player.sendMessage(text("Spawned a 10s flame fountain. /mechanicscore test clear to stop early.", NamedTextColor.GREEN));
    }

    private static void testEmitterBurst(Player player) {
        Location origin = inFront(player, 3);
        int duration = 100;

        Transition<Particle> particleCycle = new Transition<>(
            List.of(
                new Keyframe<>(0.0, Particle.FLAME, Easing.LINEAR),
                new Keyframe<>(0.5, Particle.SOUL_FIRE_FLAME, Easing.LINEAR),
                new Keyframe<>(1.0, Particle.END_ROD, Easing.LINEAR)),
            Interpolators.stepped(),
            duration);

        ParticleEmitterSettings settings = ParticleEmitterSettings.builder()
            .particle(Particle.FLAME)
            .countPerPoint(1)
            .particleSpeed(0.6)
            .shape(new PointShape())
            .direction(Direction.UP)
            .speed(0.0)
            .burst(30, 20)
            .durationTicks(duration)
            .particleTransition(particleCycle)
            .build();

        ParticleEmitter emitter = new ParticleEmitter(settings);
        emitter.spawnAt(origin);
        MechanicsCore.getInstance().getTickManager().add(emitter);
        testEmitters.add(emitter);

        player.sendMessage(text("Spawned a 5s burst emitter (flame → soul fire → end rod).", NamedTextColor.GREEN));
    }

    private static void testEmitterShapes(Player player) {
        Location base = inFront(player, 4);
        Vector right = base.getDirection().crossProduct(new Vector(0, 1, 0)).normalize();

        spawnShapeEmitter(base.clone().subtract(right.clone().multiply(2.5)), new SphereShape(0.6, false), Particle.HEART);
        spawnShapeEmitter(base.clone().subtract(right.clone().multiply(0.8)), new CircleShape(0.8, CircleShape.Axis.Y), Particle.HAPPY_VILLAGER);
        spawnShapeEmitter(base.clone().add(right.clone().multiply(0.8)), new ConeShape(Math.toRadians(20), 1.5), Particle.CRIT);
        spawnShapeEmitter(base.clone().add(right.clone().multiply(2.5)), new LineShape(1.5), Particle.END_ROD);

        player.sendMessage(text("Spawned 4 emitters: sphere, circle, cone, line.", NamedTextColor.GREEN));
    }

    private static void spawnShapeEmitter(Location origin, Shape shape, Particle particle) {
        ParticleEmitterSettings settings = ParticleEmitterSettings.builder()
            .particle(particle)
            .countPerPoint(1)
            .particleSpeed(0.0)
            .shape(shape)
            .direction(Direction.UP)
            .speed(0.0)
            .rate(2.0)
            .durationTicks(200)
            .build();

        ParticleEmitter emitter = new ParticleEmitter(settings);
        emitter.spawnAt(origin);
        MechanicsCore.getInstance().getTickManager().add(emitter);
        testEmitters.add(emitter);
    }

    private static void testEmitterDisplay(Player player) {
        try {
            Location origin = inFront(player, 4);
            int itemLifetime = 40;

            Transition<Vector3f> scalePulse = Transition.pulse(
                new Vector3f(0, 0, 0),
                new Vector3f(0.5f, 0.5f, 0.5f),
                new Vector3f(0, 0, 0),
                Interpolators.VECTOR3F,
                itemLifetime,
                Easing.EASE_OUT,
                Easing.EASE_IN);

            Transition<org.bukkit.block.data.BlockData> blockCycle = new Transition<>(
                List.of(
                    new Keyframe<>(0.0, Material.RED_STAINED_GLASS.createBlockData(), Easing.LINEAR),
                    new Keyframe<>(0.25, Material.ORANGE_STAINED_GLASS.createBlockData(), Easing.LINEAR),
                    new Keyframe<>(0.5, Material.YELLOW_STAINED_GLASS.createBlockData(), Easing.LINEAR),
                    new Keyframe<>(0.75, Material.LIME_STAINED_GLASS.createBlockData(), Easing.LINEAR),
                    new Keyframe<>(1.0, Material.BLUE_STAINED_GLASS.createBlockData(), Easing.LINEAR)),
                Interpolators.stepped(),
                itemLifetime);

            DisplayEntityEmitterSettings settings = DisplayEntityEmitterSettings.builder()
                .displayType(EntityType.BLOCK_DISPLAY)
                .displayData(Material.RED_STAINED_GLASS.createBlockData())
                .shape(new SphereShape(1.2, true))
                .direction(Direction.UP)
                .speed(0.35)
                .rate(0.5)
                .durationTicks(200)
                .emittedLifetimeTicks(itemLifetime)
                .liveCap(40)
                .scale(scalePulse)
                .blockCycle(blockCycle)
                .acceleration(new Vector3f(0, -0.04f, 0))
                .drag(0.02)
                .lifetimeJitterTicks(8)
                .scaleJitter(new Vector3f(0.1f, 0.1f, 0.1f))
                .cyclePhaseJitterTicks(10)
                .spinAxis(new Vector3f(0.3f, 1, 0.2f))
                .spinRadiansPerTick(0.18)
                .build();

            DisplayEntityEmitter emitter = new DisplayEntityEmitter(settings);
            emitter.spawnAt(origin);
            MechanicsCore.getInstance().getTickManager().add(emitter);
            testEmitters.add(emitter);

            player.sendMessage(text("Spawned a display emitter (gravity + tumble + jittered cycles).", NamedTextColor.GREEN));
        } catch (UnsupportedOperationException e) {
            player.sendMessage(text("Fake display entities are not supported on this server version.", NamedTextColor.RED));
        }
    }

    private static void testEmitterTrail(Player player) {
        ParticleEmitterSettings settings = ParticleEmitterSettings.builder()
            .particle(Particle.FLAME)
            .countPerPoint(1)
            .particleSpeed(0.0)
            .shape(new SphereShape(0.25, false))
            .direction(Direction.UP)
            .speed(0.0)
            .rate(3.0)
            .durationTicks(-1)
            .build();

        ParticleEmitter trail = new ParticleEmitter(settings);
        trail.spawnAt(player.getLocation().add(0, 1, 0));
        MechanicsCore.getInstance().getTickManager().add(trail);
        testEmitters.add(trail);

        Consumer<TaskImplementation<Void>> follow = t ->
            trail.getTransform().setPosition(player.getLocation().toVector().add(new Vector(0, 1, 0)));
        testTasks.add(MechanicsCore.getInstance().getFoliaScheduler().region(player.getLocation()).runAtFixedRate(follow, 1, 1));

        player.sendMessage(text("Spawned a flame trail following your position. /mechanicscore test clear to stop.", NamedTextColor.GREEN));
    }

    private static void testClear(Player player) {
        for (TransformTree tree : testTrees)
            tree.stop();
        for (TaskImplementation<Void> task : testTasks)
            task.cancel();
        for (AbstractEmitter<?> emitter : testEmitters)
            emitter.stop();
        for (FakeEntity entity : testEntities)
            entity.remove();

        int count = testEntities.size() + testEmitters.size();
        testTrees.clear();
        testTasks.clear();
        testEmitters.clear();
        testEntities.clear();
        player.sendMessage(text("Cleared " + count + " test entities/emitters.", NamedTextColor.GREEN));
    }

    private static Location inFront(Player player, double distance) {
        Location eye = player.getEyeLocation();
        return eye.add(eye.getDirection().multiply(distance));
    }

    private static Transformation scale(float scale) {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf());
    }

    private static class ColorData {

        private String legacy;
        private String adventure;
        private TextColor color;
        private TextDecoration decoration;

        private String altText;

        public ColorData(String legacy, String adventure, TextColor color) {
            this.legacy = legacy;
            this.adventure = adventure;
            this.color = color;
        }

        public ColorData(String legacy, String adventure, TextDecoration decoration) {
            this.legacy = legacy;
            this.adventure = adventure;
            this.decoration = decoration;
        }

        private ColorData alt(String altText) {
            this.altText = altText;
            return this;
        }

        private TextComponent build() {
            TextComponent.Builder builder = text();
            builder.append(text(adventure + " = "));

            String readable = altText != null ? altText : StringUtil.snakeToReadable(adventure.substring(1, adventure.length() - 1));
            if (color != null)
                builder.append(text(readable).color(color));
            else
                builder.append(text(readable).decorate(decoration));

            return builder.build();
        }
    }
}
