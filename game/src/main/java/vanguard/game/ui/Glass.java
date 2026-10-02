package vanguard.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import vanguard.game.render.PixelScreen;
import vanguard.game.render.Sprites;

/**
 * The glass style of the out-of-game screens (design/ui, the chosen UI kit ui-kit-r08-a) in its
 * production pieces (tools/art/ui_kit.py, {@code ui/} on the sprite pages): translucent navy glass
 * panels with a bevelled metal trim and corner tabs, white and cyan text with a one-pixel shadow,
 * amber for the selection with its fading bar and faceted cursor, chips, tabs, slider, list rows,
 * the confirm dialog with its holographic brackets, the key-hint plate and the hangar's holographic
 * callouts. Nine-patches stretch to their rectangle; nothing is allocated per frame.
 *
 * <p>Coordinates are pixels from the screen's top-left corner, y down, as in the concept sheets; a
 * text's y is the top of its capitals.
 */
public final class Glass {
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
    /** An unlit bar cell or pip. */
    public static final Color UNLIT = rgb(26, 30, 56, 255);

    static final Color SHADOW = rgb(0, 0, 30, 255);
    static final Color DISABLED = rgb(80, 88, 130, 255);
    /** A locked callout is drawn this much darker. */
    private static final Color LOCKED = rgb(115, 115, 150, 255);

    /** The key-hint plate reaches this far beyond its text on either side. */
    private static final int HINT_PAD = 10;

    private static final int HINT_Y = PixelScreen.HEIGHT - 22;

    public final Fonts fonts;
    private final TextureRegion pixel;
    private final NinePatch glass;
    private final NinePatch frame;
    private final NinePatch frameOn;
    private final NinePatch dialogFrame;
    private final NinePatch inset;
    private final NinePatch rule;
    private final NinePatch selection;
    private final NinePatch chip;
    private final NinePatch chipOn;
    private final NinePatch chipOff;
    private final NinePatch tab;
    private final NinePatch tabOn;
    private final NinePatch bar;
    private final NinePatch row;
    private final NinePatch hint;
    private final NinePatch tag;
    private final NinePatch callout;
    private final NinePatch calloutOn;
    private final TextureRegion cursorSmall;
    private final TextureRegion cursor;
    private final TextureRegion cursorLarge;
    private final TextureRegion knob;
    private final TextureRegion diamond;
    private final TextureRegion chevron;
    private final TextureRegion scrollUp;
    private final TextureRegion scrollDown;

    public Glass(Fonts fonts, Sprites sprites) {
        this.fonts = fonts;
        pixel = sprites.pixel;
        glass = sprites.patch("ui/glass");
        frame = sprites.patch("ui/frame");
        frameOn = sprites.patch("ui/frame-on");
        dialogFrame = sprites.patch("ui/dialog");
        inset = sprites.patch("ui/inset");
        rule = sprites.patch("ui/rule");
        selection = sprites.patch("ui/selection");
        chip = sprites.patch("ui/chip");
        chipOn = sprites.patch("ui/chip-on");
        chipOff = sprites.patch("ui/chip-off");
        tab = sprites.patch("ui/tab");
        tabOn = sprites.patch("ui/tab-on");
        bar = sprites.patch("ui/bar");
        row = sprites.patch("ui/row");
        hint = sprites.patch("ui/hint");
        tag = sprites.patch("ui/tag");
        callout = sprites.patch("ui/callout");
        calloutOn = sprites.patch("ui/callout-on");
        cursorSmall = sprites.region("ui/cursor-small");
        cursor = sprites.region("ui/cursor");
        cursorLarge = sprites.region("ui/cursor-large");
        knob = sprites.region("ui/knob");
        diamond = sprites.region("ui/diamond");
        chevron = sprites.region("ui/chevron");
        scrollUp = sprites.region("ui/scroll-up");
        scrollDown = sprites.region("ui/scroll-down");
    }

