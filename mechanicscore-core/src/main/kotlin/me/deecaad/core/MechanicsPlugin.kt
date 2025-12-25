package me.deecaad.core

import com.cjcrafter.foliascheduler.FoliaCompatibility
import com.cjcrafter.foliascheduler.ServerImplementation
import com.cjcrafter.foliascheduler.TaskImplementation
import me.deecaad.core.file.Configuration
import me.deecaad.core.file.FastConfiguration
import me.deecaad.core.file.FileReader
import me.deecaad.core.file.IValidator
import me.deecaad.core.file.JarInstancer
import me.deecaad.core.file.SearchMode
import me.deecaad.core.file.SerializerInstancer
import me.deecaad.core.utils.FileUtil
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import org.bstats.bukkit.Metrics
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.jar.JarFile
import java.util.logging.Level

/**
 * The base class for plugins using MechanicsCore.
 *
 * @param primaryColor The primary color to use for messages.
 * @param secondaryColor The secondary color to use for messages.
 * @param bStatsId The bStats id to use. If null, no metrics will be collected.
 */
open class MechanicsPlugin(
    val primaryColor: Style = Style.style(NamedTextColor.GOLD),
    val secondaryColor: Style = Style.style(NamedTextColor.GRAY),
    bStatsId: Int? = null,
): JavaPlugin() {

    /**
     * The scheduler compatibility layer.
     */
    lateinit var foliaScheduler: ServerImplementation

    /**
     * The parsed `config.yml` file. This is automatically loaded when
     * [onLoad] is called.
     */
    lateinit var configuration: Configuration

    /**
     * The logger for this plugin.
     */
    lateinit var debugger: MechanicsLogger

    /**
     * The bStats metrics object, or null if there is no bStats id.
     */
    var metrics: Metrics? = bStatsId?.let { Metrics(this, it) }


    /**
     * Expose the class loader
     */
    val classLoader0: ClassLoader = super.getClassLoader()

    override fun onLoad() {
        val start = System.currentTimeMillis()

        // Setup a debugger with "dummy config"... will be replaced later in handleFiles
        debugger = MechanicsLogger(this, MechanicsLogger.LoggerConfig(printLevel = Level.CONFIG))

        foliaScheduler = FoliaCompatibility(this).serverImplementation
        handleFiles().join()
        handleExtensions().join()

        val end = System.currentTimeMillis()
        debugger.info("Loaded in ${end - start}ms")
    }

    override fun onEnable() {
        val start = System.currentTimeMillis()

        init()
        handlePermissions().join()
        handleCommands().join()
        handleListeners().join()
        handlePacketListeners().join()
        handleMetrics().join()

        debugger.info("Scheduling config loading task to be run in 1 tick...")
        foliaScheduler.global().run { _ ->
            val configStart = System.currentTimeMillis()
            handleConfigs().join()
            val configEnd = System.currentTimeMillis()
            debugger.info("Loaded configs in ${configEnd - configStart}ms")
        }

        val end = System.currentTimeMillis()
        debugger.info("Enabled in ${end - start}ms")
    }

    override fun onDisable() {
        HandlerList.unregisterAll(this)
        foliaScheduler.cancelTasks()
    }

    /**
     * Sets up state for this plugin, called during [onLoad] and [reload],
     * typically right after destroying state in [onDisable].
     */
    open fun init() {
    }

    /**
     * Attempts to reload this plugin and all its components.
     */
    open fun reload(): CompletableFuture<TaskImplementation<Void>> {
        onDisable()
        init()
        return foliaScheduler.async().runNow { _ ->
            handleFiles()
        }.asFuture().thenCompose {
            foliaScheduler.global().run { _ ->
                handleConfigs().join()
                handleListeners().join()
                handleCommands().join()
            }.asFuture()
        }
    }

    /**
     * Registers all commands for this plugin.
     */
    open fun handleCommands(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Registers all extensions for this plugin.
     */
    open fun handleExtensions(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Writes all files for this plugin to the data folder. This includes
     * the config.yml file and any other files in the datafolder.
     */
    open fun handleFiles(): CompletableFuture<Void> {
        val pluginName = description.name
        if (!dataFolder.exists() || dataFolder.listFiles()?.isEmpty() != false) {
            FileUtil.copyResourcesTo(classLoader.getResource(pluginName), dataFolder.toPath())
        }
        FileUtil.ensureDefaults(classLoader.getResource("$pluginName/config.yml"), File(dataFolder, "config.yml"))

        // Configures and initializes the logger
        var loggerConfig = MechanicsLogger.LoggerConfig(printLevel = Level.CONFIG)
        val configYml = File(dataFolder, "config.yml")
        if (configYml.exists()) {

            // This configuration should be loaded as soon as possible. This means that no external
            // plugin can add serializers/validators to any plugin's config.yml
            val serializers = SerializerInstancer(JarFile(file)).createAllInstances(classLoader, SearchMode.ENABLED).toMutableList()
            val validators = JarInstancer(JarFile(file)).createAllInstances(IValidator::class.java, classLoader, SearchMode.ENABLED)

            // All plugins are expected to have a logger config
            if (!serializers.any { it is MechanicsLogger.LoggerConfig })
                serializers.add(MechanicsLogger.LoggerConfig())

            val configReader = FileReader(debugger, serializers, validators)
            configuration = configReader.fillOneFile(File(dataFolder, "config.yml"))
            configReader.usePathToSerializersAndValidators(configuration)
            val tempConfig = configuration.getObject("Logger_Config", MechanicsLogger.LoggerConfig::class.java)
            if (tempConfig == null) {
                debugger.severe("Missing required section 'Logger_Config' somehow...")
            } else {
                loggerConfig = tempConfig
            }
        } else {
            configuration = FastConfiguration()  // empty config default
        }
        debugger = MechanicsLogger(this, loggerConfig)

        return CompletableFuture.completedFuture(null)
    }

    /**
     * Loads all configs for this plugin (serialization).
     */
    open fun handleConfigs(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Registers all permissions for this plugin.
     */
    open fun handlePermissions(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Registers all listeners for this plugin.
     */
    open fun handleListeners(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Registers all packet listeners for this plugin (using PacketEvents).
     */
    open fun handlePacketListeners(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Registers all bstats metrics for this plugin ([metrics] is already setup;
     * don't try to create a new one. Instead, use this method to register custom
     * pie charts.
     */
    open fun handleMetrics(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }
}
