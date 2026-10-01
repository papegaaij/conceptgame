package vanguard.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vanguard.game.bench.Benchmark;
import vanguard.game.render.PixelScreen;
import vanguard.game.render.ProceduralArt;
import vanguard.game.render.VideoMemory;
import vanguard.game.scene.HaloScene;
import vanguard.game.scene.PlayScene;
import vanguard.game.scene.Scene;
import vanguard.game.scene.SfxScene;
import vanguard.game.scene.SharedGraphics;

/** The spike application: sets up the shared graphics, runs one scene, optionally benchmarks it. */
public final class SpikeGame extends ApplicationAdapter {
    /** The window size: the 960x540 internal screen at 2x. */
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;
    private static final float MAX_FRAME_SECONDS = 0.25f;

    private final GameOptions options;
    private SharedGraphics graphics;
    private Scene scene;
    private Benchmark benchmark;
    private DisplayModes displayModes;
    private double secondsToToggle;

    public SpikeGame(GameOptions options) {
        this.options = options;
    }

    @Override
    public void create() {
        Gdx.app.log("gl", Gdx.gl.glGetString(GL20.GL_RENDERER) + " / " + Gdx.gl.glGetString(GL20.GL_VERSION));
        graphics = new SharedGraphics(new SpriteBatch(8191), new PixelScreen(), new ProceduralArt(), new VideoMemory());
        scene = switch (options.scene()) {
            case PLAY -> new PlayScene(graphics, options.autopilot(), options.recordTo());
            case HALO -> new HaloScene(graphics);
            case SFX -> new SfxScene(graphics);
        };
        displayModes = new DisplayModes(WINDOW_WIDTH, WINDOW_HEIGHT);
        secondsToToggle = options.toggleEvery();
        if (options.benchmark()) {
            benchmark = new Benchmark(options.benchSeconds());
        }
    }

    @Override
    public void render() {
        long now = System.nanoTime();
        displayModes.poll();
        if (options.toggleEvery() > 0 && (secondsToToggle -= Gdx.graphics.getDeltaTime()) <= 0) {
            secondsToToggle += options.toggleEvery();
            displayModes.toggle();
        }
        if (benchmark != null) {
            benchmark.frame(now);
        }
        scene.render(Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_SECONDS));
        if (benchmark != null && benchmark.finished(now)) {
            System.out.println("=== benchmark: scene " + options.scene() + ", vsync " + options.vsync() + ", "
                    + Gdx.graphics.getBackBufferWidth() + "x" + Gdx.graphics.getBackBufferHeight() + ", "
                    + System.getProperty("java.vm.name") + " " + Runtime.version() + "\n"
                    + benchmark.report() + "display toggles: " + displayModes.toggles() + "\n"
                    + scene.report());
            benchmark = null;
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        displayModes.resized();
    }

    @Override
    public void dispose() {
        scene.dispose();
        graphics.batch().dispose();
        graphics.screen().dispose();
        graphics.art().dispose();
    }
}
