// Build-time asset generators. Kept apart because gdx-tools pulls in the old LWJGL 2 backend.
plugins {
    id("spike.java-conventions")
}

dependencies {
    implementation(libs.gdx.tools)
}

val haloSource = rootProject.layout.projectDirectory.file("../design/enemies/ground/concept/halo-platform-r07-a.png")
val haloAtlasDir = layout.buildDirectory.dir("generated/atlas")

/** Renders the 768-angle Halo Platform set from the round-07 sheet and packs it into atlases. */
val generateHaloAtlas = tasks.register<JavaExec>("generateHaloAtlas") {
    description = "Generates the 768-frame Halo Platform angle atlas."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.tools.AngleSetGenerator"
    jvmArgs("-Djava.awt.headless=true")
    inputs.file(haloSource)
    outputs.dir(haloAtlasDir)
    args(haloSource.asFile.absolutePath, haloAtlasDir.get().asFile.absolutePath)
}

configurations.consumable("atlases")
artifacts.add("atlases", haloAtlasDir) { builtBy(generateHaloAtlas) }
