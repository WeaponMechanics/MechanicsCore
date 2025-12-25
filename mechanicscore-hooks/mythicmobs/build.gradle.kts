plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":mechanicscore-core"))
    compileOnly(libs.paper)
    compileOnly(libs.mythicMobs)
}
