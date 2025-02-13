plugins {
    `java-library`
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    // Core Minecraft dependencies
    compileOnly(libs.brigadier)
    compileOnly(libs.spigotApi)
    compileOnly(libs.packetEvents)

    // External "hooks" or plugins that we might interact with
    compileOnly(libs.placeholderApi)

    // Shaded dependencies
    compileOnly(libs.adventureApi)
    compileOnly(libs.adventureBukkit)
    compileOnly(libs.adventureTextLegacy)
    compileOnly(libs.adventureTextMinimessage)
    compileOnly(libs.annotations)
    compileOnly(libs.bstats)
    compileOnly(libs.commandApi)
    compileOnly(libs.fastUtil)
    compileOnly(libs.foliaScheduler)
    compileOnly(libs.hikariCp)
    compileOnly(libs.jsonSimple)
    compileOnly(libs.spigotUpdateChecker)
    compileOnly(libs.xSeries)

    // Testing dependencies
    testImplementation(libs.spigotApi)
    testImplementation(libs.annotations)
    testImplementation(libs.foliaScheduler)
}
