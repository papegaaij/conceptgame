package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import vanguard.game.ui.Fonts;

/**
 * The placeholder HUD's drawing pieces in the metal style of the chosen HUD A (colours sampled
 * from hud-r08-a): panels, labels, LCD wells, readouts and bars, drawn with a white pixel and the
 * UI kit's bitmap fonts: 8x12 for the plates' labels, the radio subtitles and the control prompts
 * (a radio line of 22 characters must fit the 196 px of a well), 10x20 for the readouts.
 */
final class HudKit {
    static final int PANEL_WIDTH = PixelScreen.PLAY_FIELD_X;
    static final int TOP = PixelScreen.HEIGHT;
    static final int INSET = 16;
    static final int INNER_WIDTH = PANEL_WIDTH - 2 * INSET;
    static final int LABEL_WIDTH = 120;

    static final Color METAL = Color.valueOf("5C669A");
    static final Color METAL_LIGHT = Color.valueOf("8A96D0");
    static final Color METAL_DARK = Color.valueOf("121632");
    static final Color LCD = Color.valueOf("06061A");
    static final Color LABEL = Color.valueOf("E0E6FF");
    static final Color READOUT = Color.valueOf("40FF80");
    static final Color AMBER = Color.valueOf("FFE04A");
    static final Color ALERT = Color.valueOf("FF5040");

    final TextureRegion pixel;
    /** The 8x12 label font. */
    final BitmapFont small;
    /** The 10x20 body font. */
    final BitmapFont body;

    HudKit(TextureRegion pixel, Fonts fonts) {
        this.pixel = pixel;
        this.small = fonts.label;
        this.body = fonts.body;
    }

    void panel(SpriteBatch batch, int x) {
        fill(batch, METAL_DARK, x, 0, PANEL_WIDTH, TOP);
        fill(batch, METAL, x + 4, 4, PANEL_WIDTH - 8, TOP - 8);
        fill(batch, METAL_LIGHT, x + 4, TOP - 5, PANEL_WIDTH - 8, 1);
        fill(batch, METAL_LIGHT, x + 4, 4, 1, TOP - 8);
    }

    /** A label plate with its text's top at {@code y}. */
    void label(SpriteBatch batch, String text, int x, int y) {
        fill(batch, METAL_DARK, x - 4, y - 18, LABEL_WIDTH, 22);
        text(batch, small, text, LABEL, x, y - 4);
    }

    /** An LCD well of {@code width} x {@code height} with its bottom at {@code y}. */
    void lcd(SpriteBatch batch, int x, int y, int width, int height) {
        fill(batch, METAL_DARK, x - 2, y - 2, width + 4, height + 4);
        fill(batch, LCD, x, y, width, height);
    }

    /** Text with its capitals' top at {@code y}. */
    void text(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y) {
        font.setColor(colour);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    /** Text left-aligned in {@code width}, cut off at its end if it is longer. */
    void text(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width) {
        fitted(batch, font, text, colour, x, y, width, Align.left);
    }

    /** Text right-aligned in {@code width}, cut off at its end if it is longer. */
    void textRight(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width) {
        fitted(batch, font, text, colour, x, y, width, Align.right);
    }

    private static void fitted(
            SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width, int align) {
        font.setColor(colour);
        font.draw(batch, text, x, y, 0, text.length(), width, align, false, "");
        font.setColor(Color.WHITE);
    }

    /** A bar with its bottom at {@code y}, filled to {@code share} (0..1). */
    void bar(SpriteBatch batch, Color full, Color empty, float x, float y, float width, float height, double share) {
        fill(batch, METAL_DARK, x - 2, y - 2, width + 4, height + 4);
        fill(batch, empty, x, y, width, height);
        fill(batch, full, x, y, (float) (width * Math.clamp(share, 0, 1)), height);
    }

    void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }
}
