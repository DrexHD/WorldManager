pluginManagement {
	repositories {
		maven ("https://maven.fabricmc.net/")
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
	create(rootProject) {
		versions("26.1", "26.2", "26.3")
		vcsVersion = "26.3"
	}
}
