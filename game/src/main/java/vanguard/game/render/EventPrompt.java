package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.game.ui.Fonts;
import vanguard.sim.PlayField;

/**
 * M5 part F, an event prompt in the play field (design/ui/hud, in the play field: Level 12's
 * {@code SHAKE OFF} · {@code LEFT-RIGHT}): a one-line warning box below the ship, where the player
 * looks while a Lamprey drains the shield, in the wave banner's look (amber on a dark band with an
 * amber frame pulsing with the edge warnings), sized to its line. It shows on every latch that lasts
 * at least {@value #MIN_LATCH} s, for at most its seconds each time, until the attempt's first
 * successful shake-off; after that never again in the attempt. A latch lasts from the first Lamprey
 * latching on until none is latched. Drawn in code; geometry in play-field pixels, y up.
 */
public final class EventPrompt {
    /** A latch shorter than this (a Lamprey shot off at once) raises no prompt. */
    static final double MIN_LATCH = 0.3;
    /** The box's height: the body font's capitals (11 px) with 4 px above and below. */
    static final int HEIGHT = 19;
    /** The text keeps this far from the box's ends; the action and keys this far apart. */
    static final int TEXT_INSET = 8;

    static final int GAP = 16;
    /** The box's middle this far below the ship's centre: under its hull and a Lamprey at its tail. */
    static final int BELOW_SHIP = 80;
    /** The box's middle at least this far above the play field's bottom edge. */
    static final int MIN_Y = 16;

    private static final Color AMBER = EdgeWarnings.WARN;
    private static final Color KEYS = HudKit.LABEL;
    private static final Color BAND = new Color(20 / 255f, 14 / 255f, 4 / 255f, 200 / 255f);
    private static final Color SHADOW = new Color(40 / 255f, 25 / 255f, 0, 1);

    private final TextureRegion pixel;
    private final BitmapFont font;
    private final String action;
    private final String keys;
    private final double seconds;
    /** When the running latch started, on the real clock; NaN while nothing is latched. */
    private double latchAt = Double.NaN;
    /** Whether the attempt's first shake-off is done: no prompt any more. */
    private boolean shaken;

    /**
     * @param pixel a white pixel
     * @param font the text's font (the 10x20 body font)
     * @param action the prompt's action ({@code SHAKE OFF})
     * @param keys its keys ({@code LEFT-RIGHT})
     * @param seconds the longest it shows each time
     */
    public EventPrompt(TextureRegion pixel, BitmapFont font, String action, String keys, double seconds) {
        this.pixel = pixel;
        this.font = font;
        this.action = action;
        this.keys = keys;
        this.seconds = seconds;
    }

    /** A Lamprey latched on at real time {@code real}: a latch starts unless one runs. */
    public void latched(double real) {
        if (Double.isNaN(latchAt)) {
            latchAt = real;
        }
    }

    /** The shake-off threw the latched Lampreys off: the prompt is done for the attempt. */
    public void shaken() {
        shaken = true;
        latchAt = Double.NaN;
    }

    /** After a step's events: with nothing latched ({@code latchedNow} 0) the latch is over. */
    public void step(int latchedNow) {
        if (latchedNow == 0) {
            latchAt = Double.NaN;
        }
    }

    /** A new attempt: no latch, no shake-off yet. */
    public void reset() {
        latchAt = Double.NaN;
        shaken = false;
    }

    /** Whether it shows at real time {@code real}. */
    public boolean showing(double real) {
        return shows(real - latchAt, seconds, shaken);
    }

    /**
     * Whether a prompt shows {@code age} s into a latch (NaN: none runs): from {@value #MIN_LATCH} s on,
     * for at most {@code seconds}, and never after the attempt's first shake-off.
     */
    static boolean shows(double age, double seconds, boolean shaken) {
        return !shaken && age >= MIN_LATCH && age < MIN_LATCH + seconds;
    }

    /** The text's width: the action, a gap and the keys. */
    static int textWidth(BitmapFont font, String action, String keys) {
        return Fonts.width(font, action) + GAP + Fonts.width(font, keys);
    }

    /** The box's width for its line. */
    static int width(int textWidth) {
        return textWidth + 2 * TEXT_INSET;
    }

    /** The box's middle above the play field's bottom edge for the ship's centre at {@code shipY}. */
    static int centreY(double shipY) {
        return (int) Math.max(MIN_Y, Math.round(shipY - BELOW_SHIP));
    }

    /**
     * Draws it at real time {@code real} below the ship's centre at {@code shipY} (play-field y), centred
     * across the play field so the shake's sideways jolts do not move it.
     */
    public void draw(SpriteBatch batch, double shipY, double real) {
        if (!showing(real)) {
            return;
        }
        double age = real - latchAt - MIN_LATCH;
        int text = textWidth(font, action, keys);
        int boxWidth = width(text);
        float x0 = Math.round(PixelScreen.PLAY_FIELD_X + (PlayField.WIDTH - boxWidth) / 2f);
        int middle = centreY(shipY);
        int bottom = middle - HEIGHT / 2;
        batch.setColor(BAND);
        batch.draw(pixel, x0, bottom, boxWidth, HEIGHT);
        float frame = EdgeWarnings.pulse(age);
        batch.setColor(AMBER.r, AMBER.g, AMBER.b, frame);
        batch.draw(pixel, x0, bottom, boxWidth, 1);
        batch.draw(pixel, x0, bottom + HEIGHT - 1, boxWidth, 1);
        batch.draw(pixel, x0, bottom, 1, HEIGHT);
        batch.draw(pixel, x0 + boxWidth - 1, bottom, 1, HEIGHT);
        batch.setColor(Color.WHITE);
        float x = x0 + TEXT_INSET;
        float top = Math.round(middle + font.getCapHeight() / 2);
        font.setColor(SHADOW);
        font.draw(batch, action, x + 1, top - 1);
        font.setColor(AMBER);
        font.draw(batch, action, x, top);
        float keysX = x + Fonts.width(font, action) + GAP;
        font.setColor(SHADOW);
        font.draw(batch, keys, keysX + 1, top - 1);
        font.setColor(KEYS);
        font.draw(batch, keys, keysX, top);
        font.setColor(Color.WHITE);
    }
}
