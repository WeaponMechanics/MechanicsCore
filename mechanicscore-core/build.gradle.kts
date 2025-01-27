plugins {
    `java-library`
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    // Core Minecraft dependencies
    compileOnly(libs.spigotApi)
    compileOnly(libs.packetEvents)

    // External "hooks" or plugins that we might interact with
    compileOnly(libs.geyser)
    compileOnly(libs.placeholderApi)
    compileOnly(libs.mythicMobs)

    // Shaded dependencies
    compileOnly(libs.adventureApi)
    compileOnly(libs.adventureBukkit)
    compileOnly(libs.adventureTextLegacy)
    compileOnly(libs.adventureTextMinimessage)
    compileOnly(libs.commandApi)
    compileOnly(libs.fastUtil)
    compileOnly(libs.foliaScheduler)
    compileOnly(libs.hikariCp)
    compileOnly(libs.jsonSimple)
    compileOnly(libs.xSeries)

    // Testing dependencies
    testImplementation(libs.spigotApi)
    testImplementation(libs.annotations)
    testImplementation(libs.foliaScheduler)
}
