package vanguard.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.nio.file.Path;
import java.util.Optional;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRoute;
import vanguard.content.campaign.DebugFit;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.display.DisplayModes;
import vanguard.game.display.Screenshots;
import vanguard.game.render.PixelScreen;
import vanguard.game.screen.LevelScreen;
import vanguard.game.screen.MainMenuScreen;
import vanguard.game.screen.ScreenFlow;
import vanguard.game.settings.Settings;
import vanguard.game.settings.SettingsStore;

/**
 * The game: runs the screen flow in the 960x540 pixel screen, samples the input actions once per
 * frame, toggles the display mode and takes screenshots (F12) on every screen. It starts at the
 * title screen or, for testing, straight in Level 01; in bench mode it exits after the set time.
 * While the window is minimised a level shows its pause menu and nothing is drawn.
 */
public final class TerranVanguard extends ApplicationAdapter {
    /** Frames longer than this (a stall, a dragged window) count as this long. */
    private static final float MAX_FRAME_SECONDS = 0.25f;

    private final DisplayModes displayModes;
    private final Settings settings;
    private final SettingsStore store;
    private final Difficulty difficulty;
    private final float timeScale;
    private final boolean invulnerable;
    private final boolean startLevel;
    private final Optional<DebugFit> debugFit;
    private final int level;
    private final boolean actEnd;
    private final Optional<BenchRun> bench;
    private final SaveSlots saves;
    private final Screenshots screenshots;
    private SpriteBatch batch;
    private PixelScreen pixelScreen;
    private GameServices services;
    private ScreenFlow screens;

    /**
     * @param displayModes the display mode switcher, set up from the settings file
     * @param settings the settings from the settings file
     * @param store writes changed settings back
     * @param difficulty the difficulty the bench flies at, and the difficulty select starts on
     * @param timeScale a debug option: game time runs this many times faster than real time (1 = normal)
     * @param invulnerable a debug option: nothing hits the ship
     * @param startLevel start in Level 01 rather than at the title screen
     * @param debugFit a debug option: the weapons the level start flies
     * @param level a debug option: the level the level start flies
     * @param actEnd a debug option: winning the level start's level ends its act (act summary, outro)
     * @param benchSeconds exit after this many seconds and log the frame count; 0 runs until quit
     * @param saves the save slots: read-only in a debug run, which writes no save
     * @param screenshots the directory the screenshot key writes into
     */
    public TerranVanguard(
            DisplayModes displayModes,
            Settings settings,
            SettingsStore store,
            Difficulty difficulty,
            float timeScale,
            boolean invulnerable,
            boolean startLevel,
            Optional<DebugFit> debugFit,
            int level,
            boolean actEnd,
            double benchSeconds,
            SaveSlots saves,
            Path screenshots) {
        this.displayModes = displayModes;
        this.settings = settings;
        this.store = store;
        this.difficulty = difficulty;
        this.timeScale = timeScale;
        this.invulnerable = invulnerable;
        this.startLevel = startLevel;
        this.debugFit = debugFit;
        this.level = level;
        this.actEnd = actEnd;
        this.bench = benchSeconds > 0 ? Optional.of(new BenchRun(benchSeconds)) : Optional.empty();
        this.saves = saves;
        this.screenshots = new Screenshots(screenshots);
    }

    @Override
    public void create() {
        Gdx.app.log("gl", Gdx.gl.glGetString(GL20.GL_RENDERER) + " / " + Gdx.gl.glGetString(GL20.GL_VERSION));
        batch = new SpriteBatch();
        pixelScreen = new PixelScreen();
        services =
                new GameServices(Gdx.files, Gdx.audio, displayModes, settings, store, difficulty, invulnerable, saves);
        screens = new ScreenFlow(startLevel ? testLevel() : MainMenuScreen.title(services), Gdx.app::exit);
    }

    /** Level 01 (or the {@code --level}) of a new campaign at the launch difficulty, for testing; a debug run, which writes no save. */
    private LevelScreen testLevel() {
        Campaign campaign = DebugFit.startAt(services.campaignRules, difficulty, level);
        debugFit.ifPresent(fit -> fit.applyTo(campaign, services.catalogue));
        if (actEnd) {
            campaign.debugActEnd();
        }
        return new LevelScreen(
                services,
                campaign,
                CampaignRoute.launch(services.content, campaign).orElseThrow());
    }

    @Override
    public void render() {
        float frameSeconds = Gdx.graphics.getDeltaTime();
        displayModes.poll();
        services.input.update(services.devices);
        float seconds = Math.min(frameSeconds, MAX_FRAME_SECONDS);
        services.menu.update(seconds);
        screens.update(seconds * timeScale);
        screenshots.update(seconds);
        pixelScreen.begin(batch);
        screens.draw(batch);
        if (screenshots.requested()) {
            screenshots.capture(pixelScreen, batch);
        }
        screenshots.draw(batch, services.glass, services.fonts);
        pixelScreen.end(batch, services.settings().video());
        if (bench.isPresent() && bench.get().frame(frameSeconds)) {
            Gdx.app.log("bench", bench.get().report());
            Gdx.app.exit();
        }
    }

    /**
     * libGDX calls this when the window is minimised (on Windows and macOS also when a full-screen
     * window loses the focus) and before the game ends: a level opens its pause menu even where the
     * focus stays, and the game stops drawing until {@link #resume} rather than spinning unthrottled
     * (V-sync does not wait for a minimised window). The music thread keeps playing.
     */
    @Override
    public void pause() {
        services.input.interrupt();
        Gdx.graphics.setContinuousRendering(false);
    }

    /** The window is back: draw every frame again. */
    @Override
    public void resume() {
        Gdx.graphics.setContinuousRendering(true);
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
