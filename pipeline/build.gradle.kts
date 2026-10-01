// Build-time asset tools. Kept apart because gdx-tools pulls in the old LWJGL 2 backend.
plugins {
    id("vanguard.java-conventions")
}

dependencies {
    implementation(libs.gdx.tools)
}

val design = rootProject.layout.projectDirectory.dir("design")

/**
 * Copies the chosen concept art that stands in for production assets into assets/. Run it after a
 * concept choice changes; its output is committed (Git LFS), the build only reads assets/.
 */
tasks.register<Copy>("importPlaceholders") {
    description = "Copies chosen concept art into assets/ as placeholders."
    group = "assets"
    into(rootProject.layout.projectDirectory.dir("assets"))
    from(design.file("ui/main-menu/concept/logo-r01-d.png")) {
        into("ui")
        rename { "logo.png" }
    }
    from(design.file("audio/music/concept/title-theme-full-r08-a.ogg")) {
        into("music")
        rename { "title-theme.ogg" }
    }
}
