package vanguard.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Optional;
import vanguard.game.display.DisplayModes;
import vanguard.game.input.BackButton;
import vanguard.game.render.PixelScreen;
import vanguard.game.screen.ScreenFlow;
import vanguard.game.screen.TitleScreen;

/** The game: runs the screen flow in the 960x540 pixel screen and toggles the display mode on every screen. */
public final class TerranVanguard extends ApplicationAdapter {
    /** Frames longer than this (a stall, a dragged window) count as this long. */
    private static final float MAX_FRAME_SECONDS = 0.25f;

    private final DisplayModes displayModes;
    private final Optional<BenchRun> bench;
    private SpriteBatch batch;
    private PixelScreen pixelScreen;
    private ScreenFlow screens;

    /**
     * @param displayModes the display mode switcher, set up from the settings file
     * @param benchSeconds exit after this many seconds and log the frame count; 0 runs until quit
     */
    public TerranVanguard(DisplayModes displayModes, double benchSeconds) {
        this.displayModes = displayModes;
        this.bench = benchSeconds > 0 ? Optional.of(new BenchRun(benchSeconds)) : Optional.empty();
    }

    @Override
    public void create() {
        Gdx.app.log("gl", Gdx.gl.glGetString(GL20.GL_RENDERER) + " / " + Gdx.gl.glGetString(GL20.GL_VERSION));
        batch = new SpriteBatch();
        pixelScreen = new PixelScreen();
        screens = new ScreenFlow(new TitleScreen(Gdx.files, Gdx.audio, new BackButton()));
    }

    @Override
    public void render() {
        float frameSeconds = Gdx.graphics.getDeltaTime();
        displayModes.poll();
        screens.update(Math.min(frameSeconds, MAX_FRAME_SECONDS));
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
        pixelScreen.dispose();
        batch.dispose();
    }
}
