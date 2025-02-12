plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(project(":mechanicscore-core"))
    compileOnly(libs.adventureApi)
    compileOnly(libs.foliaScheduler)

    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}
