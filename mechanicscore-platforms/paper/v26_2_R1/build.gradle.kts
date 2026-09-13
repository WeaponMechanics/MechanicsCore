plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") // FoliaScheduler Snapshots
}

dependencies {
    compileOnly(project(":mechanicscore-core"))
    compileOnly(libs.foliaScheduler)

    paperweight.paperDevBundle("26.2.build.121-stable")
}

// Minecraft 26.1 (Tiny Takeover, March 2026) requires Java 25
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

// Paper 26.1+ ships mojang-mapped at runtime, so no reobf step is needed.
tasks.named("reobfJar") { enabled = false }
tasks.named("assemble") { dependsOn(tasks.named("jar")) }

configurations {
    listOf("apiElements", "runtimeElements").forEach { name ->
        named(name).configure {
            outgoing.artifacts.clear()
            outgoing.artifact(tasks.named("jar"))
        }
    }
}
