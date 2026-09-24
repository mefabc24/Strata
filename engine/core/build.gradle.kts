plugins {
    kotlin("jvm")
    `java-library`
}

group = "com.mefabc24.strata"
version = "0.1.0"

dependencies {
    api("com.badlogicgames.gdx:gdx:1.14.2")

    testImplementation(kotlin("test"))
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
