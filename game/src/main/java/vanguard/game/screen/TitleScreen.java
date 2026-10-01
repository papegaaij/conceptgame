package vanguard.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import java.util.Optional;
import vanguard.game.GameServices;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.input.Action;
import vanguard.game.render.PixelScreen;

/**
 * The title screen (design/ui/main-menu): the logo over black with the title theme. Confirm
 * (Enter / A) starts the M1 test sortie and Back quits; the main menu follows in M3.
 */
public final class TitleScreen implements GameScreen {
    private static final float MUSIC_VOLUME = 0.6f;
    private static final String PROMPT = "PRESS ENTER OR A";

    private final GameServices services;
    private final Texture logo;
    private final Optional<MusicStreamer> music;

    public TitleScreen(GameServices services) {
        this.services = services;
        logo = new Texture(services.files.internal("ui/logo.png"));
        logo.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        music = MusicStreamer.play(services.audio, services.files.internal("music/title-theme.ogg"), MUSIC_VOLUME);
    }

    @Override
    public GameScreen update(float seconds) {
        if (services.input.pressed(Action.MENU_CONFIRM)) {
            return new FlightScreen(services);
        }
        if (services.input.pressed(Action.MENU_BACK)) {
            Gdx.app.exit();
        }
        return this;
    }

    @Override
    public void draw(SpriteBatch batch) {
        float scale =
                Math.min((float) PixelScreen.WIDTH / logo.getWidth(), (float) PixelScreen.HEIGHT / logo.getHeight());
        float width = logo.getWidth() * scale;
        float height = logo.getHeight() * scale;
        batch.draw(logo, (PixelScreen.WIDTH - width) / 2, (PixelScreen.HEIGHT - height) / 2, width, height);
        services.font.draw(batch, PROMPT, 0, 40, PixelScreen.WIDTH, Align.center, false);
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
        logo.dispose();
    }
}
