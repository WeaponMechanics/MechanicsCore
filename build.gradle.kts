plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14" apply false
    kotlin("jvm") version libs.versions.kotlin apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME") ?: (findProperty("OSSRH_USERNAME") as? String ?: ""))
            password.set(System.getenv("OSSRH_PASSWORD") ?: (findProperty("OSSRH_PASSWORD") as? String ?: ""))
        }
    }
}
