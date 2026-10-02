package vanguard.game;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.game.audio.SfxBank;
import vanguard.game.input.ActionInput;
import vanguard.game.input.ControlSettings;
import vanguard.game.render.FlashShader;
import vanguard.game.render.Sprites;

/**
 * What every screen shares for the whole run: files and audio, the input actions, the control
 * settings, the game's content (the design data), the sprite atlases, the sound effects, the HUD
 * font and the flash shader.
 */
public final class GameServices implements Disposable {
    public final Files files;
    public final Audio audio;
    public final ActionInput input;
    public final ControlSettings controls;
    public final Content content;
    public final Sprites sprites;
    public final SfxBank sfx;
    /** libGDX's built-in font, standing in until the UI kit's bitmap fonts exist. */
    public final BitmapFont font;

    public final FlashShader flash;

    GameServices(Files files, Audio audio, ActionInput input, ControlSettings controls) {
        this.files = files;
        this.audio = audio;
        this.input = input;
        this.controls = controls;
        content = ContentLoader.fromClasspath();
        sprites = new Sprites(files);
        sfx = new SfxBank(audio, files);
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        flash = new FlashShader();
    }

    @Override
    public void dispose() {
        flash.dispose();
        font.dispose();
        sfx.dispose();
        sprites.dispose();
    }
}
