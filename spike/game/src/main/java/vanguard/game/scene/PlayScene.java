package vanguard.game.scene;

import com.badlogic.gdx.Gdx;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import vanguard.game.FixedStepClock;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.audio.SfxBank;
import vanguard.game.input.PlayerInput;
import vanguard.game.render.HaloPlatform;
import vanguard.game.render.HudPanels;
import vanguard.game.render.SparkField;
import vanguard.game.render.WorldRenderer;
import vanguard.sim.Autopilot;
import vanguard.sim.InputRecording;
import vanguard.sim.SimConfig;
import vanguard.sim.SimEvents;
import vanguard.sim.World;

/** The play field under gate-1 load, driven by the player or the autopilot. */
public final class PlayScene implements Scene {
    /** Seed of the spike's level (and of the committed replay). */
    public static final long SEED = 2185;

    private final SharedGraphics graphics;
    private final World world = new World(SimConfig.gateLoad(SEED));
    private final FixedStepClock clock = new FixedStepClock(World.STEP_SECONDS, 5);
    private final PlayerInput input = new PlayerInput();
    private final Autopilot autopilot;
    private final Optional<Path> recordTo;
    private final InputRecording.Recorder recorder = new InputRecording.Recorder(SEED);
    private final SparkField sparks = new SparkField(1024);
    private final HaloPlatform halo;
    private final WorldRenderer renderer;
    private final HudPanels hud;
    private final SfxBank sfx;
    private final MusicStreamer music;
    private float time;
    private long frames;
    private long bulletSum;
    private long sparkSum;

    public PlayScene(SharedGraphics graphics, boolean useAutopilot, Optional<Path> recordTo) {
        this.graphics = graphics;
        this.autopilot = useAutopilot ? new Autopilot() : null;
        this.recordTo = recordTo;
        this.halo = new HaloPlatform(Gdx.files, graphics.videoMemory());
        this.renderer = new WorldRenderer(graphics.art(), halo, sparks);
        this.hud = new HudPanels(graphics.art());
        this.sfx = new SfxBank(Gdx.audio, Gdx.files);
        this.music = MusicStreamer.coalitionRising(Gdx.audio, Gdx.files);
    }

    @Override
    public void render(float delta) {
        time += delta;
        for (int steps = clock.advance(delta); steps > 0; steps--) {
            int commands = autopilot != null ? autopilot.commands(world) : input.commands();
            recorder.record(commands);
            world.step(commands);
            presentEvents(world.events());
        }
        sparks.update(delta);

        var batch = graphics.batch();
        graphics.screen().begin(batch);
        renderer.draw(batch, world, clock.alpha(), time * HaloPlatform.BASE_SPEED);
        hud.draw(batch, world.player());
        graphics.screen().end(batch);

        frames++;
        bulletSum += world.bulletCount();
        sparkSum += sparks.count();
    }

    private void presentEvents(SimEvents events) {
        for (int i = 0; i < events.size(); i++) {
            float x = (float) events.x(i);
            float y = (float) events.y(i);
            switch (events.type(i)) {
                case PLAYER_SHOT -> sfx.playThrottled(Sfx.PLAYER_SHOT, 0.25f, 60);
                case ENEMY_SHOT -> sfx.playThrottled(Sfx.ENEMY_SHOT, 0.2f, 90);
                case HIT -> {
                    sparks.burst(x, y, 4, 120);
                    sfx.playThrottled(Sfx.HIT, 0.25f, 50);
                }
                case KILL -> {
                    sparks.burst(x, y, 16, 180);
                    sfx.playThrottled(Sfx.KILL, 0.4f, 40);
                }
                case PLAYER_HIT -> {
                    sparks.burst(x, y, 8, 140);
                    sfx.playThrottled(Sfx.PLAYER_HIT, 0.4f, 100);
                }
            }
        }
    }

    @Override
    public String report() {
        double perFrame = Math.max(1, frames);
        return """
                load              %d enemies, avg %.0f bullets + %.0f sparks = %.0f glowing sprites
                halo platform     %s
                sfx               %d plays, %d without a free source
                simulation        %d steps, score %d, retries %d, state hash %016x
                """.formatted(world.enemyCount(), bulletSum / perFrame, sparkSum / perFrame,
                (bulletSum + sparkSum) / perFrame, halo.describe(), sfx.plays(), sfx.dropouts(), world.tick(),
                world.score(), world.player().retries(), world.stateHash());
    }

    @Override
    public void dispose() {
        recordTo.ifPresent(this::writeRecording);
        music.close();
        sfx.dispose();
        halo.dispose();
    }

    private void writeRecording(Path path) {
        try {
            recorder.writeTo(Files.newBufferedWriter(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Gdx.app.log("replay", "recorded %d steps to %s, final state hash %016x".formatted(world.tick(), path,
                world.stateHash()));
    }
}
