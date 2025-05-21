package me.deecaad.core

import com.cjcrafter.foliascheduler.FoliaCompatibility
import com.cjcrafter.foliascheduler.ServerImplementation
import com.cjcrafter.foliascheduler.TaskImplementation
import com.jeff_media.updatechecker.UpdateChecker
import me.deecaad.core.file.BukkitConfig
import me.deecaad.core.file.Configuration
import me.deecaad.core.file.FileReader
import me.deecaad.core.file.SerializeData
import me.deecaad.core.utils.FileUtil
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import org.bstats.bukkit.Metrics
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * The base class for plugins using MechanicsCore.
 *
 * @param updateChecker The update checker to use. If null, no update checking will be performed.
 * @param primaryColor The primary color to use for messages.
 * @param secondaryColor The secondary color to use for messages.
 * @param bStatsId The bStats id to use. If null, no metrics will be collected.
 */
open class MechanicsPlugin(
    val updateChecker: UpdateChecker? = null,
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
     * The Adventure API for sending messages to players.
     */
    val adventure by lazy { BukkitAudiences.create(this) }

    override fun onLoad() {
        val start = System.currentTimeMillis()

        foliaScheduler = FoliaCompatibility(this).serverImplementation
        handleConfigs().join()
        handleExtensions().join()

        val end = System.currentTimeMillis()
        debugger.info("Loaded in ${end - start}ms")
    }

    override fun onEnable() {
        val start = System.currentTimeMillis()

        handleCommands().join()
        handleListeners().join()
        handleMetrics().join()

        val end = System.currentTimeMillis()
        debugger.info("Enabled in ${end - start}ms")
    }

    override fun onDisable() {
        HandlerList.unregisterAll(this)
        foliaScheduler.cancelTasks()
        adventure.close()
    }

    /**
     * Attempts to reload this plugin and all its components.
     */
    fun reload(): CompletableFuture<TaskImplementation<Void>> {
        return foliaScheduler.async().runNow { _ ->
            handleFiles()
        }.asFuture().thenCompose {
            foliaScheduler.global().run { _ ->
                handleConfigs().join()
                handleListeners().join()
                handleCommands().join()
                handlePermissions().join()
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
        var loggerConfig = MechanicsLogger.LoggerConfig()
        val configYml = File(dataFolder, "config.yml")
        if (configYml.exists()) {
            val data = SerializeData(configYml, null, BukkitConfig(config))
            loggerConfig = data.of("Logger_Config").assertExists().serialize(MechanicsLogger.LoggerConfig::class.java).get()
        }
        debugger = MechanicsLogger(this, loggerConfig)

        return CompletableFuture.completedFuture(null)
    }

    /**
     * Loads all configs for this plugin (serialization).
     */
    open fun handleConfigs(): CompletableFuture<Void> {
        val configReader = FileReader(debugger, listOf(), listOf())
        configuration = configReader.fillOneFile(File(dataFolder, "config.yml"))
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
