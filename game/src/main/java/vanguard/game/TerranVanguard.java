package vanguard.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Optional;
import vanguard.content.Difficulty;
import vanguard.game.display.DisplayModes;
import vanguard.game.input.ActionInput;
import vanguard.game.input.Bindings;
import vanguard.game.input.ControlSettings;
import vanguard.game.input.DeviceState;
import vanguard.game.input.GdxDevices;
import vanguard.game.render.PixelScreen;
import vanguard.game.screen.LevelScreen;
import vanguard.game.screen.ScreenFlow;
import vanguard.game.screen.TitleScreen;

/**
 * The game: runs the screen flow in the 960x540 pixel screen, samples the input actions once per
 * frame and toggles the display mode on every screen. In bench mode it flies Level 01 straight
 * away and exits after the set time.
 */
public final class TerranVanguard extends ApplicationAdapter {
    /** Frames longer than this (a stall, a dragged window) count as this long. */
    private static final float MAX_FRAME_SECONDS = 0.25f;

    private final DisplayModes displayModes;
    private final ControlSettings controls;
    private final Difficulty difficulty;
    private final float timeScale;
    private final Optional<BenchRun> bench;
    private final DeviceState devices = new GdxDevices();
    private SpriteBatch batch;
    private PixelScreen pixelScreen;
    private GameServices services;
    private ScreenFlow screens;

    /**
     * @param displayModes the display mode switcher, set up from the settings file
     * @param controls the control settings from the settings file
     * @param difficulty the difficulty levels are flown at
     * @param timeScale a debug option: game time runs this many times faster than real time (1 = normal)
     * @param benchSeconds fly Level 01, exit after this many seconds and log the frame count;
     *     0 starts at the title screen and runs until quit
     */
    public TerranVanguard(
            DisplayModes displayModes,
            ControlSettings controls,
            Difficulty difficulty,
            float timeScale,
            double benchSeconds) {
        this.displayModes = displayModes;
        this.controls = controls;
        this.difficulty = difficulty;
        this.timeScale = timeScale;
        this.bench = benchSeconds > 0 ? Optional.of(new BenchRun(benchSeconds)) : Optional.empty();
    }

    @Override
    public void create() {
        Gdx.app.log("gl", Gdx.gl.glGetString(GL20.GL_RENDERER) + " / " + Gdx.gl.glGetString(GL20.GL_VERSION));
        batch = new SpriteBatch();
        pixelScreen = new PixelScreen();
        services = new GameServices(Gdx.files, Gdx.audio, new ActionInput(Bindings.defaults()), controls, difficulty);
        screens = new ScreenFlow(bench.isPresent() ? new LevelScreen(services) : new TitleScreen(services));
    }

    @Override
    public void render() {
        float frameSeconds = Gdx.graphics.getDeltaTime();
        displayModes.poll();
        services.input.update(devices);
        screens.update(Math.min(frameSeconds, MAX_FRAME_SECONDS) * timeScale);
        pixelScreen.begin(batch);
        screens.draw(batch);
        pixelScreen.end(batch);
        if (bench.isPresent() && bench.get().frame(frameSeconds)) {
            Gdx.app.log("bench", bench.get().report());
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        displayModes.resized();
    }

    @Override
    public void dispose() {
        displayModes.storeCurrent();
        screens.dispose();
        services.dispose();
        pixelScreen.dispose();
        batch.dispose();
    }
}
