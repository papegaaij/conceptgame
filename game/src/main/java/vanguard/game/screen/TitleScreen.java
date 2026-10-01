package vanguard.game.screen;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Optional;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.input.BackButton;
import vanguard.game.render.PixelScreen;

/**
 * The title screen (design/ui/main-menu): the logo over black with the title theme. For now Back
 * quits the game; the main menu follows in a later milestone.
 */
public final class TitleScreen implements GameScreen {
    private static final float MUSIC_VOLUME = 0.6f;

    private final Texture logo;
    private final BackButton back;
    private final Optional<MusicStreamer> music;

    public TitleScreen(Files files, Audio audio, BackButton back) {
        this.back = back;
        logo = new Texture(files.internal("ui/logo.png"));
        logo.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        music = MusicStreamer.play(audio, files.internal("music/title-theme.ogg"), MUSIC_VOLUME);
    }

    @Override
    public GameScreen update(float seconds) {
        if (back.pressed()) {
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
    }

    @Override
    public void dispose() {
        music.ifPresent(MusicStreamer::close);
        logo.dispose();
    }
}
