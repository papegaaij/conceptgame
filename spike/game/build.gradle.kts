// The libGDX presentation layer: rendering, audio and input around the simulation.
plugins {
    id("spike.java-conventions")
    `java-library`
}

dependencies {
    api(project(":sim"))
    api(libs.gdx.core)
    // The desktop backend brings LWJGL's OpenAL and stb_vorbis, used by the music streamer.
    implementation(libs.gdx.backend.lwjgl3)
    implementation(libs.gdx.controllers.core)
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

tasks.test {
    // The music test decodes the real round-08 track from the design tree.
    systemProperty("spike.designDir", rootProject.layout.projectDirectory.dir("../design").asFile.absolutePath)
}
