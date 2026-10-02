package vanguard.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import vanguard.game.render.PixelScreen;

/**
 * The glass style of the out-of-game screens (design/ui, the chosen UI kit ui-kit-r08-a, colours
 * from its generator): translucent dark navy panels with a thin metal trim and corner tabs, white
 * and cyan text with a one-pixel shadow, amber for the selection with a gradient bar and a
 * triangular cursor, small rectangular chips, sliders and the confirm dialog.
 *
 * <p>Coordinates are pixels from the screen's top-left corner, y down, as in the concept sheets; a
 * text's y is the top of its capitals.
 */
public final class Glass implements Disposable {
    public static final Color PANEL = rgb(6, 8, 26, 178);
    public static final Color TRIM = rgb(78, 90, 160, 255);
    public static final Color TRIM_LIGHT = rgb(200, 208, 244, 255);
    public static final Color WHITE = rgb(235, 240, 255, 255);
    public static final Color BODY = rgb(200, 212, 240, 255);
    public static final Color LABEL = rgb(150, 170, 205, 255);
    public static final Color DIM = rgb(70, 80, 110, 255);
    public static final Color AMBER = rgb(255, 255, 0, 255);
    public static final Color CYAN = rgb(0, 255, 255, 255);
    public static final Color GREEN = rgb(80, 255, 120, 255);
    public static final Color ALERT = rgb(255, 80, 60, 255);
    public static final Color VALUE = rgb(150, 170, 200, 255);
    static final Color SHADOW = rgb(0, 0, 30, 255);
    static final Color SELECTED_FILL = rgb(60, 50, 0, 255);
    static final Color CHIP_FILL = rgb(12, 14, 36, 255);
    static final Color TRACK = rgb(18, 22, 50, 255);
    static final Color DISABLED = rgb(80, 88, 130, 255);

    /** The selection bar's gradient: amber fading out to the right. */
    private static final int GRADIENT_WIDTH = 128;

    private static final int CHEVRON_WIDTH = 53;
    private static final int CHEVRON_HEIGHT = 17;

    public final Fonts fonts;
    private final TextureRegion pixel;
    private final Texture gradient;
    private final Texture chevron;

    /** @param pixel a white pixel */
    public Glass(Fonts fonts, TextureRegion pixel) {
        this.fonts = fonts;
        this.pixel = pixel;
        gradient = gradientTexture();
        chevron = chevronTexture();
    }

