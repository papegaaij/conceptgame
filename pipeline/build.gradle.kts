// Build-time asset tools. Kept apart because gdx-tools pulls in the old LWJGL 2 backend.
plugins {
    id("vanguard.java-conventions")
}

dependencies {
    implementation(libs.gdx.tools)
    implementation(project(":content"))
}

val design = rootProject.layout.projectDirectory.dir("design")
val assets = rootProject.layout.projectDirectory.dir("assets")

/**
 * Lists every spoken line with its key and settings for the offline voice renderer
 * (tools/art/voice.py, design/audio/voice) in build/voice/lines.json.
 */
tasks.register<JavaExec>("voiceLines") {
    description = "Writes the spoken lines and their keys for tools/art/voice.py."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.pipeline.VoiceLineList"
    args(layout.buildDirectory.file("voice/lines.json").get().asFile.absolutePath)
}

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
        "enemy-leviathan-cry-r16-a.ogg",
        "enemy-mortar-lob-r21-a.ogg",
        "enemy-mortar-impact-r21-a.ogg",
        "hazard-sled-whine-r21-a.ogg",
        "hazard-sled-pass-r21-b.ogg",
        "enemy-mantis-telegraph-r23-a.ogg",
        "enemy-mantis-sweep-r23-b.ogg",
        "hazard-flare-launch-r23-a.ogg",
        "hazard-flare-burn-r23-a.ogg",
        "enemy-coilwyrm-regrow-r23-b.ogg",
        "enemy-coilwyrm-burst-r24-b.ogg",
        "enemy-coilwyrm-head-burst-r24-c.ogg",
        "enemy-carrier-roar-r25-b.ogg",
        "enemy-carrier-sac-open-r25-b.ogg",
        "enemy-carrier-sac-close-r25-b.ogg",
        "enemy-carrier-launch-r25-b.ogg",
        "enemy-carrier-sac-burst-r25-a.ogg",
        "enemy-carrier-iris-r25-b.ogg",
        "secret-cable-snap-r25-a.ogg",
        // M4 part H's SFX pass: the synthesized pickups and hangar sounds.
        "pickup-r01-a.ogg",
        "pickup-r01-b.ogg",
        "pickup-r01-c.ogg",
        "pickup-special-charge-r08-a.ogg",
        "ui-shop-buy-r08-a.ogg",
        "ui-shop-sell-r08-a.ogg",
        "ui-shop-denied-r08-a.ogg",
        "ui-equip-r08-a.ogg",
        "ui-upgrade-r08-a.ogg",
        // Concept round 27's choices: the save sound (synthesized) and the Coilwyrm's chain-cut
        // tear (its final file from tools/art/sfx_originals.py).
        "ui-save-r27-b.ogg",
        "enemy-coilwyrm-cut-r27-a.ogg",
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
        "miniboss-sting-r08-a.ogg=miniboss-sting.ogg",
        "mission-complete-r08-a.ogg=mission-complete.ogg",
        "boss-warning-r08-a.ogg=boss-warning.ogg",
        "choir-descends-full-r08-a.ogg=choir-descends.ogg",
        "act-complete-r08-a.ogg=act-complete.ogg",
    )
}

/**
 * Writes the credits roll (design/ui/credits) from CREDITS.md into assets/ui/credits.txt: the
 * attributions of every shipped CC-BY asset and font, the CC0 authors' thanks. Run it after
 * CREDITS.md or the shipped assets change and commit the result; CreditsListTest fails while the
 * committed roll differs from what CREDITS.md gives.
 */
val credits = tasks.register<JavaExec>("credits") {
    description = "Writes the credits roll from CREDITS.md into assets/ui/credits.txt."
    group = "assets"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "vanguard.pipeline.CreditsList"
    mustRunAfter(cutPlaceholderSprites, copyPlaceholderSounds, copyPlaceholderMusic)
    args(rootProject.projectDir.absolutePath, assets.file("ui/credits.txt").asFile.absolutePath)
}

tasks.test {
    // CreditsListTest compares the committed roll with CREDITS.md and the shipped assets.
    systemProperty("vanguard.rootDir", rootProject.projectDir.absolutePath)
    inputs.file(rootProject.layout.projectDirectory.file("CREDITS.md")).withPropertyName("credits")
    inputs.file(assets.file("ui/credits.txt")).withPropertyName("creditsRoll")
    inputs.files(assets.dir("sfx"), assets.dir("music"), assets.dir("voice"), assets.dir("fonts"))
        .withPropertyName("creditedAssets")
}

/**
 * Copies the chosen concept art and audio that stand in for production assets into assets/ and
 * cuts the sprite frames. Run it after a concept choice changes; its output is committed (Git
 * LFS), the build only reads assets/. The title scene, the logo and the bitmap fonts are production
 * art (tools/art/ui_scenes.py, tools/art/fonts.py), the act titles' lettering is rendered by
 * tools/concept/ui_assets.py. It rewrites the credits roll last.
 */
tasks.register("importPlaceholders") {
    description = "Copies chosen concept art and audio into assets/ as placeholders."
    group = "assets"
    dependsOn(cutPlaceholderSprites, copyPlaceholderSounds, copyPlaceholderMusic, credits)
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
