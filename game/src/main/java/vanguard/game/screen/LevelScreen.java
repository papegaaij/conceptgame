package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.Content;
import vanguard.content.Difficulty;
import vanguard.content.DifficultyData;
import vanguard.content.Expression;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.content.SpecialsData;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.Flight;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.DelayedSounds;
import vanguard.game.audio.FlightSounds;
import vanguard.game.audio.LevelMusic;
import vanguard.game.audio.Sfx;
import vanguard.game.audio.Tracks;
import vanguard.game.audio.Voices;
import vanguard.game.input.Action;
import vanguard.game.input.FlightCommands;
import vanguard.game.level.Barks;
import vanguard.game.level.ControlPrompts;
import vanguard.game.level.Outro;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.game.level.RadioSchedule;
import vanguard.game.render.BossBanner;
import vanguard.game.render.CreditNumbers;
import vanguard.game.render.EdgeWarnings;
import vanguard.game.render.Effects;
import vanguard.game.render.EnemyLooks;
import vanguard.game.render.Hud;
import vanguard.game.render.LevelRenderer;
import vanguard.game.render.PodPivots;
import vanguard.game.render.ScreenFlash;
import vanguard.game.render.SecretGlints;
import vanguard.game.render.SetPieceDeath;
import vanguard.game.render.SetPieceWrecks;
import vanguard.game.render.TargetingOverlay;
import vanguard.game.render.ThreatArrows;
import vanguard.game.render.TriggerBreak;
import vanguard.game.render.WaveBanners;
import vanguard.game.render.WeaponLooks;
import vanguard.game.settings.Settings;
import vanguard.sim.FixedStepClock;
import vanguard.sim.Layer;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;
import vanguard.sim.PlayField;
import vanguard.sim.Rules;
import vanguard.sim.SetPiece;
import vanguard.sim.Shot;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.Wingman;
import vanguard.sim.WingmanSpec;

/**
 * Flying a level of the campaign: runs the simulation at its fixed step from the campaign's
 * level-start state, turns its events into sound, effects, radio chatter and HUD feedback, plays
 * the level's music cues and draws it interpolated. When the level is won it shows the radio's
 * last messages ({@link Outro}), banks the result in the campaign and shows the debrief. When the ship is destroyed the
 * death plays out, then the mission failed screen opens over the level, or the game over screen
 * follows when no retry is left (design/systems/retry); the failure's used retry is autosaved at
 * once, and so is a game over's return to the hangar before the level. A failed primary objective (the
 * convoy lost) fails the level the same way without the explosion and the slow motion, and the
 * mission failed screen shows the level's own failure line. The sortie flies what {@link Flight} maps of the fitted loadout; the HUD lists the fitted
 * items that fly from M4 on. Pause (Esc / P / Start), the window losing
 * the focus and a gamepad disconnecting open the pause menu over it. The Gameplay tab's text speed
 * and flash reduction and the Controls tab's auto-fire apply from the next frame on.
 */
public final class LevelScreen implements GameScreen {
    /** Every run of a level gets the same waves; the seed only picks hover times. */
    private static final long SEED = 2185;

    private static final int MAX_STEPS_PER_FRAME = 8;
    /** The sensor suite's level from which it shows the threat arrows (design/player/systems). */
    private static final int THREAT_ARROW_SENSOR = 2;
    /** Death plays in slow motion for a second (design/systems/retry). */
    private static final float SLOW_MOTION_SECONDS = 1;

    private static final float SLOW_MOTION_RATE = 0.5f;
    /** From the ship's destruction to the mission failed screen: the explosion, the slow motion and the sting's start. */
    static final float FAILED_SCREEN_SECONDS = 3;
    /** The ship's blue shimmer fades over this many steps after a shield hit. */
    private static final int SHIMMER_TICKS = 8;

    private static final int TINY_EXPLOSION_FRAME_TICKS = 2;
    private static final int LARGE_EXPLOSION_FRAME_TICKS = 4;
    private static final int IMPACT_FRAME_TICKS = 2;
    /** A missile's smoke trail: a puff every this many steps, each frame of it shown this long. */
    private static final int TRAIL_TICKS = 4;

    private static final int TRAIL_FRAME_TICKS = 2;
    /** A destroyed ground target's 8 debris frames last 0.4 s. */
    private static final int DEBRIS_FRAME_TICKS = 3;
    /** A ground unit's remains stay on the ground until they scroll off (at most 10 s). */
    private static final int REMAINS_TICKS = SimStep.ticks(10);

    private static final float RADIO_VOLUME = 0.5f;
    private static final float TYPING_VOLUME = 0.15f;

    private final GameServices services;
    private final Campaign campaign;
    private final LevelData level;
    /** The level's number: its unit atlas stays loaded while the screen is open. */
    private final int levelNumber;

    private final Sortie sortie;
    private final FixedStepClock clock = new FixedStepClock(SimStep.SECONDS, MAX_STEPS_PER_FRAME);
    private final EnemyLooks[] looks;
    private final WeaponLooks weaponLooks;
    /** The layers on which an enemy was destroyed in this attempt: a prompt that skips on one of them has left. */
    private final EnumSet<Layer> layersHit = EnumSet.noneOf(Layer.class);
    /** A set piece's death: a medium burst at each part, one after the other, then a large one at its centre. */
    private static final int CHAIN_STEP_TICKS = 6;

    private static final int MEDIUM_EXPLOSION_FRAME_TICKS = 3;
    /** A set piece's death cloud (the Leviathan's ichor) shows each frame for 6 steps. */
    private static final int DEATH_CLOUD_FRAME_TICKS = 6;
    /** A boss's crown petals tearing off: 10 fps. */
    private static final int PETAL_FRAME_TICKS = 6;

