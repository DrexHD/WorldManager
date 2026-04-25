pluginManagement {
	repositories {
		maven ("https://maven.fabricmc.net/")
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.2"
}

stonecutter {
	create(rootProject) {
		versions("26.1")
		vcsVersion = "26.1"
	}
}