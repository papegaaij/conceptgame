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
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.Flight;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.GameServices;
import vanguard.game.audio.FlightSounds;
import vanguard.game.audio.LevelMusic;
import vanguard.game.audio.Sfx;
import vanguard.game.input.Action;
import vanguard.game.input.FlightCommands;
import vanguard.game.level.ControlPrompts;
import vanguard.game.level.Outro;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.game.level.RadioSchedule;
import vanguard.game.render.CreditNumbers;
import vanguard.game.render.EdgeWarnings;
import vanguard.game.render.Effects;
import vanguard.game.render.EnemyLooks;
import vanguard.game.render.Hud;
import vanguard.game.render.LevelRenderer;
import vanguard.game.render.PodPivots;
import vanguard.game.render.SetPieceDeath;
import vanguard.game.render.SetPieceWrecks;
import vanguard.game.render.WeaponLooks;
import vanguard.game.settings.Settings;
import vanguard.sim.FixedStepClock;
import vanguard.sim.Layer;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Rules;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * Flying a level of the campaign: runs the simulation at its fixed step from the campaign's
 * level-start state, turns its events into sound, effects, radio chatter and HUD feedback, plays
 * the level's music cues and draws it interpolated. When the level is won it shows the radio's
 * last messages ({@link Outro}), banks the result in the campaign and shows the debrief. When the ship is destroyed the
 * death plays out, then the mission failed screen opens over the level, or the game over screen
 * follows when no retry is left (design/systems/retry); the failure's used retry is autosaved at
 * once, and so is a game over's return to the hangar before the level. The sortie flies what {@link Flight} maps of the fitted loadout; the HUD lists the fitted
 * items that fly from M4 on. Pause (Esc / P / Start), the window losing
 * the focus and a gamepad disconnecting open the pause menu over it. The Gameplay tab's text speed
 * and flash reduction and the Controls tab's auto-fire apply from the next frame on.
 */
public final class LevelScreen implements GameScreen {
    /** Every run of a level gets the same waves; the seed only picks hover times. */
    private static final long SEED = 2185;

    private static final int MAX_STEPS_PER_FRAME = 8;
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
    /** A destroyed ground target's 8 debris frames last 0.4 s. */
    private static final int DEBRIS_FRAME_TICKS = 3;
    /** A ground unit's remains stay on the ground until they scroll off (at most 10 s). */
    private static final int REMAINS_TICKS = SimStep.ticks(10);

    private static final float RADIO_VOLUME = 0.5f;
    private static final float TYPING_VOLUME = 0.15f;

    private final GameServices services;
    private final Campaign campaign;
    private final LevelData level;
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

    private final Array<AtlasRegion> explosionMedium;
    /** Each set piece's death cloud, its slug's {@code -ichor} frames, by index in the script; empty for none. */
    private final List<Array<AtlasRegion>> deathClouds;
    /** The spark of a shot glancing off a hardened target. */
    private final Array<AtlasRegion> glance;

    private final FlightSounds sounds;
    private final LevelRenderer renderer;
    private final Hud hud;
    private final Effects effects = Effects.glowing();
    private final Effects debris = Effects.solid();
    /** The solid death pieces of air units, at play-field positions (no ground scroll). */
    private final Effects pieces = Effects.solid();
    /** The set pieces breaking up at their death. */
    private final SetPieceWrecks wrecks;

