package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import java.util.Locale;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.game.GameServices;
import vanguard.game.audio.FlightSounds;
import vanguard.game.audio.LevelMusic;
import vanguard.game.audio.Sfx;
import vanguard.game.input.Action;
import vanguard.game.input.FlightCommands;
import vanguard.game.level.ControlPrompts;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.game.render.CreditNumbers;
import vanguard.game.render.Effects;
import vanguard.game.render.EnemyLooks;
import vanguard.game.render.Hud;
import vanguard.game.render.LevelRenderer;
import vanguard.game.settings.Settings;
import vanguard.sim.FixedStepClock;
import vanguard.sim.LevelScript;
import vanguard.sim.Rules;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * Flying a level: runs the simulation at its fixed step, turns its events into sound, effects,
 * radio chatter and HUD feedback, plays the level's music cues and draws it interpolated. When the
 * level is won it shows the last radio line, then the debrief. Pause (Esc / P / Start), the window
 * losing the focus and a gamepad disconnecting open the pause menu over it. The Gameplay tab's text
 * speed and flash reduction and the Controls tab's auto-fire apply from the next frame on.
 */
public final class LevelScreen implements GameScreen {
    /** The only level so far; the campaign picks the level in M3. */
    static final String LEVEL = "act-1-first-contact/level-01-break-at-dawn";
    /** Every run of a level gets the same waves; the seed only picks hover times. */
    private static final long SEED = 2185;

    private static final int MAX_STEPS_PER_FRAME = 8;
    /** Death plays in slow motion for a second (design/systems/retry). */
    private static final float SLOW_MOTION_SECONDS = 1;

    private static final float SLOW_MOTION_RATE = 0.5f;
    /** The ship's blue shimmer fades over this many steps after a shield hit. */
    private static final int SHIMMER_TICKS = 8;

    private static final int TINY_EXPLOSION_FRAME_TICKS = 2;
    private static final int LARGE_EXPLOSION_FRAME_TICKS = 4;
    private static final int IMPACT_FRAME_TICKS = 2;
    /** A destroyed ground target's 8 debris frames last 0.4 s. */
    private static final int DEBRIS_FRAME_TICKS = 3;

    private static final float RADIO_VOLUME = 0.5f;
    private static final float TYPING_VOLUME = 0.15f;
    private static final String WEAPON = "PULSE CANNON";
    /** The starter loadout flies the Pulse Cannon at level 1. */
    private static final int WEAPON_LEVEL = 1;

    private final GameServices services;
    private final LevelData level;
    private final Sortie sortie;
    private final FixedStepClock clock = new FixedStepClock(SimStep.SECONDS, MAX_STEPS_PER_FRAME);
    private final EnemyLooks[] looks;
    private final FlightSounds sounds;
    private final LevelRenderer renderer;
    private final Hud hud;
    private final Effects effects = Effects.glowing();
    private final Effects debris = Effects.solid();
    private final CreditNumbers creditNumbers = new CreditNumbers();
    private final RadioQueue radio = new RadioQueue();
    private final ControlPrompts prompts;
    private final PromptTexts promptTexts;
    private final LevelMusic music;
    private final String name;
    private final Difficulty difficulty;
    private float slowMotion;
    private float outro = -1;
    private int shimmer;
    private int typed;

    /** Level 01 at {@code difficulty}; the campaign picks the level from part B of M3 on. */
    public LevelScreen(GameServices services, Difficulty difficulty) {
        this.services = services;
        this.difficulty = difficulty;
        level = services.content.level(LEVEL);
        Rules rules = SimSpecs.rules(services.content, LEVEL, difficulty);
        sortie = new Sortie(
                SEED,
                SimSpecs.starterLoadout(services.content, difficulty),
                SimSpecs.level(services.content, LEVEL, difficulty),
                services.invulnerable ? rules.withInvulnerableShip() : rules);
        looks = EnemyLooks.of(sortie.enemyKinds(), services.sprites);
        sounds = new FlightSounds(services.sfx, looks);
        renderer = new LevelRenderer(services.sprites, looks, services.flash, services.fonts.body, level, LEVEL);
        name = levelName(LEVEL);
        hud = new Hud(
                services.sprites,
                services.fonts,
                sortie.script().number(),
                name,
                services.content.economy().startingCredits());
        prompts = new ControlPrompts(level.controlPrompts());
        promptTexts = new PromptTexts(services.input.bindings());
        // Track 5, "Coalition Rising" (design/audio/music), over the Earth-orbit ambience.
        music = new LevelMusic(
                services.audio,
                services.mixer,
                services.files.internal("music/coalition-rising.ogg"),
                services.sfx,
                Sfx.AMBIENCE_ORBIT,
                level.music().startSection());
    }

