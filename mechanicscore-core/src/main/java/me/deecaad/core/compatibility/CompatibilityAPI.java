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

public final class CompatibilityAPI {

    private static ICompatibility compatibility;
    private static WorldGuardCompatibility worldGuardCompatibility;
    private static IVaultCompatibility vaultCompatibility;

    static {
        try {
            compatibility = new CompatibilitySetup().getCompatibleVersion(ICompatibility.class, "me.deecaad.core.compatibility");

            // When we don't have a compatibility interface for this version
            if (compatibility == null) {
                MechanicsCore.getInstance().getDebugger().severe("Unsupported server version: " + MinecraftVersions.getCurrent() + " (" + Bukkit.getBukkitVersion() + ")",
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
            MechanicsCore.getInstance().getDebugger().severe("Failed to init CompatibilityAPI", ex);
        }
    }

    public static ICompatibility getCompatibility() {
        return compatibility;
    }

    public static EntityCompatibility getEntityCompatibility() {
        return compatibility.getEntityCompatibility();
    }

    public static BlockCompatibility getBlockCompatibility() {
        return compatibility.getBlockCompatibility();
    }

    public static NBTCompatibility getNBTCompatibility() {
        return compatibility.getNBTCompatibility();
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