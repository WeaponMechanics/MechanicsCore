package me.deecaad.core.compatibility;

import com.cjcrafter.foliascheduler.util.ConstructorInvoker;
import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.block.BlockCompatibility;
import me.deecaad.core.compatibility.entity.EntityCompatibility;
import me.deecaad.core.compatibility.nbt.NBTCompatibility;
import me.deecaad.core.compatibility.vault.IVaultCompatibility;
import me.deecaad.core.compatibility.worldguard.NoWorldGuard;
import me.deecaad.core.compatibility.worldguard.WorldGuardCompatibility;
import me.deecaad.core.utils.LogLevel;
import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class CompatibilityAPI {

    private static final Logger FALLBACK_LOGGER = Logger.getLogger("MechanicsCore");

    private static ICompatibility compatibility;
    private static WorldGuardCompatibility worldGuardCompatibility;
    private static IVaultCompatibility vaultCompatibility;

    static {
        try {
            compatibility = new CompatibilitySetup().getCompatibleVersion(ICompatibility.class, "me.deecaad.core.compatibility");

            // When we don't have a compatibility interface for this version
            if (compatibility == null) {
                severe(null, "Unsupported server version: " + MinecraftVersions.getCurrent() + " (" + Bukkit.getBukkitVersion() + ")",
                    "The protocol version " + MinecraftVersions.getCurrent().toProtocolString() + " has no compatibility class...",
                    "If you are using a new version of Minecraft, you might need to update your plugins!",
                    "!!! CRITICAL ERROR !!!");
            }

            WorldGuardCompatibility worldGuardCompatibility1;
            try {
                // Check if WorldGuard is there
                Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                ConstructorInvoker<?> worldGuardV7Constructor = ReflectionUtil.getConstructor(Class.forName("me.deecaad.core.compatibility.worldguard.WorldGuardV7"));
                worldGuardCompatibility1 = (WorldGuardCompatibility) worldGuardV7Constructor.newInstance();
            } catch (Throwable e) {
                worldGuardCompatibility1 = new NoWorldGuard();
            }
            worldGuardCompatibility = worldGuardCompatibility1;
        } catch (Throwable ex) {
            severe(ex, "Failed to init CompatibilityAPI");
        }
    }

    /**
     * Logs a severe message via the plugin debugger when a live plugin exists, falling back to a plain
     * JUL logger otherwise. This lets {@link CompatibilityAPI} initialize headless (tests/verification)
     * without a {@link MechanicsCore} instance.
     */
    private static void severe(Throwable ex, String... messages) {
        MechanicsCore instance = MechanicsCore.getInstance();
        if (instance != null) {
            if (ex != null)
                instance.getDebugger().severe(messages.length == 0 ? "" : messages[0], ex);
            else
                instance.getDebugger().severe(messages);
            return;
        }
        for (String message : messages)
            FALLBACK_LOGGER.severe(message);
        if (ex != null)
            FALLBACK_LOGGER.log(Level.SEVERE, messages.length == 0 ? "" : messages[0], ex);
    }

    public static ICompatibility getCompatibility() {
        return compatibility;
    }

    public static EntityCompatibility getEntityCompatibility() {
        return ensureCompatibility().getEntityCompatibility();
    }

    public static BlockCompatibility getBlockCompatibility() {
        return ensureCompatibility().getBlockCompatibility();
    }

    public static NBTCompatibility getNBTCompatibility() {
        return ensureCompatibility().getNBTCompatibility();
    }

    /**
     * The NMS compatibility layer, or a clear error when none exists. {@code compatibility} is null
     * when running without a real server (e.g. headless verification under MockBukkit), since there is
     * no {@code net.minecraft.server}. Features that touch NMS can only be verified in-game.
     */
    private static ICompatibility ensureCompatibility() {
        if (compatibility == null)
            throw new NoCompatibilityException("No NMS compatibility layer is available "
                + "(running headless without a real server?). NMS-backed features like item NBT/Tags "
                + "can only be verified in-game.");
        return compatibility;
    }

    public static WorldGuardCompatibility getWorldGuardCompatibility() {
        return worldGuardCompatibility;
    }

    public static IVaultCompatibility getVaultCompatibility() {
        if (vaultCompatibility == null) {
            boolean hasVault = Bukkit.getPluginManager().getPlugin("Vault") != null;
            String path = "me.deecaad.core.compatibility.vault." + (hasVault ? "VaultCompatibility" : "NoVaultCompatibility");
            ConstructorInvoker<?> vaultCompatibilityConstructor = ReflectionUtil.getConstructor(ReflectionUtil.getClass(path));
            vaultCompatibility = (IVaultCompatibility) vaultCompatibilityConstructor.newInstance();
        }
        return vaultCompatibility;
    }
}