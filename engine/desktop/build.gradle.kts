plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "com.mefabc24.strata"
version = "0.1.0"

dependencies {
    api(project(":engine:core"))

    api("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2")

    runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")

    testImplementation(kotlin("test"))
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
