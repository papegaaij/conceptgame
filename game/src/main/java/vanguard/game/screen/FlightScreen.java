package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Optional;
import vanguard.game.GameServices;
import vanguard.game.audio.FlightSounds;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.input.Action;
import vanguard.game.input.FlightCommands;
import vanguard.game.render.Effects;
import vanguard.game.render.FlightRenderer;
import vanguard.game.render.HudPanels;
import vanguard.sim.FixedStepClock;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * Flying the M1 test sortie: runs the simulation at its fixed step, turns its events into sound,
 * effects and HUD feedback and draws it interpolated. Pause (Esc / Start) returns to the title
 * until the pause screen arrives in M3.
 */
public final class FlightScreen implements GameScreen {
    /** The sandbox flies the same waves every time. */
    private static final long SEED = 2185;

    private static final int MAX_STEPS_PER_FRAME = 8;
    private static final float MUSIC_VOLUME = 0.6f;
    /** Death plays in slow motion for a second (design/systems/retry). */
    private static final float SLOW_MOTION_SECONDS = 1;

    private static final float SLOW_MOTION_RATE = 0.5f;
    /** The ship's blue shimmer fades over this many steps after a shield hit. */
    private static final int SHIMMER_TICKS = 8;

    private static final int TINY_EXPLOSION_FRAME_TICKS = 2;
    private static final int LARGE_EXPLOSION_FRAME_TICKS = 4;
    private static final int IMPACT_FRAME_TICKS = 2;
    private static final String WEAPON = "PULSE CANNON";
    /** M1 flies the Pulse Cannon at level 1 only. */
    private static final int WEAPON_LEVEL = 1;

    private final GameServices services;
    private final Sortie sortie = new Sortie(SEED);
    private final FixedStepClock clock = new FixedStepClock(SimStep.SECONDS, MAX_STEPS_PER_FRAME);
    private final FlightCommands commands;
    private final FlightSounds sounds;
    private final FlightRenderer renderer;
    private final HudPanels hud;
    private final Effects effects = new Effects();
    private Optional<MusicStreamer> music;
    private float slowMotion;
    private int shimmer;

    public FlightScreen(GameServices services) {
        this.services = services;
        commands = new FlightCommands(services.controls);
        sounds = new FlightSounds(services.sfx);
        renderer = new FlightRenderer(services.sprites, services.flash);
        hud = new HudPanels(services.sprites.pixel, services.font);
        music = startMusic();
    }

    private Optional<MusicStreamer> startMusic() {
        // Level 01's track 5, "Coalition Rising" (design/audio/music).
        return MusicStreamer.play(services.audio, services.files.internal("music/coalition-rising.ogg"), MUSIC_VOLUME);
    }

    @Override
    public GameScreen update(float seconds) {
        if (services.input.pressed(Action.PAUSE)) {
            return new TitleScreen(services);
        }
        float simSeconds = seconds;
        if (slowMotion > 0) {
            slowMotion -= seconds;
            simSeconds *= SLOW_MOTION_RATE;
        }
        int steps = clock.advance(simSeconds);
        int stepCommands = commands.of(services.input);
        for (int i = 0; i < steps; i++) {
            sortie.step(stepCommands);
            effects.step();
            if (shimmer > 0) {
                shimmer--;
            }
            react(sortie.events());
        }
        return this;
    }

    private void react(SimEvents events) {
        sounds.play(events);
        for (int i = 0; i < events.size(); i++) {
            double x = events.x(i);
            double y = events.y(i);
            switch (events.type(i)) {
                case ENEMY_HIT -> effects.start(services.sprites.pulseImpact, IMPACT_FRAME_TICKS, x, y);
                case ENEMY_DESTROYED -> effects.start(services.sprites.explosionTiny, TINY_EXPLOSION_FRAME_TICKS, x, y);
                case SHIELD_HIT -> shimmer = SHIMMER_TICKS;
                case SHIP_DESTROYED -> {
                    effects.start(services.sprites.explosionLarge, LARGE_EXPLOSION_FRAME_TICKS, x, y);
                    slowMotion = SLOW_MOTION_SECONDS;
                    // The music cuts; the failure sting plays as a sound effect.
                    music.ifPresent(MusicStreamer::close);
                    music = Optional.empty();
                }
                case SORTIE_RESTARTED -> {
                    effects.clear();
                    music = startMusic();
                }
                case SHOT_FIRED, SHIELD_BROKEN, ARMOUR_HIT -> {}
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        renderer.draw(batch, sortie, effects, clock.alpha(), (float) shimmer / SHIMMER_TICKS);
        hud.draw(batch, sortie.ship().defences(), WEAPON, WEAPON_LEVEL, sortie.attempt());
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
    }
}
