package me.deecaad.core.file;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.MechanicsPlugin;
import me.deecaad.core.diagnostic.DiagnosticRenderer;
import me.deecaad.core.utils.FileUtil;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Parses a pre-defined folder from config into a {@link Configuration}.
 *
 * <p>For example, WeaponMechanics has the "ammo" folder which is serialized by
 * the Ammo serializer. So we may call <code>new RootFileReader(plugin, Ammo.class, "ammos")</code>.
 * This serializer will recursively read through all YAML files in the "ammos" directory.
 *
 * <p>For "extension functionality," this class supports adding serializers and
 * validators that are used in the file reading stage.
 *
 * @param <R> The serialized type
 * @param <T> The serializer
 */
public class RootFileReader<R, T extends Serializer<R>> implements Listener {

    private final @NotNull File pluginDataFolder;
    private final @NotNull MechanicsLogger debugger;
    private final @NotNull ClassLoader classLoader;
    private final @NotNull Class<T> serializerClass;
    private final @NotNull String relativeFolder;
    private final @NotNull File rootFolder;

    private final @NotNull List<Serializer<?>> serializers;
    private final @NotNull List<IValidator> validators;

    /**
     * Uses the given {@link MechanicsPlugin} to set the data, as a shorthand.
     *
     * @param plugin The plugin to use
     * @param serializerClass The serializer class to use for deserialization
     * @param relativeFolder The relative folder to read from, relative to the plugin's data folder
     */
    public RootFileReader(@NotNull MechanicsPlugin plugin, @NotNull Class<T> serializerClass, @NotNull String relativeFolder) {
        this(plugin.getDataFolder(), plugin.getDebugger(), plugin.getClassLoader0(), serializerClass, relativeFolder);
    }

    public RootFileReader(@NotNull File pluginDataFolder, @NotNull MechanicsLogger debugger, @NotNull ClassLoader classLoader, @NotNull Class<T> serializerClass, @NotNull String relativeFolder) {
        this.pluginDataFolder = pluginDataFolder;
        this.debugger = debugger;
        this.classLoader = classLoader;
        this.serializerClass = serializerClass;
        this.relativeFolder = relativeFolder;
        this.rootFolder = new File(pluginDataFolder, relativeFolder);
        if (rootFolder.exists() && !rootFolder.isDirectory())
            throw new IllegalArgumentException("Root folder is not a directory: " + rootFolder.getAbsolutePath());
        this.serializers = new ArrayList<>();
        this.validators = new ArrayList<>();
    }

    public @NotNull Class<T> getSerializerClass() {
        return serializerClass;
    }

    public @NotNull String getRelativeFolder() {
        return relativeFolder;
    }

    public @NotNull File getRootFolder() {
        return rootFolder;
    }

    public @NotNull RootFileReader<R, T> withSerializers(@NotNull Collection<Serializer<?>> serializers) {
        this.serializers.addAll(serializers);
        return this;
    }

    public @NotNull RootFileReader<R, T> withValidators(@NotNull Collection<IValidator> validators) {
        this.validators.addAll(validators);
        return this;
    }

    /**
     * If the root folder does not exist, copy the default files from the jar to the root folder.
     * This method is great for forcing a required directory, or letting users delete the directory
     * so you can refill it with default values.
     */
    public @NotNull RootFileReader<R, T> assertFiles() {
        if (rootFolder.exists())
            return this;

        URL source = classLoader.getResource(pluginDataFolder.getName() + "/" + relativeFolder);
        Path dest = rootFolder.toPath();
        FileUtil.copyResourcesTo(source, dest);
        return this;
    }

    /**
     * Walks the file directory and tries to parse everything.
     *
     * <p>All YAML files will be read, and accumulated into 1 {@link Configuration}. Rarely,
     * a duplicate value may be detected (YAML doesn't allow for duplicate values, but when
     * combining many YAML files, duplicate values may occur). In this case, that file will
     * skip the deserialization phase and an error will be logged to console.
     *
     * @return The filled config
     */
    public @NotNull Configuration read() {
        try {
            // "accumulate" all the configs into 1 fat config
            Configuration accumulate = new FastConfiguration();
            FileReader fileReader = new FileReader(debugger, serializers, validators);
            ConfigTemplates templates = new ConfigTemplates(debugger);
            Path root = FileUtil.PathReference.of(rootFolder.toURI()).path();

            // Walk 1: register every template in templates/ folders. These are reusable config
            // fragments referenced via 'Path_To'; they are NOT instantiated as live entries.
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    if (isYaml(file) && isUnderTemplates(root, file)) {
                        SnakeYamlConfig yaml = load(file);
                        if (yaml != null)
                            templates.registerFile(file.toFile(), yaml.root());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // Walk 2: expand template references then serialize every entry file (skipping templates/).
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    if (!isYaml(file) || isUnderTemplates(root, file))
                        return FileVisitResult.CONTINUE;

                    SnakeYamlConfig config = load(file);
                    if (config == null)
                        return FileVisitResult.CONTINUE;

                    // Inline 'Path_To' references on the raw value tree before any serialization, so the
                    // expanded config is equivalent to hand-typed inline config.
                    TemplateExpander.expand(config.root(), templates, file.toFile(), debugger);

                    // By using the FileReader here, we can use all normal serializers and validators.
                    // This lets other plugins save their own data to the final config.
                    try {
                        accumulate.copyFrom(fileReader.fillOneFile(config, file.toFile()));
                    } catch (DuplicateKeyException ex) {
                        debugger.severe("Found duplicate keys in configuration!",
                                "This occurs when you have 2 lines in configuration with the same name... Usually due to copy-pasting directories",
                                "Duplicates Found: " + Arrays.toString(ex.getKeys()),
                                "Found in file: " + file);

                        debugger.finest("Duplicate Key Exception: ", ex);
                        return FileVisitResult.CONTINUE;
                    }

                    // Go through each key from root, and deserialize as the root type
                    for (String key : config.getKeys(null, false)) {
                        try {
                            SerializeData data = new SerializeData(file.toFile(), key, config);
                            R obj = data.of().assertExists().serialize(serializerClass).get();
                            accumulate.set(key, obj);
                        } catch (SerializerException ex) {
                            DiagnosticRenderer.log(debugger, config.enrich(ex.toDiagnostic()));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            return fileReader.useValidators(accumulate);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isYaml(@NotNull Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static boolean isUnderTemplates(@NotNull Path root, @NotNull Path file) {
        Path relative = root.relativize(file);
        for (int i = 0; i < relative.getNameCount() - 1; i++)
            if (relative.getName(i).toString().equalsIgnoreCase("templates"))
                return true;
        return false;
    }

    private @Nullable SnakeYamlConfig load(@NotNull Path file) {
        try {
            return SnakeYamlConfig.ofFile(file.toFile());
        } catch (IOException | InvalidConfigurationException ex) {
            debugger.warning("Failed to load " + file + "!", ex);
            return null;
        }
    }
}
