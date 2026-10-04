plugins {
    kotlin("jvm")
    application
}

group = "com.mefabc24.sandbox"
version = "0.1.0"

val atlasTools = configurations.create("atlasTools")

dependencies {
    implementation(project(":engine:core"))
    implementation(project(":engine:desktop"))

    add(atlasTools.name, project(":engine:tools"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.mefabc24.sandbox.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

val terrainAtlasSource = layout.projectDirectory.dir("src/main/assets-src/tiles")
val atlasOutput = layout.projectDirectory.dir("src/main/resources/atlas")

tasks.register<JavaExec>("packTextures") {
    group = "assets"
    description = "Packs Sandbox terrain PNG sources with Strata atlas tools."
    classpath = atlasTools
    mainClass.set("com.mefabc24.strata.tools.atlas.MainKt")
    args(
        terrainAtlasSource.asFile.absolutePath,
        atlasOutput.asFile.absolutePath,
        "tiles"
    )
}

tasks.register<JavaExec>("cleanPackedTextures") {
    group = "assets"
    description = "Removes only generated Sandbox terrain atlas files."
    classpath = atlasTools
    mainClass.set("com.mefabc24.strata.tools.atlas.CleanMainKt")
    args(atlasOutput.asFile.absolutePath, "tiles")
}
