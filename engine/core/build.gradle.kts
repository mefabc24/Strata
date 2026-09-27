plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "com.mefabc24.strata"
version = "0.1.0"

dependencies {
    api("com.badlogicgames.gdx:gdx:1.14.2")

    testImplementation(kotlin("test"))
    testImplementation(project(":engine:tools"))
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