    private static Color rgb(int r, int g, int b, int a) {
        return new Color(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    /** A nine-patch over the rectangle. */
    private static void draw(SpriteBatch batch, NinePatch patch, float x, float y, float width, float height) {
        patch.draw(batch, x, PixelScreen.HEIGHT - y - height, width, height);
    }

    /** A piece at its size with its top-left corner at (x, y). */
    private static void draw(SpriteBatch batch, TextureRegion region, float x, float y) {
        batch.draw(region, x, PixelScreen.HEIGHT - y - region.getRegionHeight());
    }

    /** A filled rectangle: lines, grids, ticks. */
    public void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(pixel, x, PixelScreen.HEIGHT - y - height, width, height);
        batch.setColor(Color.WHITE);
    }

    /** A one-pixel outline inside the rectangle: a focus marker. */
    public void outline(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        fill(batch, colour, x, y, width, 1);
        fill(batch, colour, x, y + height - 1, width, 1);
        fill(batch, colour, x, y, 1, height);
        fill(batch, colour, x + width - 1, y, 1, height);
    }

    /** Darkens the whole screen by {@code share}, 0..1. */
    public void dim(SpriteBatch batch, float share) {
        batch.setColor(0, 0, 0, share);
        batch.draw(pixel, 0, 0, PixelScreen.WIDTH, PixelScreen.HEIGHT);
        batch.setColor(Color.WHITE);
    }

    /** A glass panel with its metal trim and the four corner tabs. */
    public void panel(SpriteBatch batch, float x, float y, float width, float height) {
        panel(batch, x, y, width, height, PANEL.a);
    }

    /** A glass panel of the given opacity, 0..1. */
    public void panel(SpriteBatch batch, float x, float y, float width, float height, float alpha) {
        panel(batch, x, y, width, height, alpha, false);
    }

    /** A glass panel; {@code on} gives it the amber trim of a selected card. */
    public void panel(SpriteBatch batch, float x, float y, float width, float height, float alpha, boolean on) {
        batch.setColor(1, 1, 1, alpha);
        draw(batch, glass, x, y, width, height);
        batch.setColor(Color.WHITE);
        draw(batch, on ? frameOn : frame, x, y, width, height);
    }

    /** The metal trim alone round a rectangle: a portrait's frame. */
    public void frame(SpriteBatch batch, float x, float y, float width, float height) {
        draw(batch, frame, x, y, width, height);
    }

    /** A recessed dark glass area with its bezel: a box, a trough, a track. */
    public void inset(SpriteBatch batch, float x, float y, float width, float height) {
        draw(batch, inset, x, y, width, height);
    }

    /** A lit bar cell in {@code colour}: a fill, a pip, a bullet; nothing narrower than its two ends. */
    public void bar(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        if (width < bar.getLeftWidth() + bar.getRightWidth()) {
            return;
        }
        batch.setColor(colour);
        draw(batch, bar, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    /** A list row's faint glass band. */
    public void row(SpriteBatch batch, float x, float y, float width, float height) {
        draw(batch, row, x, y, width, height);
    }

    /** A trim line from {@code x} to {@code right} with its top at {@code y}, ending in a cap. */
    public void rule(SpriteBatch batch, float x, float right, float y) {
        draw(batch, rule, x, y, right - x, rule.getTotalHeight());
    }

    /** A section header: amber label caps with a trim line running on to {@code right}. */
    public void header(SpriteBatch batch, String text, float x, float right, float y) {
        BitmapFont font = fonts.label;
        shadowed(batch, font, text, AMBER, x, y);
        rule(batch, x + Fonts.width(font, text) + 6, right, y + 2);
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

    /** Shadowed text cut to the characters that fit {@code width} (the fonts are monospaced). */
    public void shadowed(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width) {
        int end = Math.clamp((int) (width / (Fonts.advance(font) * font.getScaleX())), 0, text.length());
        font.setColor(SHADOW);
        font.draw(batch, text, x + 1, PixelScreen.HEIGHT - y - 1, 0, end, 0, Align.left, false);
        font.setColor(colour);
        font.draw(batch, text, x, PixelScreen.HEIGHT - y, 0, end, 0, Align.left, false);
        font.setColor(Color.WHITE);
    }

    public void centred(SpriteBatch batch, BitmapFont font, String text, Color colour, float centreX, float y) {
        shadowed(batch, font, text, colour, Math.round(centreX - Fonts.width(font, text) / 2f), y);
    }

    public void right(SpriteBatch batch, BitmapFont font, String text, Color colour, float rightX, float y) {
        shadowed(batch, font, text, colour, rightX - Fonts.width(font, text), y);
    }

    /**
     * The selection highlight: the amber bar with lit top and left edges, fading out over its last
     * 96 px (so it is at least 99 px wide).
     */
    public void selection(SpriteBatch batch, float x, float y, float width, float height) {
        draw(batch, selection, x, y, width, height);
    }

    /** The cursor of a font's menu items, pointing right, its left edge at {@code x}, centred on {@code y}. */
    public void cursor(SpriteBatch batch, BitmapFont font, float x, float y) {
        TextureRegion region = cursorFor(font);
        draw(batch, region, x, y - region.getRegionHeight() / 2);
    }

    private TextureRegion cursorFor(BitmapFont font) {
        if (font == fonts.heading) {
            return cursorLarge;
        }
        return font == fonts.body ? cursor : cursorSmall;
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
            int depth = cursorFor(font).getRegionWidth();
            cursor(batch, font, Math.max(barX + 2, x - depth - 4), Math.round(y + capHeight / 2));
        }
        Color colour = selected ? AMBER : enabled ? WHITE : DISABLED;
        shadowed(batch, font, label, colour, x, y);
    }

    /** A chip's or tile's body without text: amber when on, dim when it cannot be chosen. */
    public void button(SpriteBatch batch, float x, float y, float width, float height, boolean on, boolean enabled) {
        draw(batch, on ? chipOn : enabled ? chip : chipOff, x, y, width, height);
    }

    /** A chip: amber when on. */
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
        button(batch, x, y, width, height, on, enabled);
        label(batch, font, text, x, y, width, height, on, enabled);
    }

    /** A tab of a tabbed panel: the active one amber with its lit underline. */
    public void tab(
            SpriteBatch batch, BitmapFont font, String text, float x, float y, float width, float height, boolean on) {
        draw(batch, on ? tabOn : tab, x, y, width, height);
        label(batch, font, text, x, y, width, height, on, true);
    }

    private void label(
            SpriteBatch batch,
            BitmapFont font,
            String text,
            float x,
            float y,
            float width,
            float height,
            boolean on,
            boolean enabled) {
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

    /** A small tag (NEW) in {@code colour} with its label text. */
    public void tag(SpriteBatch batch, String text, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        draw(batch, tag, x, y, width, height);
        batch.setColor(Color.WHITE);
        shadowed(batch, fonts.label, text, colour, x + 3, y + 2);
    }

    /** A slider: the recessed track, the amber fill to {@code share} (0..1), the knob and the value after it. */
    public void slider(SpriteBatch batch, float x, float y, float width, double share, String value) {
        float filled = Math.round(width * Math.clamp(share, 0, 1));
        inset(batch, x, y, width, 10);
        bar(batch, AMBER, x + 2, y + 2, Math.min(filled, width - 2) - 2, 6);
        draw(batch, knob, x + filled - knob.getRegionWidth() / 2, y - 3);
        shadowed(batch, fonts.label, value, AMBER, x + width + 12, y + 2);
    }

    /** The rank chevron of a difficulty card, centred on {@code centreX}. */
    public void chevron(SpriteBatch batch, Color colour, float centreX, float y) {
        batch.setColor(colour);
        draw(batch, chevron, Math.round(centreX - chevron.getRegionWidth() / 2f), y);
        batch.setColor(Color.WHITE);
    }

    /** The ◆ trait-match marker with its top-left corner at (x, y). */
    public void diamond(SpriteBatch batch, float x, float y) {
        draw(batch, diamond, x, y);
    }

    /** A list's scroll markers, right-aligned at {@code right} from {@code y} down: up if rows are above, down if below. */
    public void scrollMarkers(SpriteBatch batch, float right, float y, boolean above, boolean below) {
        float x = right - scrollDown.getRegionWidth();
        if (above) {
            draw(batch, scrollUp, x, y);
        }
        if (below) {
            draw(batch, scrollDown, x, y + 7);
        }
    }

    /** A holographic callout of the schematic: cyan, amber when selected. */
    public void callout(SpriteBatch batch, float x, float y, float width, float height, boolean selected) {
        draw(batch, selected ? calloutOn : callout, x, y, width, height);
    }

    /** A callout of a slot that is not open yet, darkened. */
    public void lockedCallout(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(LOCKED);
        draw(batch, callout, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    /** The key hints at the bottom of a screen, centred on their plate. */
    public void hints(SpriteBatch batch, String hints) {
        float width = Fonts.width(fonts.label, hints) + 2 * HINT_PAD;
        draw(batch, hint, Math.round((PixelScreen.WIDTH - width) / 2), HINT_Y - 3, width, 18);
        centred(batch, fonts.label, hints, DIM, PixelScreen.WIDTH / 2f, HINT_Y);
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
        batch.setColor(1, 1, 1, 0.94f);
        draw(batch, glass, x, y, width, height);
        batch.setColor(Color.WHITE);
        draw(batch, dialogFrame, x, y, width, height);
        header(batch, "CONFIRM", x + 12, x + width - 12, y + 12);
        shadowed(batch, body, dialog.question(), WHITE, x + 12, y + 32);
        shadowed(batch, fonts.label, dialog.detail(), LABEL, x + 12, y + 56);
        float buttonWidth = (width - 36) / 2f;
        float buttonY = y + height - 42;
        chip(batch, body, dialog.yes(), x + 12, buttonY, buttonWidth, 26, dialog.yesSelected(), true);
        chip(batch, body, dialog.no(), x + 24 + buttonWidth, buttonY, buttonWidth, 26, !dialog.yesSelected(), true);
    }
}
