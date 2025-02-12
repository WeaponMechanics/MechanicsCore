import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
}

dependencies {
    // Main project code
    implementation(project(":mechanicscore-core"))

    // Platforms and hook modules
    file("../mechanicscore-hooks").listFiles()?.forEach {
        implementation(project(":${it.name}"))
    }
    file("../mechanicscore-platforms/paper").listFiles()?.forEach {
        implementation(project(":${it.name}", "reobf"))
    }

    // Implementation for all shaded libraries:
    implementation(libs.adventureApi)
    implementation(libs.adventureBukkit)
    implementation(libs.adventureTextLegacy)
    implementation(libs.adventureTextMinimessage)
    implementation(libs.bstats)
    implementation(libs.commandApiShade)
    implementation(libs.foliaScheduler)
    implementation(libs.hikariCp)
    implementation(libs.kotlinStdlib)
    implementation(libs.spigotUpdateChecker)
    implementation(libs.xSeries)
}

bukkitPluginYaml {
    val versionProperty = findProperty("mechanicscore.version") as? String ?: throw IllegalArgumentException("mechanicscore.version was null")

    main = "me.deecaad.core.MechanicsCore"
    name = "MechanicsCore"
    version = versionProperty
    apiVersion = "1.13"  // Use 1.13, since apiVersion was added in 1.13
    foliaSupported = true

    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors = listOf("DeeCaaD", "CJCrafter")
    loadBefore = listOf("WorldEdit", "WorldGuard", "PlaceholderAPI", "MythicMobs", "Geyser-Spigot")
}

tasks.shadowJar {
    val versionProperty = findProperty("mechanicscore.version") as? String ?: throw IllegalArgumentException("mechanicscore.version was null")
    archiveFileName.set("MechanicsCore-$versionProperty.jar")

    dependencies {
        // Main project code
        include(project(":mechanicscore-core"))

        // Platforms and hook modules
        file("../mechanicscore-hooks").listFiles()?.filter { it.isDirectory }?.forEach {
            include(project(":${it.name}"))
        }
        file("../mechanicscore-platforms/paper").listFiles()?.filter { it.isDirectory }?.forEach {
            include(project(":${it.name}"))
        }

        /**
         * Shades the library into a new location, to avoid conflicts with other
         * plugins that may use the same library.
         */
        fun relocateLib(
            lib: Provider<MinimalExternalModuleDependency>,
            fromPackage: String,
            toPackage: String,
        ) {
            relocate(fromPackage, toPackage) {
                val group = lib.get().group
                val name = lib.get().name
                include(dependency("$group:$name"))
            }
        }

        val libPackage = "me.deecaad.core.lib"
        relocateLib(libs.adventureApi, "net.kyori", "$libPackage.kyori")
        relocateLib(libs.adventureBukkit, "net.kyori", "$libPackage.kyori")
        relocateLib(libs.adventureTextLegacy, "net.kyori", "$libPackage.kyori")
        relocateLib(libs.adventureTextMinimessage, "net.kyori", "$libPackage.kyori")
        relocateLib(libs.commandApiShade, "dev.jorel.commandapi", "$libPackage.commandapi")
        relocateLib(libs.foliaScheduler, "com.cjcrafter.foliascheduler", "$libPackage.scheduler")
        relocateLib(libs.hikariCp, "com.zaxxer.hikari", "$libPackage.hikari")
        relocateLib(libs.kotlinStdlib, "kotlin.", "$libPackage.kotlin.")
        relocateLib(libs.xSeries, "com.cryptomorin.xseries", "$libPackage.xseries")
    }
}
