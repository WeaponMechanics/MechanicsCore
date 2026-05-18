package me.deecaad.core;

import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import me.deecaad.core.events.QueueSerializerEvent;
import me.deecaad.core.events.triggers.EquipListener;
import me.deecaad.core.extensions.Extension;
import me.deecaad.core.file.SearchMode;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerInstancer;
import me.deecaad.core.listeners.ItemCraftListener;
import me.deecaad.core.listeners.MechanicsCastListener;
import me.deecaad.core.tick.TickManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarFile;

/**
 * Main class for MechanicsCore, handles registration of listeners and commands.
 */
public class MechanicsCore extends MechanicsPlugin {

    private static MechanicsCore INSTANCE;

    private TickManager tickManager;

    public void onLoad() {
        INSTANCE = this;
        CommandAPI.onLoad(new CommandAPIPaperConfig(this));
        super.onLoad();

        registerExtension("MythicMobs", "me.deecaad.core.extensions.MythicExtension");
        registerExtension("Geyser-Spigot", "me.deecaad.core.extensions.GeyserExtension");
    }

    public void onEnable() {
        CommandAPI.onEnable();
        super.onEnable();
    }

    @Override
    public void init() {
        super.init();
        tickManager = TickManager.create(this, getFoliaScheduler());
    }

    @Override
    public void onDisable() {
        if (tickManager != null) {
            tickManager.shutdown();
            tickManager = null;
        }
        super.onDisable();
    }

    /**
     * @return The shared tick manager for this plugin (drives all registered {@link
     *         me.deecaad.core.tick.Tickable}s).
     */
    public @NotNull TickManager getTickManager() {
        return tickManager;
    }

    /**
     * Registers extensions for various plugins.
     *
     * @param pluginName The name of the plugin that the extension uses (like MythicMobs)
     * @param extensionClass The class name of the extension
     */
    private void registerExtension(String pluginName, String extensionClass) {
        try {
            if (getServer().getPluginManager().isPluginEnabled(pluginName)) {
                Class<?> clazz = ReflectionUtil.getClass(extensionClass);
                Extension extension = (Extension) ReflectionUtil.getConstructor(clazz).newInstance();
                extension.register(this);
            }
        } catch (Throwable ex) {
            debugger.warning("Failed to hook into " + pluginName + "... " + pluginName + " might be outdated", ex);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> handleListeners() {
        Bukkit.getPluginManager().registerEvents(EquipListener.SINGLETON, this);
        Bukkit.getPluginManager().registerEvents(new ItemCraftListener(), this);
        Bukkit.getPluginManager().registerEvents(new MechanicsCastListener(), this);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQueue(QueueSerializerEvent event) throws IOException {
                List<Serializer<?>> serializers = new SerializerInstancer(new JarFile(getFile())).createAllInstances(getClassLoader(), SearchMode.ON_DEMAND);
                event.addSerializers(serializers);
            }
        }, this);

        return super.handleListeners();
    }

    @Override
    public @NotNull CompletableFuture<Void> handleCommands() {
        MechanicsCoreCommand.build();
        return super.handleCommands();
    }

    /**
     * @return the MechanicsCore plugin instance
     */
    public static @NotNull MechanicsCore getInstance() {
        return INSTANCE;
    }
}
