package vanguard.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Optional;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRoute;
import vanguard.content.campaign.DebugFit;
import vanguard.content.campaign.SaveSlots;
import vanguard.game.display.DisplayModes;
import vanguard.game.render.PixelScreen;
import vanguard.game.screen.LevelScreen;
import vanguard.game.screen.MainMenuScreen;
import vanguard.game.screen.ScreenFlow;
import vanguard.game.settings.Settings;
import vanguard.game.settings.SettingsStore;

/**
 * The game: runs the screen flow in the 960x540 pixel screen, samples the input actions once per
 * frame and toggles the display mode on every screen. It starts at the title screen or, for
 * testing, straight in Level 01; in bench mode it exits after the set time.
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
    private final Optional<BenchRun> bench;
    private final SaveSlots saves;
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
     * @param benchSeconds exit after this many seconds and log the frame count; 0 runs until quit
     * @param saves the save slots
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
            double benchSeconds,
            SaveSlots saves) {
        this.displayModes = displayModes;
        this.settings = settings;
        this.store = store;
        this.difficulty = difficulty;
        this.timeScale = timeScale;
        this.invulnerable = invulnerable;
        this.startLevel = startLevel;
        this.debugFit = debugFit;
        this.level = level;
        this.bench = benchSeconds > 0 ? Optional.of(new BenchRun(benchSeconds)) : Optional.empty();
        this.saves = saves;
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

    /** Level 01 (or the {@code --level}) of a new campaign at the launch difficulty, for testing; nothing is saved before its hangar. */
    private LevelScreen testLevel() {
        Campaign campaign = DebugFit.startAt(services.campaignRules, difficulty, level);
        debugFit.ifPresent(fit -> fit.applyTo(campaign, services.catalogue));
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
        pixelScreen.begin(batch);
        screens.draw(batch);
        pixelScreen.end(batch, services.settings().video());
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