    private final CreditNumbers creditNumbers = new CreditNumbers();
    private final EdgeWarnings warnings;
    private final RadioQueue radio = new RadioQueue();
    private final RadioSchedule radioSchedule;
    private final ControlPrompts prompts;
    private final PromptTexts promptTexts;
    private final LevelMusic music;
    private final String name;
    private final Flight flight;
    private float slowMotion;
    private final Outro outro = new Outro();
    /** What the ship's destruction leads to, and how long until its screen opens. */
    private Optional<Campaign.Failure> failure = Optional.empty();

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
        Rules rules = SimSpecs.rules(services.content, levelKey, difficulty);
        campaign.launch();
        flight = Flight.of(services.content, services.catalogue, campaign);
        sortie = new Sortie(
                SEED,
                flight.loadout(),
                SimSpecs.level(services.content, levelKey, difficulty),
                services.invulnerable ? rules.withInvulnerableShip() : rules,
                campaign.armour());
        radioSchedule = new RadioSchedule(sortie.script());
        wrecks = new SetPieceWrecks(sortie.setPieceCount());
        looks = EnemyLooks.of(sortie.enemyKinds(), services.sprites, services.content);
        weaponLooks = new WeaponLooks(
                sortie.armament(),
                flight.weapons().stream().mapToInt(Flight.Weapon::level).toArray(),
                services.sprites,
                new PodPivots(services.files));
        glance = services.sprites.frames("ballistic-impact");
        explosionMedium = services.sprites.frames("explosion-medium");
        deathClouds = sortie.script().setPieces().stream()
                .map(spec -> services.sprites.has(spec.slug() + "-ichor")
                        ? services.sprites.frames(spec.slug() + "-ichor")
                        : new Array<AtlasRegion>())
                .toList();
        sounds = new FlightSounds(
                services.sfx,
                looks,
                sortie.armament(),
                sortie.script().setPieces().stream()
                        .map(LevelScript.SetPieceSpec::slug)
                        .toList());
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
        music = new LevelMusic(
                services.audio,
                services.mixer,
                services.files.internal("music/" + theme + "-base.ogg"),
                services.files.internal("music/" + theme + ".ogg"),
                services.sfx,
                ambience(level.music().ambience()),
                level.music().startSection(),
                level.music().startDb().orElse(0.0),
                level.music()::full);
    }

    /** The file name of a level theme's stems by its track number (design/audio/music, track list). */
    private static String theme(int track) {
        return switch (track) {
            case 4 -> "afterburner";
            case 5 -> "coalition-rising";
            default -> throw new IllegalArgumentException("no stems for track " + track + " yet");
        };
    }

    /** A setting's ambience loop (design/audio/sfx, ambience per setting). */
    private static Sfx ambience(String setting) {
        return switch (setting) {
            case "earth-orbit" -> Sfx.AMBIENCE_ORBIT;
            default -> throw new IllegalArgumentException("no ambience for " + setting + " yet");
        };
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

    /** Starts the level over from its start state with {@code armour} (a retry, or the pause menu's restart). */
    void retry(double armour) {
        sortie.retry(armour);
        outro.stop();
        slowMotion = 0;
        failure = Optional.empty();
    }

    @Override
    public Transition update(float seconds) {
        campaign.play(seconds);
        if (services.input.pressed(Action.PAUSE) || services.input.interrupted()) {
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
                return what == Campaign.Failure.GAME_OVER
                        ? Transition.replace(new GameOverScreen(services, campaign, mission()))
                        : Transition.open(new MissionFailedScreen(services, this));
            }
        }
        Settings settings = services.settings();
        radio.charsPerSecond(settings.gameplay().textSpeed());
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
            wrecks.step();
            sounds.step();
            creditNumbers.step();
            if (shimmer > 0) {
                shimmer--;
            }
            react(sortie.events());
            sounds.edgeWarnings(warnings.step(sortie.edgeWarnings(), sortie.tick()));
        }
        if (launchPending && sortie.launching()) {
            sounds.launch();
            launchPending = false;
        }
        playRadio(seconds);
        music.update(sortie.section(), seconds, radio.current().isPresent());
        return Transition.STAY;
    }

    private void react(SimEvents events) {
        sounds.play(events);
        for (int i = 0; i < events.size(); i++) {
            double x = events.x(i);
            double y = events.y(i);
            switch (events.type(i)) {
                case ENEMY_HIT, GROUND_HIT ->
                    effects.start(weaponLooks.impact(events.value(i)), IMPACT_FRAME_TICKS, x, y);
                case SHOT_GLANCED -> effects.start(glance, IMPACT_FRAME_TICKS, x, y);
                case BLAST -> effects.start(services.sprites.explosionSmall, TINY_EXPLOSION_FRAME_TICKS, x, y);
                case ENEMY_DESTROYED -> {
                    EnemyLooks look = looks[events.value(i)];
                    layersHit.add(sortie.enemyKinds().get(events.value(i)).layer());
                    // The solid pieces first, so the glows of the same death draw over them.
                    start(pieces, look.deathPieces(), x, y);
                    effects.start(look.explosion(), TINY_EXPLOSION_FRAME_TICKS, x, y);
                    start(effects, look.deathGlow(), x, y);
                    if (!look.remains().isEmpty()) {
                        debris.start(look.remains(), REMAINS_TICKS, x, y + sortie.groundScroll());
                    }
                }
                case CLAMP_HIT -> effects.start(glance, IMPACT_FRAME_TICKS, x, y);
                case DEBRIS_HIT -> effects.start(weaponLooks.impact(events.value(i)), IMPACT_FRAME_TICKS, x, y);
                case DEBRIS_DESTROYED, MINE_BURST, MINE_DESTROYED ->
                    effects.start(services.sprites.explosionTiny, TINY_EXPLOSION_FRAME_TICKS, x, y);
                case PART_DESTROYED -> effects.start(explosionMedium, MEDIUM_EXPLOSION_FRAME_TICKS, x, y);
                case SET_PIECE_DESTROYED -> chainedDeath(events.value(i), deathClouds.get(events.value(i)), x, y);
                case GROUND_DESTROYED -> {
                    debris.start(
                            services.sprites.cargoContainerBreak, DEBRIS_FRAME_TICKS, x, y + sortie.groundScroll());
                    effects.start(services.sprites.explosionSmall, TINY_EXPLOSION_FRAME_TICKS, x, y);
                }
                case CREDITS_PICKED_UP -> creditNumbers.show(events.value(i), x, y);
                case SHIELD_HIT -> shimmer = SHIMMER_TICKS;
                case RADIO -> {
                    LevelScript.RadioCue cue = sortie.script().radio().get(events.value(i));
                    radio.add(
                            cue.speaker(),
                            cue.portrait(),
                            cue.expression(),
                            cue.line(),
                            cue.distorted(),
                            radioSchedule.priority(events.value(i)));
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
                case SORTIE_RESTARTED -> {
                    layersHit.clear();
                    effects.clear();
                    debris.clear();
                    pieces.clear();
                    wrecks.clear();
                    creditNumbers.clear();
                    radio.clear();
                    warnings.clear();
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
                        SET_PIECE_ESCAPED -> {}
            }
        }
    }

    /** Starts a unit's death animation, if it has one, centred on it. */
    private static void start(Effects into, EnemyLooks.DeathEffect death, double x, double y) {
        if (!death.frames().isEmpty()) {
            into.start(death.frames(), death.ticksPerFrame(), x, y, death.delayTicks());
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
        double altitude = piece.onPlane() ? 0 : piece.altitude(1);
        float scale = LevelRenderer.highAirScale(altitude);
        float opacity = LevelRenderer.highAirOpacity(altitude);
        LevelScript.Pass pass = piece.spec().passes().get(piece.pass());
        SetPieceDeath death = renderer.death(k);
        wrecks.start(k, x, y, scale, opacity, !pass.descends());
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
        double cos = Math.cos(pass.headingRadians());
        double sin = Math.sin(pass.headingRadians());
        for (SetPieceDeath.Blast blast : death.blasts) {
            double px = x + (blast.dx() * cos + blast.dy() * sin) * scale;
            double py = y + (-blast.dx() * sin + blast.dy() * cos) * scale;
            effects.start(blast.frames(), blast.ticksPerFrame(), px, py, blast.at(), scale, opacity);
        }
        sounds.breakUp(x, death.swap);
    }

    /** The radio's squelch on open and close, and a soft blip for every other typed character. */
    private void playRadio(float seconds) {
        float untilTimed =
                sortie.complete() ? Float.POSITIVE_INFINITY : radioSchedule.untilTimed(sortie.levelSeconds());
        switch (radio.update(seconds, untilTimed)) {
            case OPENED -> services.sfx.play(Sfx.RADIO_OPEN, RADIO_VOLUME, 1, 0);
            case CLOSED -> services.sfx.play(Sfx.RADIO_CLOSE, RADIO_VOLUME, 1, 0);
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
                wrecks,
                creditNumbers,
                warnings,
                clock.alpha(),
                (float) shimmer / SHIMMER_TICKS,
                services.settings().gameplay().flashReduction());
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
        boolean newBest = campaign.complete(result, sortie.ship().defences().armour());
        return new DebriefScreen(services, campaign, result, sortie.script().number(), name, launchBalance, newBest);
    }

    @Override
    public void dispose() {
        music.dispose();
        warnings.dispose();
    }
}
