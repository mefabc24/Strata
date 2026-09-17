plugins {
    kotlin("jvm")
    application
}

group = "com.mefabc24.citybuilder"
version = "0.1.0"

dependencies {
    implementation(project(":engine:core"))
    implementation(project(":engine:desktop"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.mefabc24.citybuilder.MainKt")
}

tasks.test {
    useJUnitPlatform()
}