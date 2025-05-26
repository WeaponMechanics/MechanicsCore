plugins {
    `java-library`
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
}

dependencies {
    // Core Minecraft dependencies
    compileOnly(libs.brigadier)
    compileOnly(libs.spigotApi)
    compileOnly(libs.packetEvents)

    // External "hooks" or plugins that we might interact with
    compileOnly(libs.placeholderApi)

    // Shaded dependencies
    compileOnly(libs.adventureApi)
    compileOnly(libs.adventureBukkit)
    compileOnly(libs.adventureTextLegacy)
    compileOnly(libs.adventureTextMinimessage)
    compileOnly(libs.annotations)
    compileOnly(libs.bstats)
    compileOnly(libs.commandApi)
    compileOnly(libs.fastUtil)
    compileOnly(libs.foliaScheduler)
    compileOnly(libs.hikariCp)
    compileOnly(libs.jsonSimple)
    compileOnly(libs.spigotUpdateChecker)
    compileOnly(libs.xSeries)

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
            url = layout.buildDirectory.dir("staging-deploy").map { it.asFile.toURI() }.get()
        }
    }
}
