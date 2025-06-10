<div align="center">

# Mechanics Core

[![Download](https://img.shields.io/github/downloads/WeaponMechanics/MechanicsCore/total?color=green)](https://github.com/WeaponMechanics/MechanicsCore/releases/latest)
[![Version](https://img.shields.io/github/v/release/WeaponMechanics/MechanicsCore?include_prereleases&label=version)](https://github.com/WeaponMechanics/MechanicsCore/releases/latest)
[![Wiki](https://img.shields.io/badge/-wiki%20-blueviolet)](https://cjcrafter.gitbook.io/core/)
[![License](https://img.shields.io/github/license/WeaponMechanics/MechanicsCore)](https://github.com/WeaponMechanics/MechanicsCore/blob/master/LICENSE)

</div>

Spigot library plugin including:
- Adventure for messages
- Jorel's CommandAPI for command handling
- FoliaScheduler for Folia support
- xSeries for serialization

With unique features:
- Advanced config deserialization with human-readable error messages
- Custom NBT serialization for `String[]`
- EntityEquipmentEvent for armor, hand, and offhand item changes
- Fake Entities
- Fake Blocks
- Modular Mechanic system for configurable actions at events (sounds, particles, etc.)
- Registry handling

## How to (Server Owners)
You might need this plugin if you are using any of my other plugins, such as:
- [WeaponMechanics](https://github.com/WeaponMechanics/WeaponMechanics)

## How to (Developers)
See [Contributing](https://github.com/weaponmechanics/mechanicsmain/contribute) for information on how to contribute to the project.

### Maven
MechanicsCore is available on [Maven Central](https://central.sonatype.com/artifact/com.cjcrafter/mechanicscore).
TO use it, add the following to your `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>com.cjcrafter</groupId>
        <artifactId>mechanicscore</artifactId>
        <version>4.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle
Add the following into your `build.gradle.kts`:
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.cjcrafter:weaponmechanics:4.1.0")
}
```

### Snapshots
Snapshots for MechanicsCore are available on Sonatype's snapshot repository.
```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```
or for Gradle:
```kotlin
repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
}
```

## Donate
Supporting the developers helps to keep the project alive and allows us to continue improving it.

* [PayPal](https://www.paypal.com/paypalme/cjcrafter)
* [GitHub Sponsors](https://github.com/sponsors/CJCrafter/)
