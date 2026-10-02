// Build-time asset tools. Kept apart because gdx-tools pulls in the old LWJGL 2 backend.
plugins {
    id("vanguard.java-conventions")
}

dependencies {
    implementation(libs.gdx.tools)
}

val design = rootProject.layout.projectDirectory.dir("design")
val assets = rootProject.layout.projectDirectory.dir("assets")

/** Cuts the placeholder sprites out of the chosen concept sheets (crop rectangles in PlaceholderSprites). */
val cutPlaceholderSprites = tasks.register<JavaExec>("cutPlaceholderSprites") {
    description = "Cuts placeholder sprite frames from chosen concept sheets into assets/."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.pipeline.PlaceholderSprites"
    args(design.asFile.absolutePath, assets.asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

/**
 * Copies the chosen concept art and audio that stand in for production assets into assets/ and
 * cuts the sprite frames. Run it after a concept choice changes; its output is committed (Git
 * LFS), the build only reads assets/.
 */
tasks.register<Copy>("importPlaceholders") {
    description = "Copies chosen concept art and audio into assets/ as placeholders."
    group = "assets"
    dependsOn(cutPlaceholderSprites)
    into(assets)
    from(design.file("ui/main-menu/concept/logo-r01-d.png")) {
        into("ui")
        rename { "logo.png" }
    }
    from(design.dir("audio/music/concept")) {
        into("music")
        include(
            "title-theme-full-r08-a.ogg",
            "coalition-rising-full-r08-a.ogg",
            "mission-failed-r08-a.ogg",
            "mission-complete-r08-a.ogg",
        )
        rename("title-theme-full-r08-a.ogg", "title-theme.ogg")
        rename("coalition-rising-full-r08-a.ogg", "coalition-rising.ogg")
        rename("mission-failed-r08-a.ogg", "mission-failed.ogg")
        rename("mission-complete-r08-a.ogg", "mission-complete.ogg")
    }
    // The sound effects keep their concept names, so CREDITS.md rows match at a glance.
    from(design.dir("audio/sfx/concept")) {
        into("sfx")
        include(
            "player-shot-r02-a.ogg",
            "hit-organic-r08-a.ogg",
            "hit-organic-r08-b.ogg",
            "explosion-tiny-r03-a.ogg",
            "explosion-tiny-r03-b.ogg",
            "player-shield-hit-r08-a.ogg",
            "player-shield-break-r08-a.ogg",
            "player-armour-hit-r08-a.ogg",
            "player-destroyed-r08-a.ogg",
            "enemy-shot-small-r08-a.ogg",
            "enemy-shot-small-r08-b.ogg",
            "explosion-r02-a.ogg",
            "explosion-small-r03-a.ogg",
            "hit-metal-r08-a.ogg",
            "hit-metal-r08-b.ogg",
            "pickup-salvage-small-r08-a.ogg",
            "pickup-salvage-large-r08-a.ogg",
            "pickup-shield-cell-r08-a.ogg",
            "pickup-armour-patch-r08-a.ogg",
            "ui-radio-open-r08-a.ogg",
            "ui-radio-close-r08-a.ogg",
            "ui-typewriter-r08-a.ogg",
            "ui-tally-tick-r08-a.ogg",
            "ui-tally-total-r08-a.ogg",
            "ui-grade-stamp-r08-a.ogg",
            "ambience-orbit-r08-a.ogg",
        )
    }
}

/** Packs assets/sprites and assets/backdrop into texture atlases; build output, never committed. */
val packAtlases = tasks.register<JavaExec>("packAtlases") {
    description = "Packs the sprite frames in assets/ into texture atlases."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.pipeline.AtlasPacker"
    val output = layout.buildDirectory.dir("atlases")
    inputs.dir(assets.dir("sprites"))
    inputs.dir(assets.dir("backdrop"))
    outputs.dir(output)
    args(assets.asFile.absolutePath, output.get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

/** The packed atlases, for the desktop build's resources. */
val atlases by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(atlases.name, layout.buildDirectory.dir("atlases")) {
        builtBy(packAtlases)
    }
}