    private final Array<AtlasRegion> explosionMedium;
    /** Each set piece's death cloud, its slug's {@code -ichor} frames, by index in the script; empty for none. */
    private final List<Array<AtlasRegion>> deathClouds;
    /** Each ground object's break-apart, its look's {@code -break} frames, by index in the script. */
    private final List<Array<AtlasRegion>> groundBreaks;
    /**
     * Each ground object's wreck, left where it was destroyed: its look's wrecked frame (the third),
     * by index in the script; empty for a look without one (the cargo container).
     */
    private final List<Array<AtlasRegion>> groundWrecks;
    /** The spark of a shot glancing off a hardened target. */
    private final Array<AtlasRegion> glance;

    private final FlightSounds sounds;
    private final LevelRenderer renderer;
    private final Hud hud;
    private final Effects effects = Effects.glowing();
    private final Effects debris = Effects.solid();
    /** The solid death pieces of units: a flyer's at its play-field position, a ground unit's on the ground. */
    private final Effects pieces = Effects.solid();
    /** The Airstrike's blasts on the ground, so they stay where the bombs landed. */
    private final Effects blasts = Effects.glowing();
    /** The set pieces breaking up at their death. */
    private final SetPieceWrecks wrecks;

    private final CreditNumbers creditNumbers = new CreditNumbers();
    private final EdgeWarnings warnings;
    /** An act boss's warning banner, its death's screen flash and the chain's bursts' sounds. */
    private final BossBanner banner;
    /** The wave warning banner of a side or rear wave's edge warning (design/ui/hud). */
    private final WaveBanners waveBanners;
    /** The sensor suite's threat arrows, with a sensor suite at L2+ (design/player/systems); null without. */
    private final ThreatArrows threatArrows;

    private final ScreenFlash screenFlash = new ScreenFlash();
    private final DelayedSounds chainSounds;
    /** The steps from a boss's death to its credit shower: the end of its chained bursts. */
    private int showerDelay = SHOWER_DELAY_TICKS;

    private final RadioQueue radio = new RadioQueue();
    private final RadioSchedule radioSchedule;
    /** Rook's radio barks while he flies in the level (M5 part A); null without him. */
    private final Barks barks;

    private final ControlPrompts prompts;
    private final PromptTexts promptTexts;
    private final LevelMusic music;
    private final String name;
    private final Flight flight;
    private float slowMotion;
    private final Outro outro = new Outro();
    /** What the ship's destruction leads to, and how long until its screen opens. */
    private Optional<Campaign.Failure> failure = Optional.empty();
    /** The level's line for its failed primary objective, once it failed in this attempt. */
    private Optional<LevelScript.RadioCue> failureLine = Optional.empty();

    private float failedIn;
    private boolean launchPending = true;
    private int shimmer;
    private int typed;

    /**
     * The campaign's next level, flown from the campaign's level-start state.
     *
     * @param levelKey the level, as in {@link Content#level(String)}
     */
    public LevelScreen(GameServices services, Campaign campaign, String levelKey) {
        this.services = services;
        this.campaign = campaign;
        Difficulty difficulty = campaign.difficulty();
        level = services.content.level(levelKey);
        levelNumber = Content.levelNumber(levelKey);
        // The level's own sprites (its unit atlas) until the screen closes.
        services.sprites.enterLevel(levelNumber);
        Rules rules = SimSpecs.rules(services.content, levelKey, difficulty);
        campaign.launch();
        flight = Flight.of(services.content, services.catalogue, campaign);
        sortie = new Sortie(
                SEED,
                flight.loadout(),
                SimSpecs.level(services.content, levelKey, difficulty),
                services.invulnerable ? rules.withInvulnerableShip() : rules,
                campaign.armour());
        radioSchedule = new RadioSchedule(sortie.script(), sortie.special().fitted());
        barks = sortie.wingman().isPresent()
                ? Barks.of(
                        services.content.wingmen().barks(),
                        sortie.script(),
                        sortie.special().fitted(),
                        radio,
                        this::bark)
                : null;
        wrecks = new SetPieceWrecks(sortie.setPieceCount());
        looks = EnemyLooks.of(sortie.enemyKinds(), services.sprites, services.content);
        weaponLooks = new WeaponLooks(
                sortie.armament(),
                flight.weapons().stream().mapToInt(Flight.Weapon::level).toArray(),
                services.sprites,
                new PodPivots(services.files),
                sortie.wingman().map(Wingman::gun));
        glance = services.sprites.frames("ballistic-impact");
        explosionMedium = services.sprites.frames("explosion-medium");
        deathClouds = sortie.script().setPieces().stream()
                .map(spec -> services.sprites.has(spec.slug() + "-ichor")
                        ? services.sprites.frames(spec.slug() + "-ichor")
                        : new Array<AtlasRegion>())
                .toList();
        // A trigger is never destroyed (it is spent and stays, a billboard's toppled frame its own
        // wreck): no break-apart, no wreck.
        groundBreaks = sortie.script().groundObjects().stream()
                .map(spec ->
                        spec.trigger() ? new Array<AtlasRegion>() : services.sprites.frames(spec.look() + "-break"))
                .toList();
        groundWrecks = sortie.script().groundObjects().stream()
                .map(spec -> spec.trigger() ? new Array<AtlasRegion>() : services.sprites.frames(spec.look()))
                .map(frames -> frames.size > 2 ? Array.with(frames.get(2)) : new Array<AtlasRegion>())
                .toList();
        sounds = new FlightSounds(
                services.sfx,
                looks,
                sortie.armament(),
                sortie.script().setPieces().stream()
                        .map(LevelScript.SetPieceSpec::slug)
                        .toList(),
                sortie.wingman().map(Wingman::gun));
        sounds.flareSeconds(sortie.script()
                .darkness()
                .map(LevelScript.Darkness::flareSeconds)
                .orElse(0.0));
        renderer = new LevelRenderer(
                services.sprites,
                looks,
                weaponLooks,
                services.files,
                services.flash,
                services.fonts.body,
                level,
                sortie.script(),
                levelKey);
        warnings = new EdgeWarnings(services.sprites.pixel, services.fonts.body);
        banner = new BossBanner(services.sprites.pixel, services.fonts.heading, services.fonts.body);
        waveBanners = new WaveBanners(services.sprites.pixel, services.fonts.body);
        threatArrows = flight.sensor() >= THREAT_ARROW_SENSOR ? new ThreatArrows() : null;
        renderer.modules(
                flight.targeting()
                        ? new TargetingOverlay(
                                services.sprites.pixel,
                                services.content,
                                sortie.enemyKinds(),
                                SimSpecs.targeting(services.content))
                        : null,
                flight.salvage() > 0 ? new SecretGlints(services.sprites) : null);
        chainSounds = new DelayedSounds(services.sfx);
        name = Content.levelName(levelKey);
        hud = new Hud(
                services.sprites,
                services.fonts,
                services.transmissionStatic,
                sortie.script().number(),
                name,
                campaign.credits(),
                flight,
                SimSpecs.regenBonus(services.content, flight.sparePower()),
                services.content.player().pickups().overdrive().seconds());
        prompts = new ControlPrompts(level.controlPrompts());
        promptTexts = new PromptTexts(services.input.bindings());
        // The level's theme (design/audio/music, track list) as its two stems, over its setting's ambience.
        String theme = theme(level.music().track());
        var full = services.files.internal(Tracks.path(theme));
        var base = services.files.internal(Tracks.basePath(theme));
        music = new LevelMusic(
                services.audio,
                services.mixer,
                // A theme without stems plays its full mix as both (the intensity layer always on).
                base.exists() ? base : full,
                full,
                services.sfx,
                Sfx.ambience(level.music().ambience()),
                level.music().startSection(),
                level.music().startDb().orElse(0.0),
                level.music()::full,
                level.music().ambienceFrom().orElse(Double.POSITIVE_INFINITY),
                level.music().voiceLoop().flatMap(loop -> voiceLoop(level, loop)),
                bossMusic(level.music())
                        .map(names -> new LevelMusic.Boss(
                                names.warning().map(name -> services.files.internal(Tracks.path(name))),
                                names.track().map(name -> services.files.internal(Tracks.path(name))),
                                Tracks.BOSS_WARNING_BARS_SECONDS)));
    }

