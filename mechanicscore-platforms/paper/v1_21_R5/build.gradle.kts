plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") // FoliaScheduler Snapshots
}

dependencies {
    compileOnly(project(":mechanicscore-core"))
    compileOnly(libs.adventureApi)
    compileOnly(libs.foliaScheduler)

    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}
