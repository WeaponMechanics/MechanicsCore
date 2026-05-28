import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT" apply false
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
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("failed", "skipped", "passed")
                exceptionFormat = TestExceptionFormat.FULL
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
