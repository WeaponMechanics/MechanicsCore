plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17" apply false
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

        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).apply {
                encoding = Charsets.UTF_8.name()
                charSet = Charsets.UTF_8.name()
                addStringOption("Xdoclint:none", "-quiet")
                links("https://docs.oracle.com/en/java/javase/21/docs/api/")
            }
        }
    }
}
