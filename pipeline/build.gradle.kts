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
 * Copies the chosen concept sound effects the game plays into assets/sfx; they keep their concept
 * names, so CREDITS.md rows match at a glance. Final sounds (tools/art/sfx_originals.py, with a
 * SOURCE comment) are left alone.
 */
val copyPlaceholderSounds = tasks.register<JavaExec>("copyPlaceholderSounds") {
    description = "Copies chosen concept sound effects into assets/sfx, except final ones."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.pipeline.PlaceholderSounds"
    args(
        design.dir("audio/sfx/concept").asFile.absolutePath,
        assets.dir("sfx").asFile.absolutePath,
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
        "ui-menu-move-r08-a.ogg",
        "ui-menu-confirm-r08-a.ogg",
        "ui-menu-back-r08-a.ogg",
        "ambience-orbit-r08-a.ogg",
        "launch-rail-r11-b.ogg",
        "ui-edge-warning-r11-b.ogg",
    )
}

/**
 * Copies the chosen concept music the game plays into assets/music under the names it loads. Final
 * themes (tools/art/themes.py, with a SOURCE comment) are left alone.
 */
val copyPlaceholderMusic = tasks.register<JavaExec>("copyPlaceholderMusic") {
    description = "Copies chosen concept music into assets/music, except final ones."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.pipeline.PlaceholderSounds"
    args(
        design.dir("audio/music/concept").asFile.absolutePath,
        assets.dir("music").asFile.absolutePath,
        "title-theme-full-r08-a.ogg=title-theme.ogg",
        "coalition-rising-full-r08-a.ogg=coalition-rising.ogg",
        "coalition-rising-base-r11-a.ogg=coalition-rising-base.ogg",
        "afterburner-full-r08-a.ogg=afterburner.ogg",
        "afterburner-base-r15-a.ogg=afterburner-base.ogg",
        "briefing-theme-r08-a.ogg=briefing-theme.ogg",
        "hangar-theme-full-r08-a.ogg=hangar-theme.ogg",
        "game-over-r08-a.ogg=game-over.ogg",
        "mission-failed-r08-a.ogg=mission-failed.ogg",
        "mission-complete-r08-a.ogg=mission-complete.ogg",
    )
}

/**
 * Copies the chosen concept art and audio that stand in for production assets into assets/ and
 * cuts the sprite frames. Run it after a concept choice changes; its output is committed (Git
 * LFS), the build only reads assets/. The title scene, the logo and the bitmap fonts are production
 * art (tools/art/ui_scenes.py, tools/art/fonts.py), the act titles' lettering is rendered by
 * tools/concept/ui_assets.py.
 */
tasks.register("importPlaceholders") {
    description = "Copies chosen concept art and audio into assets/ as placeholders."
    group = "assets"
    dependsOn(cutPlaceholderSprites, copyPlaceholderSounds, copyPlaceholderMusic)
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
