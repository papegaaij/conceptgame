// The libGDX presentation layer: screens, rendering, audio and input.
plugins {
    id("vanguard.java-conventions")
    `java-library`
}

dependencies {
    api(project(":content"))
    api(libs.gdx.core)
    // The desktop backend brings GLFW (window placement), OpenAL and stb_vorbis (music streaming).
    implementation(libs.gdx.backend.lwjgl3)
    implementation(libs.gdx.controllers.core)
    testImplementation(libs.junit.jupiter.params)
}

tasks.test {
    // The music test decodes the real title theme from the assets.
    systemProperty("vanguard.assetsDir", rootProject.layout.projectDirectory.dir("assets").asFile.absolutePath)
    // BackdropAssetsTest checks the backdrop images against the levels' data files.
    inputs.dir(rootProject.layout.projectDirectory.dir("assets/backdrop")).withPropertyName("backdropAssets")
    // MissionLayoutTest measures the HUD's texts with the UI kit's fonts.
    inputs.dir(rootProject.layout.projectDirectory.dir("assets/fonts")).withPropertyName("fonts")
}
