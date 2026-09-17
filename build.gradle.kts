plugins {
    kotlin("jvm") version "2.4.0"
}

group = "com.mefabc24.strata"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val gdxVersion = "1.14.2"

    testImplementation(kotlin("test"))

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}