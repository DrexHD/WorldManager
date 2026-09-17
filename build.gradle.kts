import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension

plugins {
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
    id("maven-publish")
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
    id("org.jetbrains.changelog")
}

version = findProperty("mod_version") as String + "+" + findProperty("minecraft_version")
group = findProperty("maven_group") as String

base {
    archivesName = findProperty("archives_base_name") as String
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(findProperty("java_version") as String)
}

repositories {
    mavenCentral()
    maven("https://maven.nucleoid.xyz/")
}

loom {
    splitEnvironmentSourceSets()

    runConfigs.all {
        ideConfigGenerated(true)
    }
}

fun DependencyHandlerScope.includeDep(dep: String) {
    include(implementation(dep)!!)
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${findProperty("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${findProperty("loader_version")}")

    implementation("net.fabricmc.fabric-api:fabric-api:${findProperty("fabric_version")}")

    includeDep("xyz.nucleoid:fantasy:${findProperty("fantasy_version")}")

    includeDep("me.lucko:fabric-permissions-api:${findProperty("permission_api_version")}")

    includeDep("eu.pb4:sgui:${findProperty("sgui_version")}")

    includeDep("eu.pb4:placeholder-api:${findProperty("placeholder_api_version")}")
    includeDep("eu.pb4:player-data-api:${findProperty("player_data_api_version")}")

    includeDep("com.github.junrar:junrar:${findProperty("junrar_version")}")
    includeDep("org.apache.commons:commons-compress:${findProperty("apache_common_compress_version")}")
}

publishMods {
    file.set(tasks.jar.get().archiveFile)
    type.set(STABLE)
    changelog.set(fetchChangelog())

    displayName = "WorldManager ${version.get()}"
    modLoaders.add("fabric")
    modLoaders.add("quilt")


    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = "1235570"
        minecraftVersions.addAll(findProperty("curseforge_minecraft_versions")!!.toString().split(", "))
    }
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "vbAJDaEx"
        minecraftVersions.addAll(findProperty("modrinth_minecraft_versions")!!.toString().split(", "))
    }
    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = providers.environmentVariable("GITHUB_REPOSITORY").getOrElse("DrexHD/WorldManager")
        commitish = providers.environmentVariable("GITHUB_REF_NAME").getOrElse("main")
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version,
            "javaVersion" to findProperty("java_version")
        )

        inputs.properties(props)

        filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
            expand(props)
        }
    }
}

fun fetchChangelog(): String {
    val log = rootProject.extensions.getByType<ChangelogPluginExtension>()
    val modVersion = findProperty("mod_version")!!.toString()
    return if (log.has(modVersion)) {
        log.renderItem(
                log.get(modVersion).withHeader(false),
                Changelog.OutputType.MARKDOWN
        )
    } else {
        ""
    }
}