    /** The level's name from its key: {@code level-01-break-at-dawn} is "break at dawn". */
    private static String levelName(String key) {
        return key.substring(key.lastIndexOf('/') + "/level-01-".length()).replace('-', ' ');
    }

    /** "MISSION 01 - BREAK AT DAWN", for the pause menu. */
    String mission() {
        return String.format(Locale.ROOT, "MISSION %02d - %s", sortie.script().number(), name)
                .toUpperCase(Locale.ROOT);
    }

    Difficulty difficulty() {
        return difficulty;
    }

    /** The time flown in this attempt. */
    double seconds() {
        return sortie.levelSeconds();
    }

    long score() {
        return sortie.score();
    }

    /** Starts the level over (the pause menu's Restart mission): a retry, as after the ship's destruction. */
    void retry() {
        sortie.retry();
        outro = -1;
        slowMotion = 0;
    }

    @Override
    public Transition update(float seconds) {
        if (services.input.pressed(Action.PAUSE) || services.input.interrupted()) {
            return Transition.open(new PauseScreen(services, this));
        }
        if (outro >= 0) {
            outro -= seconds;
            if (outro <= 0) {
                return Transition.replace(new DebriefScreen(
                        services,
                        sortie.result(),
                        sortie.script().number(),
                        name,
                        services.content.economy().startingCredits()));
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
            creditNumbers.step();
            if (shimmer > 0) {
                shimmer--;
            }
            react(sortie.events());
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
                case ENEMY_HIT, GROUND_HIT -> effects.start(services.sprites.pulseImpact, IMPACT_FRAME_TICKS, x, y);
                case ENEMY_DESTROYED ->
                    effects.start(looks[events.value(i)].explosion(), TINY_EXPLOSION_FRAME_TICKS, x, y);
                case GROUND_DESTROYED -> {
                    debris.start(
                            services.sprites.cargoContainerBreak, DEBRIS_FRAME_TICKS, x, y + sortie.groundScroll());
                    effects.start(services.sprites.explosionSmall, TINY_EXPLOSION_FRAME_TICKS, x, y);
                }
                case CREDITS_PICKED_UP -> creditNumbers.show(events.value(i), x, y);
                case SHIELD_HIT -> shimmer = SHIMMER_TICKS;
                case RADIO -> {
                    LevelScript.RadioCue cue = sortie.script().radio().get(events.value(i));
                    radio.add(cue.speaker(), cue.line(), cue.distorted());
                }
                case SHIP_DESTROYED -> {
                    effects.start(services.sprites.explosionLarge, LARGE_EXPLOSION_FRAME_TICKS, x, y);
                    slowMotion = SLOW_MOTION_SECONDS;
                    music.cut();
                }
                case SORTIE_RESTARTED -> {
                    effects.clear();
                    debris.clear();
                    creditNumbers.clear();
                    radio.clear();
                    music.restart();
                }
                case LEVEL_COMPLETE -> {
                    music.fadeOut();
                    outro = (float) LevelData.OUTRO_SECONDS;
                }
                case SHOT_FIRED,
                        ENEMY_FIRED,
                        SECRET_FOUND,
                        PICKUP_COLLECTED,
                        SHIELD_BROKEN,
                        ARMOUR_HIT,
                        OBJECTIVE_MET -> {}
            }
        }
    }

    /** The radio's squelch on open and close, and a soft blip for every other typed character. */
    private void playRadio(float seconds) {
        switch (radio.update(seconds)) {
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
                creditNumbers,
                clock.alpha(),
                (float) shimmer / SHIMMER_TICKS,
                services.settings().gameplay().flashReduction());
        hud.draw(batch, sortie, radio, visiblePrompts(), WEAPON, WEAPON_LEVEL);
    }

    /** The control prompts show in the first section, once the launch is over. */
    private List<PromptTexts.Text> visiblePrompts() {
        return sortie.launching() || sortie.section() != 1 ? List.of() : promptTexts.of(prompts.pending());
    }

    @Override
    public void dispose() {
        music.dispose();
    }
}
