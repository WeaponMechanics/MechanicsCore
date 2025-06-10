import org.jreleaser.model.Active

plugins {
    `java-library`
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
    id("org.jreleaser") version "1.18.0"
}

dependencies {
    // Core Minecraft dependencies
    compileOnly(libs.authlib)
    compileOnly(libs.brigadier)
    compileOnly(libs.spigotApi)
    compileOnly(libs.packetEvents)

    // External "hooks" or plugins that we might interact with
    compileOnly(libs.placeholderApi)

    // Shaded dependencies
    implementation(libs.adventureApi)
    implementation(libs.adventureBukkit)
    implementation(libs.adventureTextLegacy)
    implementation(libs.adventureTextMinimessage)
    implementation(libs.adventureTextPlain)
    implementation(libs.annotations)
    implementation(libs.bstats)
    compileOnly(libs.commandApi)
    compileOnly(libs.fastUtil)
    implementation(libs.foliaScheduler)
    implementation(libs.hikariCp) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation(libs.spigotUpdateChecker)
    implementation(libs.xSeries)

    // Testing dependencies
    testImplementation(libs.spigotApi)
    testImplementation(libs.annotations)
    testImplementation(libs.foliaScheduler)
    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc").map { it.outputs.files })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = "com.cjcrafter"
            artifactId = "mechanicscore"
            version = findProperty("mechanicscore.version").toString()

            pom {
                name.set("MechanicsCore")
                description.set("A plugin that adds scripting capabilities to Plugins")
                url.set("https://github.com/WeaponMechanics/MechanicsCore")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("CJCrafter")
                        name.set("Collin Barber")
                        email.set("collinjbarber@gmail.com")
                    }
                    developer {
                        id.set("DeeCaaD")
                        name.set("DeeCaaD")
                        email.set("perttu.kangas@hotmail.fi")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/WeaponMechanics/MechanicsCore.git")
                    developerConnection.set("scm:git:ssh://github.com/WeaponMechanics/MechanicsCore.git")
                    url.set("https://github.com/WeaponMechanics/MechanicsCore")
                }
            }
        }
    }

    // Deploy this repository locally for staging, then let the root project actually
    // upload the maven repo using jReleaser
    repositories {
        maven {
            name = "stagingDeploy"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    gitRootSearch.set(true)

    project {
        name.set("MechanicsCore")
        group = "com.cjcrafter"
        version = findProperty("mechanicscore.version").toString()
        description = "A plugin that adds scripting capabilities to Plugins"
        authors.add("CJCrafter <collinjbarber@gmail.com>")
        authors.add("DeeCaaD <perttu.kangas@hotmail.fi>")
        license = "MIT" // SPDX identifier
        copyright = "Copyright © 2023-2025 CJCrafter, DeeCaaD"

        links {
            homepage.set("https://github.com/WeaponMechanics/MechanicsCore")
            documentation.set("https://github.com/WeaponMechanics/MechanicsCore#readme")
        }

        java {
            groupId = "com.cjcrafter"
            artifactId = "mechanicscore"
            version = findProperty("mechanicscore.version").toString()
        }

        snapshot {
            fullChangelog.set(true)
        }
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("releaseDeploy") {
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    // run ./gradlew mechanicscore-core:publish before deployment
                    stagingRepository("build/staging-deploy")
                    // Credentials (JRELEASER_MAVENCENTRAL_USERNAME, JRELEASER_MAVENCENTRAL_PASSWORD or JRELEASER_MAVENCENTRAL_TOKEN)
                    // will be picked up from ~/.jreleaser/config.toml
                }
            }

            nexus2 {
                create("sonatypeSnapshots") {
                    active.set(Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots/")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                    applyMavenCentralRules = true
                    snapshotSupported = true
                    closeRepository = true
                    releaseRepository = true
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }

    distributions {
        create("mechanicscore") {
            active.set(Active.ALWAYS)
            distributionType.set(org.jreleaser.model.Distribution.DistributionType.SINGLE_JAR)
            artifact {
                path.set(file("../mechanicscore-build/build/libs/MechanicsCore-${findProperty("mechanicscore.version")}.jar"))
            }
        }
    }

    release {
        github {
            repoOwner.set("WeaponMechanics")
            name.set("MechanicsCore")
            host.set("github.com")

            val version = findProperty("mechanicscore.version").toString()
            val isSnapshot = version.endsWith("-SNAPSHOT")
            releaseName.set(if (isSnapshot) "SNAPSHOT" else "v$version")
            tagName.set("v{{projectVersion}}")
            draft.set(false)
            skipTag.set(isSnapshot)
            overwrite.set(false)
            update { enabled.set(isSnapshot) }

            prerelease {
                enabled.set(isSnapshot)
                pattern.set(".*-SNAPSHOT")
            }

            commitAuthor {
                name.set("Collin Barber")
                email.set("collinjbarber@gmail.com")
            }

            changelog {
                formatted.set(Active.ALWAYS)
                preset.set("conventional-commits")
                format.set("- {{commitShortHash}} {{commitTitle}}")
                contributors {
                    enabled.set(true)
                    format.set("{{contributorUsernameAsLink}}")
                }
                hide {
                    contributors.set(listOf("[bot]"))
                }
            }
        }
    }
}