    /** A level's act boss music by file name: the warning (track 22) and the boss track (track 18). */
    record BossMusic(Optional<String> warning, Optional<String> track) {}

    /**
     * The level's {@code music.boss_warning} and {@code boss_track} (design/tech/architecture, the
     * level music block); empty without either.
     */
    static Optional<BossMusic> bossMusic(LevelData.Music music) {
        Optional<String> warning = music.bossWarning();
        Optional<String> track = music.bossTrack();
        return warning.isEmpty() && track.isEmpty() ? Optional.empty() : Optional.of(new BossMusic(warning, track));
    }

    /** A music block's voice loop: its speaker's first timed radio line, if it has a voice file. */
    private Optional<LevelMusic.VoiceLoop> voiceLoop(LevelData level, LevelData.Music.VoiceLoop loop) {
        return level.radio().stream()
                .filter(cue -> cue.t().isPresent() && cue.speaker().equals(loop.speaker()))
                .findFirst()
                .flatMap(cue -> services.voices.radio(
                        cue.speaker(),
                        cue.line(),
                        cue.expression().orElse(Expression.NEUTRAL).slug()))
                .map(voice -> new LevelMusic.VoiceLoop(
                        services.files.internal(voice.path()), loop.section(), (float) Math.pow(10, loop.db() / 20)));
    }

    /** The file name of a level theme by its track number (design/audio/music, track list). */
    static String theme(int track) {
        return Tracks.name(track)
                .orElseThrow(() -> new IllegalArgumentException("no music file for track " + track + " yet"));
    }

    /**
     * Rook's side for a radio line's {@code {side}} (M5 part B): his flight's side setting (the
     * save's, or {@code --escort}'s {@code side=}), left when he does not fly.
     */
    private WingmanSpec.Side escortSide() {
        return sortie.wingman().map(wingman -> wingman.spec().side()).orElse(WingmanSpec.Side.LEFT);
    }

    /** "MISSION 01 - BREAK AT DAWN", for the pause menu. */
    String mission() {
        return String.format(Locale.ROOT, "MISSION %02d - %s", sortie.script().number(), name)
                .toUpperCase(Locale.ROOT);
    }

    Campaign campaign() {
        return campaign;
    }

    /** The time flown in this attempt. */
    double seconds() {
        return sortie.levelSeconds();
    }

    long score() {
        return sortie.score();
    }

    /** What the attempt has earned so far, for the mission failed screen. */
    int attemptCredits() {
        return sortie.credits();
    }

    /** The level's own line for the mission failed screen, when its primary objective failed (design/systems/retry). */
    /** The failure line with the lost group's name in it ("Battery C is behind you"), when a group failed the level. */
    private LevelScript.RadioCue failedGroupLine(LevelScript.RadioCue cue) {
        int group = sortie.failedGroup();
        if (group < 0) {
            return cue;
        }
        return new LevelScript.RadioCue(
                cue.trigger(),
                cue.t(),
                cue.subject(),
                cue.speaker(),
                vanguard.content.voice.VoiceLines.groupLine(
                        cue.line(), sortie.script().groups().get(group)),
                cue.distorted(),
                cue.expression(),
                cue.portrait(),
                cue.requiresSpecial(),
                cue.alliesMin(),
                cue.alliesMax());
    }

    Optional<LevelScript.RadioCue> failureLine() {
        return failureLine;
    }

    /**
     * Whether the mission failed screen offers Retry from boss (design/systems/retry): the attempt
     * reached the boss checkpoint, on a difficulty with boss checkpoints ({@code boss_checkpoint} of
     * design/systems/difficulty: easy and medium).
     */
    boolean bossCheckpoint() {
        return checkpointOffered(services.content.difficulty(), campaign.difficulty(), sortie.bossCheckpoint());
    }

    /** Whether Retry from boss is offered: the checkpoint was reached and the difficulty has them. */
    static boolean checkpointOffered(DifficultyData data, Difficulty difficulty, boolean reached) {
        return reached && data.bossCheckpoint().of(difficulty);
    }

