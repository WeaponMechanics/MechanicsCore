plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14" apply false
    kotlin("jvm") version libs.versions.kotlin apply false
}

allprojects {
    subprojects {
        pluginManager.withPlugin("java") {
            tasks.withType<JavaCompile>().configureEach {
                options.release.set(21)
                options.encoding = Charsets.UTF_8.name()
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = "21"
                }
            }
        }
    }
}
