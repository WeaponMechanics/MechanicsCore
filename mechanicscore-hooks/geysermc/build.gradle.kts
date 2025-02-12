plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":mechanicscore-core"))
    compileOnly(libs.spigotApi)
    compileOnly(libs.geyser)
}
