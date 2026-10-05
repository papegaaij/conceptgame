package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.Locale;
import vanguard.game.ui.Fonts;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;

/**
 * The boss warning banner (design/ui/hud, chosen concept edge-warnings-r09-a, panel C): a band across
 * the play field with scrolling yellow-and-black hazard stripes along its edges, a large red
 * {@code WARNING} that blinks and the boss's name in amber, "BROOD CARRIER APPROACHING". It opens
 * when an act boss arrives with its warning track and klaxon, and clears after 5 s, growing in and
 * out over 0.2 s. It sits on the HUD layer above everything in the play field. Drawn in code until
 * its production art; geometry in play-field pixels.
 */
public final class BossBanner implements Disposable {
    /** How long it shows, s (the concept: "the boss banner clears after 5 s"). */
    static final double SECONDS = 5;
    /** It grows in and out over this long. */
    static final double GROW_SECONDS = 0.2;
    /** {@code WARNING} is lit for this long, then dark for as long. */
    static final double BLINK_SECONDS = 0.4;

    private static final Color RED = Color.valueOf("FF3A2E");
    private static final Color AMBER = EdgeWarnings.WARN;
    private static final Color BAND = new Color(0x14 / 255f, 0x08 / 255f, 0x06 / 255f, 0.88f);
    private static final Color EDGE = Color.valueOf("8C8C96");
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The band's centre above the bottom edge, its height, the stripes' and the bevel's height. */
    private static final int CENTRE_Y = 300;

    private static final int HEIGHT = 108;
    private static final int STRIPES = 8;
    private static final int BEVEL = 2;
    /** The stripes scroll this fast, px/s. */
    private static final double STRIPE_SPEED = 30;
    /** The stripe texture: a 16×8 tile of diagonal bars, 4 px each. */
    private static final int TILE_W = 16;

    private static final int TILE_H = 8;

    private final TextureRegion pixel;
    private final BitmapFont heading;
    private final BitmapFont body;
    private final Texture stripes = stripes();
    private String name = "";
    /** The tick it opened, or -1 while it shows none. */
    private long since = -1;

    /** @param heading the {@code WARNING}'s font (drawn at twice its size); @param body the name's */
    public BossBanner(TextureRegion pixel, BitmapFont heading, BitmapFont body) {
        this.pixel = pixel;
        this.heading = heading;
        this.body = body;
    }

    /** Opens the banner for the boss of {@code barName} at {@code tick}. */
    public void show(String barName, long tick) {
        name = barName.toUpperCase(Locale.ROOT) + " APPROACHING";
        since = tick;
    }

    /** No banner, as when the level restarts. */
    public void clear() {
        since = -1;
    }

    /** Whether it shows {@code seconds} after it opened. */
    static boolean visible(double seconds) {
        return seconds >= 0 && seconds < SECONDS;
    }

    /** How far the band is open, 0..1: growing in over its first 0.2 s and out over its last. */
    static float grow(double seconds) {
        if (!visible(seconds)) {
            return 0;
        }
        return (float) Math.clamp(Math.min(seconds, SECONDS - seconds) / GROW_SECONDS, 0, 1);
    }

    /** Whether {@code WARNING} is lit: on for 0.4 s, off for 0.4 s, from the start. */
    static boolean lit(double seconds) {
        return (long) Math.floor(seconds / BLINK_SECONDS) % 2 == 0;
    }

    /** Draws it at the tick being shown ({@code tick} plus {@code alpha} of a step). */
    public void draw(SpriteBatch batch, long tick, float alpha) {
        if (since < 0) {
            return;
        }
        double seconds = (tick - since + alpha) * SimStep.SECONDS;
        if (!visible(seconds)) {
            if (seconds >= SECONDS) {
                since = -1;
            }
            return;
        }
        float open = grow(seconds);
        float half = HEIGHT / 2f * open;
        float bottom = CENTRE_Y - half;
        float top = CENTRE_Y + half;
        int width = PlayField.WIDTH;
        batch.setColor(BAND);
        batch.draw(pixel, X0, bottom, width, top - bottom);
        float stripe = Math.min(STRIPES, half);
        float scroll = (float) (seconds * STRIPE_SPEED / TILE_W);
        batch.setColor(Color.WHITE);
        batch.draw(stripes, X0, top - stripe, width, stripe, scroll, 0, scroll + (float) width / TILE_W, 1);
        batch.draw(stripes, X0, bottom, width, stripe, -scroll, 0, -scroll + (float) width / TILE_W, 1);
        batch.setColor(EDGE);
        batch.draw(pixel, X0, top, width, BEVEL);
        batch.draw(pixel, X0, bottom - BEVEL, width, BEVEL);
        batch.setColor(Color.WHITE);
        if (open < 1) {
            return;
        }
        if (lit(seconds)) {
            heading.getData().setScale(2);
            float w = Fonts.width(heading, "WARNING");
            heading.setColor(RED);
            heading.draw(batch, "WARNING", Math.round(X0 + (width - w) / 2), CENTRE_Y + 38);
            heading.getData().setScale(1);
            heading.setColor(Color.WHITE);
        }
        float w = Fonts.width(body, name);
        body.setColor(AMBER);
        body.draw(batch, name, Math.round(X0 + (width - w) / 2), CENTRE_Y - 20);
        body.setColor(Color.WHITE);
    }

    /** The hazard stripes' tile: yellow and black bars at 45°, repeating. */
    private static Texture stripes() {
        Pixmap pixmap = new Pixmap(TILE_W, TILE_H, Pixmap.Format.RGBA8888);
        for (int y = 0; y < TILE_H; y++) {
            for (int x = 0; x < TILE_W; x++) {
                boolean yellow = (x + y) / 4 % 2 == 0;
                pixmap.drawPixel(x, y, yellow ? Color.rgba8888(AMBER) : Color.rgba8888(0.04f, 0.03f, 0.02f, 1));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        stripes.dispose();
    }
}