    /** Restarts at the boss checkpoint: the boss's arrival on an empty field, with the defences and tallies of then. */
    void retryFromBoss() {
        sortie.retryFromBoss();
        outro.stop();
        slowMotion = 0;
        failure = Optional.empty();
        failureLine = Optional.empty();
    }

    /** Starts the level over from its start state with {@code armour} (a retry, or the pause menu's restart). */
    void retry(double armour) {
        // The flight's Rook: the gear's, or the --escort debug option's (never the gear's).
        double rook = campaign.escortFlight().map(Campaign.EscortFlight::armour).orElse(0.0);
        if (sortie.wingman().isPresent() && rook > 0) {
            // Rook starts over with his level-start armour, raised to the floor like the player's.
            sortie.retry(armour, rook);
        } else {
            sortie.retry(armour);
        }
        outro.stop();
        slowMotion = 0;
        failure = Optional.empty();
        failureLine = Optional.empty();
    }

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        if (services.input.pressed(Action.PAUSE) || services.input.interrupted()) {
            services.voices.pause();
            return Transition.open(new PauseScreen(services, this));
        }
        if (outro.update(seconds, radio)) {
            return Transition.replace(debrief());
        }
        if (failure.isPresent()) {
            failedIn -= seconds;
            if (failedIn <= 0) {
                Campaign.Failure what = failure.get();
                failure = Optional.empty();
                services.voices.stop();
                return what == Campaign.Failure.GAME_OVER
                        ? Transition.replace(new GameOverScreen(services, campaign, mission()))
                        : Transition.open(new MissionFailedScreen(services, this));
            }
        }
        Settings settings = services.settings();
        radio.charsPerSecond(settings.gameplay().textSpeed());
        creditNumbers.visible(settings.gameplay().creditNumbers());
        float simSeconds = seconds;
        if (slowMotion > 0) {
            slowMotion -= seconds;
            simSeconds *= SLOW_MOTION_RATE;
        }
        int steps = clock.advance(simSeconds);
        int stepCommands = FlightCommands.of(services.input, settings.controls());
        for (int i = 0; i < steps; i++) {
            sortie.step(stepCommands);
            if (!sortie.launching() && sortie.section() == 1) {
                prompts.update(stepCommands);
            }
            effects.step();
            debris.step();
            pieces.step();
            blasts.step();
            wrecks.step();
            sounds.step();
            creditNumbers.step();
            screenFlash.step();
            chainSounds.step();
            if (shimmer > 0) {
                shimmer--;
            }
            react(sortie.events());
            if (barks != null) {
                barks.step(SimStep.ticks(sortie.levelSeconds()), armourShare(), overdrivePickups());
            }
            smokeTrails();
            sounds.watch(sortie);
            int started = warnings.step(sortie.edgeWarnings(), sortie.tick());
            sounds.edgeWarnings(started);
            waveBanners.step(sortie.edgeWarnings(), started, sortie.tick());
        }
        if (launchPending && sortie.launching()) {
            sounds.launch();
            launchPending = false;
        }
        services.voices.resume();
        playRadio(seconds);
        services.voices.update(seconds);
        music.update(
                sortie.section(),
                sortie.levelSeconds(),
                seconds,
                radio.current().isPresent() || services.voices.playing());
        return Transition.STAY;
    }

    /** The ship's armour as a share of its most, for Rook's bark about it. */
    private double armourShare() {
        var defences = sortie.ship().defences();
        return defences.maxArmour() > 0 ? defences.armour() / defences.maxArmour() : 1;
    }

    /** The overdrive pickups on the field, for Rook's bark when one appears. */
    private int overdrivePickups() {
        int count = 0;
        for (int i = 0; i < sortie.pickupCount(); i++) {
            if (sortie.pickup(i).type() == PickupType.OVERDRIVE) {
                count++;
            }
        }
        return count;
    }

    /** Queues one of Rook's barks (an event line; his eject bark urgent) with its voice file, if it has one. */
    private RadioQueue.Message bark(Barks.Line line) {
        return queue(line.speaker(), line.speaker(), line.expression(), line.text(), false, line.priority());
    }

    /** Every few steps a missile with a smoke trail (the Hornet) leaves a puff where it is. */
    private void smokeTrails() {
        if (sortie.tick() % TRAIL_TICKS != 0) {
            return;
        }
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            Array<AtlasRegion> trail = weaponLooks.trail(shot.mount());
            if (trail != null) {
                pieces.start(trail, TRAIL_FRAME_TICKS, shot.renderX(1), shot.renderY(1));
            }
        }
    }

    private void react(SimEvents events) {
        sounds.play(events);
        for (int i = 0; i < events.size(); i++) {
            double x = events.x(i);
            double y = events.y(i);
            switch (events.type(i)) {
                case ENEMY_HIT -> effects.start(weaponLooks.impact(events.value(i)), IMPACT_FRAME_TICKS, x, y);
                case GROUND_HIT ->
                    effects.startOnGround(
                            weaponLooks.impact(events.value(i)), IMPACT_FRAME_TICKS, x, y, 0, sortie.groundScroll());
                case SHOT_GLANCED -> effects.start(glance, IMPACT_FRAME_TICKS, x, y);
                case BLAST -> {
                    // A bomb's or shell's burst is the small explosion; a mine bursts in its own blast.
                    Array<AtlasRegion> blast = weaponLooks.blast(events.value(i), services.sprites.explosionSmall);
                    effects.start(
                            blast,
                            blast == services.sprites.explosionSmall
                                    ? TINY_EXPLOSION_FRAME_TICKS
                                    : MEDIUM_EXPLOSION_FRAME_TICKS,
                            x,
                            y);
                }
                case ENEMY_DESTROYED -> {
                    if (barks != null) {
                        barks.kill(sortie.levelSeconds());
                    }
                    EnemyLooks look = looks[events.value(i)];
                    layersHit.add(sortie.enemyKinds().get(events.value(i)).layer());
                    death(events.value(i), x, y);
                    if (!look.remains().isEmpty()) {
                        debris.startOnGround(
                                look.remains(), REMAINS_TICKS / look.remains().size, x, y, 0, sortie.groundScroll());
                    }
                }
                case BROOD_BURST -> {
                    // A self-burst: the pod's death animation without a kill.
                    death(events.value(i), x, y);
                }
                case WALKER_DOWN -> {
                    // The legless husk at its last heading, left on the ground like a turret's stump.
                    int value = events.value(i);
                    EnemyLooks look = looks[SimEvents.walkerKind(value)];
                    if (!look.husks().isEmpty()) {
                        debris.startOnGround(
                                look.husk(SimEvents.walkerFacing(value)),
                                REMAINS_TICKS,
                                x,
                                y,
                                0,
                                sortie.groundScroll());
                    }
                }
                case CLAMP_HIT -> effects.start(glance, IMPACT_FRAME_TICKS, x, y);
                case DEBRIS_HIT -> effects.start(weaponLooks.impact(events.value(i)), IMPACT_FRAME_TICKS, x, y);
                case DEBRIS_DESTROYED, MINE_BURST, MINE_DESTROYED ->
                    effects.start(services.sprites.explosionTiny, TINY_EXPLOSION_FRAME_TICKS, x, y);
                case PART_DESTROYED -> effects.start(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, x, y);
                case SET_PIECE_DESTROYED -> chainedDeath(events.value(i), deathClouds.get(events.value(i)), x, y);
                case GROUND_DESTROYED -> {
                    int object = events.value(i);
                    if (!groundWrecks.get(object).isEmpty()) {
                        debris.startOnGround(groundWrecks.get(object), REMAINS_TICKS, x, y, 0, sortie.groundScroll());
                    }
                    debris.startOnGround(groundBreaks.get(object), DEBRIS_FRAME_TICKS, x, y, 0, sortie.groundScroll());
                    effects.startOnGround(
                            services.sprites.explosionSmall,
                            TINY_EXPLOSION_FRAME_TICKS,
                            x,
                            y,
                            0,
                            sortie.groundScroll());
                }
                case TRIGGER_SPENT -> {
                    // A trigger whose spent frame is a wreck (Level 08's billboard topples) bursts in a
                    // destructible's small explosion; its spent frame stays as the wreck (TriggerBreak).
                    if (TriggerBreak.breaks(sortie.script().groundObjects().get(events.value(i)))) {
                        effects.startOnGround(
                                services.sprites.explosionSmall,
                                TINY_EXPLOSION_FRAME_TICKS,
                                x,
                                y,
                                0,
                                sortie.groundScroll());
                    }
                }
                case CREDITS_PICKED_UP -> creditNumbers.show(events.value(i), x, y);
                case BOSS_DESTROYED -> {
                    creditShower(events.value(i), x, y);
                    music.bossDown();
                }
                case BOSS_ARRIVED -> {
                    if (barks != null) {
                        barks.bossWarning(sortie.levelSeconds());
                    }
                    if (music.bossArrived()) {
                        // An act boss: the warning track with the klaxon and the banner (design/ui/hud).
                        services.sfx.play(Sfx.KLAXON, KLAXON_VOLUME, 1, 0);
                        sortie.setPiece(events.value(i))
                                .boss()
                                .ifPresent(boss -> banner.show(boss.barName(), sortie.tick()));
                    } else if (level.music().bossSting().isPresent()) {
                        music.sting(Sfx.MINIBOSS_STING);
                    }
                }
                case BOSS_RETRY -> music.bossRetry();
                case SPECIAL_CALLED -> {
                    // The Airstrike's call answers the player at once: an urgent line that interrupts
                    // whatever is on the radio, which plays again after it (design/ui/hud, priority
                    // interrupts). The Smart Bomb has no call.
                    if (sortie.special().smartBomb() == null) {
                        SpecialsData.Radio call =
                                services.content.specials().airstrike().radio();
                        queue(
                                call.speaker(),
                                call.portrait(),
                                "neutral",
                                call.line(),
                                false,
                                RadioQueue.Priority.URGENT);
                    }
                }
                case CHAIN_POP -> {
                    // A chained death's burst: the member's own death, as when it is shot (no kill), in
                    // the same step as its sound; until then the member was drawn intact.
                    death(SimEvents.chainPopKind(events.value(i)), x, y);
                }
                case CHAIN_CUT, CHAIN_REGROWN ->
                    effects.start(services.sprites.explosionSmall, TINY_EXPLOSION_FRAME_TICKS, x, y);
                case SPECIAL_DENIED -> hud.specialDenied();
                case ARMOUR_CRITICAL -> {
                    // Okafor's low-armour warning (design/player/armor), once per attempt: urgent, so it
                    // plays at once, interrupting whatever is on the radio, and is never dropped as stale.
                    LevelData.RadioLine warning = services.content.armour().radio();
                    queue(
                            warning.speaker(),
                            warning.speaker(),
                            warning.expression().orElse(Expression.NEUTRAL).slug(),
                            warning.line(),
                            warning.distorted().orElse(false),
                            RadioQueue.Priority.URGENT);
                }
                case AIRSTRIKE_BLAST ->
                    blasts.startOnGround(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, x, y, 0, sortie.groundScroll());
                case SHIELD_HIT -> shimmer = SHIMMER_TICKS;
                case RADIO -> {
                    LevelScript.RadioCue cue = sortie.script().radio().get(events.value(i));
                    if (cue.trigger() == LevelScript.CueTrigger.MISSION_FAILED) {
                        failureLine = Optional.of(failedGroupLine(cue));
                        continue;
                    }
                    int unit =
                            switch (cue.trigger()) {
                                case FIRST_ALLY_HIT -> sortie.firstAllyHit();
                                case FIRST_ALLY_LOST -> sortie.firstAllyLost();
                                default -> -1;
                            };
                    if (cue.trigger() == LevelScript.CueTrigger.ESCORT_FIRST_KILL && barks != null) {
                        // Rook's first kill (M5 part B): a scripted Rook line for the barks' spacing.
                        barks.scripted(cue.speaker(), sortie.levelSeconds());
                    }
                    queue(
                            cue.speaker(),
                            cue.portrait(),
                            cue.expression(),
                            RadioSchedule.line(cue.line(), unit, escortSide()),
                            cue.distorted(),
                            radioSchedule.priority(events.value(i)));
                }
                case ALLY_LOST -> {
                    // It burns on the road: the blast stays where it was, scrolling with the ground.
                    blasts.startOnGround(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, x, y, 0, sortie.groundScroll());
                }
                case PRIMARY_FAILED -> {
                    // As a wreck, without the explosion and the slow motion (design/systems/retry).
                    music.cut();
                    failure = Optional.of(campaign.fail());
                    services.save(SaveSlots.Slot.AUTOSAVE, campaign);
                    failedIn = FAILED_SCREEN_SECONDS;
                }
                case SHIP_DESTROYED -> {
                    effects.start(services.sprites.explosionLarge, LARGE_EXPLOSION_FRAME_TICKS, x, y);
                    slowMotion = SLOW_MOTION_SECONDS;
                    music.cut();
                    failure = Optional.of(campaign.fail());
                    // The used retry, or a game over's return to the launch state, goes into the
                    // autosave at once (design/systems/retry).
                    services.save(SaveSlots.Slot.AUTOSAVE, campaign);
                    failedIn = FAILED_SCREEN_SECONDS;
                }
                // Rook (M5 part A): his hull flashes on a hit; his low-armour bark; as he ejects his
                // craft bursts in the medium explosion and he shouts his eject bark.
                case WINGMAN_HIT -> renderer.wingmanHit(sortie.tick());
                case WINGMAN_CRITICAL -> {
                    if (barks != null) {
                        barks.rookCritical(sortie.levelSeconds());
                    }
                }
                case WINGMAN_EJECTED -> {
                    effects.start(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, x, y);
                    if (barks != null) {
                        barks.rookEjected(sortie.levelSeconds());
                    }
                }
                case SORTIE_RESTARTED -> {
                    if (barks != null) {
                        barks.reset(SimStep.ticks(sortie.levelSeconds()));
                    }
                    failureLine = Optional.empty();
                    layersHit.clear();
                    effects.clear();
                    debris.clear();
                    pieces.clear();
                    blasts.clear();
                    wrecks.clear();
                    creditNumbers.clear();
                    radio.clear();
                    services.voices.stop();
                    warnings.clear();
                    banner.clear();
                    waveBanners.clear();
                    screenFlash.clear();
                    chainSounds.clear();
                    renderer.restart();
                    music.restart();
                    launchPending = true;
                }
                case LEVEL_COMPLETE -> {
                    music.fadeOut();
                    outro.start();
                }
                case SHOT_FIRED,
                        ENEMY_FIRED,
                        SECRET_FOUND,
                        PICKUP_COLLECTED,
                        SHIELD_BROKEN,
                        ARMOUR_HIT,
                        OVERDRIVE_ENDED,
                        GROUP_CLEARED,
                        GROUP_LOST,
                        OBJECTIVE_MET,
                        OBJECTIVE_FAILED,
                        MINE_DROPPED,
                        SET_PIECE_DESCENDED,
                        SET_PIECE_ESCAPED,
                        AIRSTRIKE_INBOUND,
                        ALLY_HIT -> {}
            }
        }
    }

    /**
     * A unit's death at its position: its solid pieces, its explosion and its glow (the pieces first,
     * so the glows of the same death draw over them). A ground unit's stay on the ground, scrolling
     * with it and with its husk; a flyer's stay where it died on the play field.
     */
    private void death(int kind, double x, double y) {
        EnemyLooks look = looks[kind];
        boolean onGround = sortie.enemyKinds().get(kind).layer() == Layer.GROUND;
        EnemyLooks.DeathEffect deathPieces = look.deathPieces();
        start(pieces, deathPieces.frames(), deathPieces.ticksPerFrame(), x, y, deathPieces.delayTicks(), onGround);
        start(effects, look.explosion(), TINY_EXPLOSION_FRAME_TICKS, x, y, 0, onGround);
        EnemyLooks.DeathEffect glow = look.deathGlow();
        start(effects, glow.frames(), glow.ticksPerFrame(), x, y, glow.delayTicks(), onGround);
    }

    /** Starts an animation, if there is one, centred on the position: on the ground or on the play field. */
    private void start(
            Effects into,
            Array<AtlasRegion> frames,
            int ticksPerFrame,
            double x,
            double y,
            int delayTicks,
            boolean onGround) {
        if (frames.isEmpty()) {
            return;
        }
        if (onGround) {
            into.startOnGround(frames, ticksPerFrame, x, y, delayTicks, sortie.groundScroll());
        } else {
            into.start(frames, ticksPerFrame, x, y, delayTicks);
        }
    }

    /**
     * A set piece's chained death (design/enemies/space/leviathan: chained {@code medium} bursts
     * along the body, ichor cloud): a burst at each part in turn, each with its death cloud when it
     * has one; then its break-up's blasts over the body while the body comes apart (or, without a
     * break-up, a large burst at the centre). Off the play plane the bursts sit where its parts are
     * drawn there, turned with the pass's heading, and every burst is scaled and faded as it is.
     */
    private void chainedDeath(int k, Array<AtlasRegion> cloud, double x, double y) {
        SetPiece piece = sortie.setPiece(k);
        if (piece.boss().isPresent()) {
            showerDelay = LevelRenderer.chainTicks(piece);
            if (LevelRenderer.tailToHead(piece)) {
                tailToHeadDeath(k, piece, cloud, x, y);
                return;
            }
        }
        double altitude = piece.onPlane() ? 0 : piece.altitude(1);
        float scale = LevelRenderer.highAirScale(altitude);
        float opacity = LevelRenderer.highAirOpacity(altitude);
        boolean crossing = piece.boss().isEmpty()
                && !piece.spec().passes().get(piece.pass()).descends();
        double heading = piece.boss().isPresent()
                ? 0
                : piece.spec().passes().get(piece.pass()).headingRadians();
        SetPieceDeath death = renderer.death(k);
        wrecks.start(k, x, y, scale, opacity, crossing);
        String petals = piece.spec().slug() + "-petals";
        if (piece.boss().isPresent() && services.sprites.has(petals)) {
            // the crown's petals tear off the core as it bursts, at the break-up's swap when it has
            // one (design/enemies/bosses/gorgon-frigate)
            for (int p = 0; p < piece.partCount(); p++) {
                if (piece.spec().parts().get(p).vital()) {
                    pieces.start(
                            services.sprites.frames(petals),
                            PETAL_FRAME_TICKS,
                            x + piece.partOffsetX(p),
                            y + piece.partOffsetY(p),
                            death == null ? 0 : death.swap);
                }
            }
        }
        for (int p = 0; p < piece.partCount(); p++) {
            int delay = p * CHAIN_STEP_TICKS;
            double px = x + piece.partOffsetX(p) * scale;
            double py = y + piece.partOffsetY(p) * scale;
            effects.start(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, px, py, delay, scale, opacity);
            if (!cloud.isEmpty()) {
                effects.start(cloud, DEATH_CLOUD_FRAME_TICKS, px, py, delay, scale, opacity);
            }
        }
        if (death == null) {
            int delay = piece.partCount() * CHAIN_STEP_TICKS;
            effects.start(services.sprites.explosionLarge, LARGE_EXPLOSION_FRAME_TICKS, x, y, delay, scale, opacity);
            if (!cloud.isEmpty()) {
                effects.start(cloud, DEATH_CLOUD_FRAME_TICKS, x, y, delay, scale, opacity);
            }
            return;
        }
        double cos = Math.cos(heading);
        double sin = Math.sin(heading);
        for (SetPieceDeath.Blast blast : death.blasts) {
            double px = x + (blast.dx() * cos + blast.dy() * sin) * scale;
            double py = y + (-blast.dx() * sin + blast.dy() * cos) * scale;
            effects.start(blast.frames(), blast.ticksPerFrame(), px, py, blast.at(), scale, opacity);
        }
        sounds.breakUp(x, death.swap);
    }

    /**
     * An act boss's chained death (design/enemies/bosses/brood-carrier, Death): {@code medium}
     * bursts at every part and along the hull from tail to head over its chain, each with its ichor
     * cloud and a burst's sound; at the chain's end {@code large} blasts at the head and the centre,
     * the screen flash and then the credit shower; its break-up's blasts on top when it has one. The
     * body stays under the chain ({@link SetPieceWrecks}).
     */
    private void tailToHeadDeath(int k, SetPiece piece, Array<AtlasRegion> cloud, double x, double y) {
        double altitude = piece.onPlane() ? 0 : piece.altitude(1);
        float scale = LevelRenderer.highAirScale(altitude);
        SetPieceDeath death = renderer.death(k);
        wrecks.start(k, x, y, scale, 1, false);
        int chain = LevelRenderer.chainTicks(piece);
        // A boss with its own sac burst (round 25, the Brood Carrier) bursts each sac with it.
        Sfx sacBurst = sounds.sacBurst(k);
        int n = 0;
        for (var burst : LevelRenderer.deathChain(piece)) {
            double px = x + burst.dx();
            double py = y + burst.dy();
            effects.start(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, px, py, burst.at(), scale, 1);
            if (!cloud.isEmpty() && burst.part() >= 0) {
                effects.start(cloud, DEATH_CLOUD_FRAME_TICKS, px, py, burst.at(), scale, 1);
            }
            if (sacBurst != null && burst.part() >= 0 && sac(piece, burst.part())) {
                chainSounds.play(sacBurst, CHAIN_SOUND_VOLUME, 0.85f + 0.05f * (burst.part() % 3), pan(px), burst.at());
            } else if (n++ % 2 == 0) {
                chainSounds.play(
                        n % 4 == 1 ? Sfx.EXPLOSION_MEDIUM_A : Sfx.EXPLOSION_MEDIUM_B,
                        CHAIN_SOUND_VOLUME,
                        0.8f + 0.1f * (n % 3),
                        pan(px),
                        burst.at());
            }
        }
        double[] head = LevelRenderer.deathHead(piece);
        effects.start(
                services.sprites.explosionLarge,
                LARGE_EXPLOSION_FRAME_TICKS,
                x + head[0],
                y + head[1],
                chain,
                scale,
                1);
        effects.start(services.sprites.explosionLarge, LARGE_EXPLOSION_FRAME_TICKS, x, y, chain, scale, 1);
        if (!cloud.isEmpty()) {
            effects.start(cloud, DEATH_CLOUD_FRAME_TICKS, x, y, chain, scale, 1);
        }
        chainSounds.play(sounds.deathBlast(k), 1, 1, pan(x), chain);
        chainSounds.play(Sfx.EXPLOSION_LARGE_C, 1, 0.9f, pan(x), chain + 3);
        if (LevelRenderer.flashesAtDeath(piece)) {
            screenFlash.start(chain);
        }
        if (death != null) {
            for (SetPieceDeath.Blast blast : death.blasts) {
                effects.start(
                        blast.frames(), blast.ticksPerFrame(), x + blast.dx(), y + blast.dy(), blast.at(), scale, 1);
            }
            sounds.breakUp(x, death.swap);
        }
    }

    /** Whether part {@code p} of a boss is a bay sac: neither its vital core nor a fire-only turret. */
    private static boolean sac(SetPiece piece, int p) {
        return !piece.spec().parts().get(p).vital() && !piece.partArmoured(p);
    }

    /** A sound's pan for a play-field x: the play field's width spans 1.2 of the stereo field. */
    private static float pan(double x) {
        return (float) Math.clamp((x / PlayField.WIDTH - 0.5) * 1.2, -0.6, 0.6);
    }

    /** The chain's bursts' and the klaxon's levels. */
    private static final float CHAIN_SOUND_VOLUME = 0.7f;

    private static final float KLAXON_VOLUME = 0.8f;

    /** The coins of a boss's credit shower fly out this many steps apart, in a ring of this many. */
    private static final int SHOWER_STEP_TICKS = 3;

    private static final int SHOWER_COINS = 16;
    /** A coin's spin: the pickups' 10 fps. */
    private static final int SHOWER_FRAME_TICKS = 6;
    /** The shower starts after the chained bursts over the frigate's four parts (an act boss's: after its chain). */
    private static final int SHOWER_DELAY_TICKS = 4 * CHAIN_STEP_TICKS;

    /**
     * A boss's credit shower (design/enemies/bosses: the death sequence): its credits as a number
     * over the bell and a ring of salvage coins spinning out after the chained bursts.
     */
    private void creditShower(int credits, double x, double y) {
        int delay = showerDelay;
        creditNumbers.show(credits, x, y);
        for (int c = 0; c < SHOWER_COINS; c++) {
            double angle = 2 * Math.PI * c / SHOWER_COINS;
            double reach = 40 + 50 * (c % 3);
            effects.start(
                    services.sprites.salvageSmall,
                    SHOWER_FRAME_TICKS,
                    x + Math.cos(angle) * reach,
                    y + Math.sin(angle) * reach,
                    delay + c * SHOWER_STEP_TICKS,
                    1,
                    1);
        }
    }

    /** Queues a radio line with its voice file, if it has one; without one it shows as text only. */
    private RadioQueue.Message queue(
            String speaker,
            String portrait,
            String expression,
            String line,
            boolean distorted,
            RadioQueue.Priority priority) {
        Optional<Voices.Voice> voice = services.voices.radio(speaker, line, expression);
        return radio.add(
                speaker,
                portrait,
                expression,
                line,
                distorted,
                priority,
                voice.map(Voices.Voice::path),
                voice.map(Voices.Voice::seconds).orElse(0f));
    }

    /** The radio's squelch on open and close, and a soft blip for every other typed character. */
    private void playRadio(float seconds) {
        float untilTimed =
                sortie.complete() ? Float.POSITIVE_INFINITY : radioSchedule.untilTimed(sortie.levelSeconds());
        switch (radio.update(seconds, untilTimed)) {
            case OPENED -> {
                services.sfx.play(Sfx.RADIO_OPEN, RADIO_VOLUME, 1, 0);
                // The voice starts with the message; an urgent line cuts the one that plays
                // (design/audio/voice, Playback).
                RadioQueue.Message message = radio.current().orElseThrow();
                if (barks != null) {
                    // A Rook line on the radio, bark or scripted, starts the barks' spacing.
                    barks.opened(message.speaker(), sortie.levelSeconds());
                }
                message.voice()
                        .ifPresentOrElse(
                                path -> services.voices.play(new Voices.Voice(path, message.voiceSeconds())),
                                services.voices::stop);
            }
            case CLOSED -> {
                services.sfx.play(Sfx.RADIO_CLOSE, RADIO_VOLUME, 1, 0);
                services.voices.stop();
            }
            case TYPED -> {
                if (++typed % 2 == 0) {
                    services.sfx.play(Sfx.TYPEWRITER, TYPING_VOLUME, 1, 0);
                }
            }
            case NONE -> {}
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        renderer.draw(
                batch,
                sortie,
                effects,
                debris,
                pieces,
                blasts,
                wrecks,
                creditNumbers,
                warnings,
                clock.alpha(),
                (float) shimmer / SHIMMER_TICKS,
                services.settings().gameplay().flashReduction(),
                screenFlash);
        if (threatArrows != null && sortie.flying()) {
            threatArrows.draw(batch, sortie, clock.alpha());
        }
        waveBanners.draw(batch, sortie.tick(), clock.alpha());
        banner.draw(batch, sortie.tick(), clock.alpha());
        hud.draw(batch, sortie, radio, visiblePrompts());
    }

    /**
     * The control prompts show in the first section, once the launch is over; a level's
     * contextual prompts from their time for their seconds (design/ui/hud, control prompts).
     */
    private List<PromptTexts.Text> visiblePrompts() {
        List<PromptTexts.Text> shown = new ArrayList<>();
        if (!sortie.launching() && sortie.section() == 1) {
            shown.addAll(promptTexts.of(prompts.pending()));
        }
        double t = sortie.levelSeconds();
        for (LevelData.Prompt prompt : level.prompts().orElse(List.of())) {
            if (prompt.requiresSpecial() && !sortie.special().fitted()) {
                continue;
            }
            // Done what it says, as the control prompts: an enemy destroyed on its skip layer
            // (Level 02's ground unit, Level 03's Spore Bomber on low-air) makes it leave.
            boolean done = prompt.skipLayer().map(layersHit::contains).orElse(false);
            if (!done && t >= prompt.t() && t < prompt.t() + prompt.seconds()) {
                shown.add(new PromptTexts.Text(prompt.action(), prompt.keys()));
            }
        }
        return shown;
    }

    /** Banks the won level in the campaign and shows its debrief. */
    private DebriefScreen debrief() {
        LevelResult result = sortie.result();
        int launchBalance = campaign.credits();
        boolean newBest = campaign.complete(
                result,
                sortie.ship().defences().armour(),
                sortie.special().used(),
                sortie.special().found(),
                sortie.wingman()
                        .map(rook -> java.util.OptionalDouble.of(rook.armour()))
                        .orElse(java.util.OptionalDouble.empty()));
        if (newBest) {
            // A replay's better grade goes into the save it was started from (design/ui/mission-select).
            campaign.replay()
                    .ifPresent(replay -> MissionSelectScreen.keepGrade(
                            services, replay, result.grade().letter()));
        }
        return new DebriefScreen(services, campaign, result, sortie.script().number(), name, launchBalance, newBest);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        music.dispose();
        warnings.dispose();
        banner.dispose();
        if (threatArrows != null) {
            threatArrows.dispose();
        }
        services.sprites.leaveLevel(levelNumber);
    }
}