    private static Color rgb(int r, int g, int b, int a) {
        return new Color(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    /** As the concept's {@code lit_bar}: the colour falls from 55 % to 8 %, the alpha from full to none. */
    private static Texture gradientTexture() {
        var pixmap = new Pixmap(GRADIENT_WIDTH, 1, Pixmap.Format.RGBA8888);
        for (int x = 0; x < GRADIENT_WIDTH; x++) {
            float u = x / (GRADIENT_WIDTH - 1f);
            float t = (1 - u * 0.85f) * 0.55f;
            pixmap.drawPixel(x, 0, Color.rgba8888(t, t, 0, (float) Math.pow(1 - u, 0.6)));
        }
        var texture = new Texture(pixmap);
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /** A rank chevron of the difficulty cards, white for tinting. */
    private static Texture chevronTexture() {
        var pixmap = new Pixmap(CHEVRON_WIDTH, CHEVRON_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fillTriangle(0, 10, 26, 0, 26, 6);
        pixmap.fillTriangle(0, 10, 26, 6, 0, 16);
        pixmap.fillTriangle(26, 0, 52, 10, 52, 16);
        pixmap.fillTriangle(26, 0, 52, 16, 26, 6);
        var texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /** A filled rectangle. */
    public void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(pixel, x, PixelScreen.HEIGHT - y - height, width, height);
        batch.setColor(Color.WHITE);
    }

    /** A one-pixel outline inside the rectangle. */
    public void outline(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        fill(batch, colour, x, y, width, 1);
        fill(batch, colour, x, y + height - 1, width, 1);
        fill(batch, colour, x, y, 1, height);
        fill(batch, colour, x + width - 1, y, 1, height);
    }

    /** Darkens the whole screen by {@code share}, 0..1. */
    public void dim(SpriteBatch batch, float share) {
        fill(batch, new Color(0, 0, 0, share), 0, 0, PixelScreen.WIDTH, PixelScreen.HEIGHT);
    }

    /** A glass panel with its metal trim, the light top line and the four corner tabs. */
    public void panel(SpriteBatch batch, float x, float y, float width, float height) {
        panel(batch, x, y, width, height, PANEL.a);
    }

    /** A glass panel of the given opacity, 0..1. */
    public void panel(SpriteBatch batch, float x, float y, float width, float height, float alpha) {
        fill(batch, new Color(PANEL.r, PANEL.g, PANEL.b, alpha), x, y, width, height);
        outline(batch, TRIM, x, y, width, height);
        fill(batch, TRIM_LIGHT, x + 1, y + 1, width - 2, 1);
        for (float tabX : new float[] {x, x + width - 10}) {
            fill(batch, TRIM_LIGHT, tabX, y, 10, 3);
            fill(batch, TRIM_LIGHT, tabX, y + height - 3, 10, 3);
        }
    }

    /** A section header: amber label caps with a trim line running on to {@code right}. */
    public void header(SpriteBatch batch, String text, float x, float right, float y) {
        BitmapFont font = fonts.label;
        shadowed(batch, font, text, AMBER, x, y);
        float lineX = x + Fonts.width(font, text) + 6;
        fill(batch, TRIM, lineX, y + 3, right - lineX, 1);
    }

    /** Text with its capitals' top at {@code y}. */
    public void text(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y) {
        font.setColor(colour);
        font.draw(batch, text, x, PixelScreen.HEIGHT - y);
        font.setColor(Color.WHITE);
    }

    /** Text with the kit's shadow: the same glyphs one pixel down and right in near-black navy. */
    public void shadowed(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y) {
        text(batch, font, text, SHADOW, x + 1, y + 1);
        text(batch, font, text, colour, x, y);
    }

    public void centred(SpriteBatch batch, BitmapFont font, String text, Color colour, float centreX, float y) {
        shadowed(batch, font, text, colour, Math.round(centreX - Fonts.width(font, text) / 2f), y);
    }

    public void right(SpriteBatch batch, BitmapFont font, String text, Color colour, float rightX, float y) {
        shadowed(batch, font, text, colour, rightX - Fonts.width(font, text), y);
    }

    /** The selection highlight: the amber gradient bar with a bright top and left edge. */
    public void selection(SpriteBatch batch, float x, float y, float width, float height) {
        batch.draw(gradient, x, PixelScreen.HEIGHT - y - height, width, height);
        fill(batch, AMBER, x, y, Math.round(width * 2 / 3f), 1);
        fill(batch, AMBER, x, y, 1, height);
    }

    /** The triangular cursor pointing right, its tip {@code size} px right of {@code x}, centred on {@code y}. */
    public void cursor(SpriteBatch batch, float x, float y, int size) {
        for (int i = 0; i < size; i++) {
            fill(batch, AMBER, x + i, y - (size - i), 1, 2 * (size - i) + 1);
        }
    }

    /**
     * A menu item: amber on the selection bar with the cursor when selected, dim when disabled.
     *
     * @param barX the left edge of the selection bar, usually the panel's inner edge
     * @param barWidth the width of the bar
     * @param height the height of the bar around the text
     */
    public void item(
            SpriteBatch batch,
            BitmapFont font,
            String label,
            float x,
            float y,
            float barX,
            float barWidth,
            float height,
            boolean selected,
            boolean enabled) {
        float capHeight = font.getCapHeight();
        if (selected) {
            float barY = Math.round(y + capHeight / 2 - height / 2);
            selection(batch, barX, barY, barWidth, height);
            int size = Math.max(4, Math.round(capHeight / 3));
            cursor(batch, Math.max(barX + 2, x - size - 4), Math.round(y + capHeight / 2), size);
        }
        Color colour = selected ? AMBER : enabled ? WHITE : DISABLED;
        shadowed(batch, font, label, colour, x, y);
    }

    /** A chip: amber when on, dim when it cannot be chosen. */
    public void chip(SpriteBatch batch, String text, float x, float y, float width, float height, boolean on) {
        chip(batch, fonts.label, text, x, y, width, height, on, true);
    }

    public void chip(
            SpriteBatch batch,
            BitmapFont font,
            String text,
            float x,
            float y,
            float width,
            float height,
            boolean on,
            boolean enabled) {
        fill(batch, on ? SELECTED_FILL : CHIP_FILL, x, y, width, height);
        outline(batch, on ? AMBER : enabled ? TRIM : DIM, x, y, width, height);
        Color colour = on ? AMBER : enabled ? BODY : DISABLED;
        float textY = Math.round(y + (height - font.getCapHeight()) / 2);
        centred(batch, font, text, colour, x + width / 2, textY);
    }

    /** A row of chips, each as wide as its text plus padding; returns the right edge of the last one. */
    public float chips(SpriteBatch batch, String[] labels, int on, float x, float y, float height) {
        float chipX = x;
        for (int i = 0; i < labels.length; i++) {
            float width = Fonts.width(fonts.label, labels[i]) + 16;
            chip(batch, labels[i], chipX, y, width, height, i == on);
            chipX += width + 6;
        }
        return chipX - 6;
    }

    /** A slider: the dark track, the amber fill to {@code share} (0..1), the handle and the value after it. */
    public void slider(SpriteBatch batch, float x, float y, float width, double share, String value) {
        float filled = (float) (width * Math.clamp(share, 0, 1));
        fill(batch, TRACK, x, y, width, 10);
        outline(batch, TRIM, x, y, width, 10);
        fill(batch, AMBER, x + 1, y + 2, Math.max(0, filled - 2), 6);
        fill(batch, TRIM_LIGHT, x + filled - 3, y - 3, 6, 16);
        shadowed(batch, fonts.label, value, AMBER, x + width + 12, y + 2);
    }

    /** The rank chevron of a difficulty card, centred on {@code centreX}. */
    public void chevron(SpriteBatch batch, Color colour, float centreX, float y) {
        batch.setColor(colour);
        batch.draw(
                chevron,
                centreX - CHEVRON_WIDTH / 2f,
                PixelScreen.HEIGHT - y - CHEVRON_HEIGHT,
                CHEVRON_WIDTH,
                CHEVRON_HEIGHT);
        batch.setColor(Color.WHITE);
    }

    /** The key hints at the bottom of a screen, centred. */
    public void hints(SpriteBatch batch, String hints) {
        centred(batch, fonts.label, hints, DIM, PixelScreen.WIDTH / 2f, PixelScreen.HEIGHT - 22);
    }

    /** The confirm dialog of the UI kit, centred on the screen over a dimmed backdrop. */
    public void dialog(SpriteBatch batch, Dialog dialog) {
        BitmapFont body = fonts.body;
        int width = Math.max(
                360, Math.max(Fonts.width(body, dialog.question()), Fonts.width(fonts.label, dialog.detail())) + 40);
        int height = 132;
        float x = Math.round((PixelScreen.WIDTH - width) / 2f);
        float y = Math.round((PixelScreen.HEIGHT - height) / 2f);
        dim(batch, 0.4f);
        panel(batch, x, y, width, height, 0.94f);
        header(batch, "CONFIRM", x + 12, x + width - 12, y + 12);
        shadowed(batch, body, dialog.question(), WHITE, x + 12, y + 32);
        shadowed(batch, fonts.label, dialog.detail(), LABEL, x + 12, y + 56);
        float buttonWidth = (width - 36) / 2f;
        float buttonY = y + height - 42;
        chip(batch, body, dialog.yes(), x + 12, buttonY, buttonWidth, 26, dialog.yesSelected(), true);
        chip(batch, body, dialog.no(), x + 24 + buttonWidth, buttonY, buttonWidth, 26, !dialog.yesSelected(), true);
    }

    @Override
    public void dispose() {
        gradient.dispose();
        chevron.dispose();
    }
}
