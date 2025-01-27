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

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.release.set(21)
    }
}
