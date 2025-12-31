plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.9"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
}

dependencies {
    // Main project code
    implementation(project(":mechanicscore-core"))

    // Platforms and hook modules
    file("../mechanicscore-hooks").listFiles()?.forEach {
        implementation(project(":${it.name}"))
    }
    file("../mechanicscore-platforms/paper").listFiles()?.forEach {
        implementation(project(":${it.name}"))
    }

    implementation(libs.commandApiShade)
}

paperPluginYaml {
    val versionProperty = findProperty("version") as? String ?: throw IllegalArgumentException("version was null")

    main = "me.deecaad.core.MechanicsCore"
    name = "MechanicsCore"
    version = versionProperty
    apiVersion = "1.21"
    foliaSupported = true

    authors = listOf("DeeCaaD", "CJCrafter")
    dependencies {
        server("WorldEdit", required = false)
        server("WorldGuard", required = false)
        server("PlaceholderAPI", required = false)
        server("MythicMobs", required = false)
        server("GeyserSpigot", required = false)
    }
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
