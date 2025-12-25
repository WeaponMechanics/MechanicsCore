plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.9"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.2.0"
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

    implementation(libs.commandApiShade)
}

paperPluginYaml {
    val versionProperty = findProperty("version") as? String ?: throw IllegalArgumentException("version was null")

    main = "me.deecaad.core.MechanicsCore"
    name = "MechanicsCore"
    version = versionProperty
    apiVersion = "1.13"  // Use 1.13, since apiVersion was added in 1.13
    foliaSupported = true

    //load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors = listOf("DeeCaaD", "CJCrafter")
    //loadBefore = listOf("WorldEdit", "WorldGuard", "PlaceholderAPI", "MythicMobs", "Geyser-Spigot")
}

tasks.shadowJar {
    val versionProperty = findProperty("version") as? String ?: throw IllegalArgumentException("version was null")
    archiveFileName.set("MechanicsCore-$versionProperty.jar")

    val libPackage = "me.deecaad.core.lib"

    relocate("org.bstats", "$libPackage.bstats")
    relocate("dev.jorel.commandapi", "$libPackage.commandapi")
    relocate("com.cjcrafter.foliascheduler", "$libPackage.scheduler")
    relocate("com.zaxxer.hikari", "$libPackage.hikari")
    relocate("kotlin.", "$libPackage.kotlin.")
    relocate("com.cryptomorin.xseries", "$libPackage.xseries")
}
