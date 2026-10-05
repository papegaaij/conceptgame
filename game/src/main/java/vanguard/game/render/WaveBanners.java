package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.game.ui.Fonts;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.WarningEdge;

/**
 * The wave warning banner (design/ui/hud, in the play field; the chosen round-09 sheet
 * edge-warnings-r09-a, panel B): "WARNING — HOSTILES FROM THE REAR" in amber on a dark band with an
 * amber frame across the upper play field, opened by an edge warning as it starts (with its tone),
 * so it needs no data of its own: the warned edges name it. One banner at a time, for 2.5 s: an
 * edge warning that starts while one shows waits for it and gets its own banner after it, if that
 * edge is still warned then. The band opens from its middle over 0.15 s, its frame pulses with the
 * edge warnings and it fades out over its last 0.3 s. Drawn in code until its production art;
 * geometry in play-field pixels, y up.
 */
public final class WaveBanners {
    /** How long a banner shows, s; an edge warning lasts at least 3 s. */
    static final double SECONDS = 2.5;

    private static final int TICKS = SimStep.ticks(SECONDS);
    /** It opens over this long and fades out over the last {@link #FADE_SECONDS}. */
    static final double GROW_SECONDS = 0.15;

    static final double FADE_SECONDS = 0.3;
    /** The band's middle above the play field's bottom edge (above the boss banner's band), its size. */
    static final int CENTRE_Y = 376;

    static final int HEIGHT = 30;
    static final int WIDTH = PlayField.WIDTH - 40;
    /** The text keeps this far from the band's ends. */
    static final int TEXT_INSET = 8;

    private static final Color AMBER = EdgeWarnings.WARN;
    private static final Color BAND = new Color(20 / 255f, 14 / 255f, 4 / 255f, 200 / 255f);
    private static final Color HIGHLIGHT = Color.valueOf("FFF0B4");
    private static final Color SHADOW = new Color(40 / 255f, 25 / 255f, 0, 1);
    private static final float X0 = PixelScreen.PLAY_FIELD_X + (PlayField.WIDTH - WIDTH) / 2f;

    private final TextureRegion pixel;
    private final BitmapFont font;
    private String text = "";
    /** The tick the shown banner opened, or -1 while none shows. */
    private long since = -1;
    /** Edges whose warning started while a banner showed, as {@link WarningEdge} bits. */
    private int pending;

    /** @param pixel a white pixel; @param font the text's font (the 10x20 body font) */
    public WaveBanners(TextureRegion pixel, BitmapFont font) {
        this.pixel = pixel;
        this.font = font;
    }

    /**
     * Follows the edge warnings after a simulation step.
     *
     * @param edges the edges warned now ({@code Sortie.edgeWarnings()})
     * @param started the edges whose warning started in this step ({@link EdgeWarnings#step})
     */
    public void step(int edges, int started, long tick) {
        pending = (pending | started) & edges;
        if (since >= 0 && tick - since >= TICKS) {
            since = -1;
        }
        if (since < 0 && pending != 0) {
            text = text(pending);
            since = tick;
            pending = 0;
        }
    }

    /** No banner and nothing waiting, as when the level restarts. */
    public void clear() {
        since = -1;
        pending = 0;
    }

    /** The banner's text now; empty while none shows. */
    String shown() {
        return since < 0 ? "" : text;
    }

    /** The banner for the warned edges, as {@link WarningEdge} bits: "WARNING — HOSTILES FROM THE REAR". */
    static String text(int edges) {
        boolean rear = WarningEdge.BOTTOM.in(edges);
        boolean left = WarningEdge.LEFT.in(edges);
        boolean right = WarningEdge.RIGHT.in(edges);
        if (rear && left && right) {
            return "WARNING — HOSTILES ON ALL SIDES";
        }
        String from;
        if (rear) {
            from = left ? "REAR AND LEFT" : right ? "REAR AND RIGHT" : "REAR";
        } else {
            from = left && right ? "LEFT AND RIGHT" : left ? "LEFT" : "RIGHT";
        }
        return "WARNING — HOSTILES FROM THE " + from;
    }

    /** How far the band is open, 0..1, {@code seconds} after it opened. */
    static float grow(double seconds) {
        return (float) Math.clamp(seconds / GROW_SECONDS, 0, 1);
    }

    /** The banner's opacity: full, fading out over its last 0.3 s; 0 once it is over. */
    static float opacity(double seconds) {
        if (seconds < 0 || seconds >= SECONDS) {
            return 0;
        }
        return (float) Math.clamp((SECONDS - seconds) / FADE_SECONDS, 0, 1);
    }

    private double seconds(long tick, float alpha) {
        return (tick - since + alpha) * SimStep.SECONDS;
    }

    /** Draws it at the tick being shown ({@code tick} plus {@code alpha} of a step). */
    public void draw(SpriteBatch batch, long tick, float alpha) {
        if (since < 0) {
            return;
        }
        double seconds = seconds(tick, alpha);
        float opacity = opacity(seconds);
        if (opacity <= 0) {
            return;
        }
        float open = grow(seconds);
        float half = HEIGHT / 2f * open;
        float bottom = Math.round(CENTRE_Y - half);
        float height = Math.max(2, Math.round(2 * half));
        batch.setColor(BAND.r, BAND.g, BAND.b, BAND.a * opacity);
        batch.draw(pixel, X0, bottom, WIDTH, height);
        float frame = opacity * EdgeWarnings.pulse(seconds);
        batch.setColor(AMBER.r, AMBER.g, AMBER.b, frame);
        batch.draw(pixel, X0, bottom, WIDTH, 1);
        batch.draw(pixel, X0, bottom + height - 1, WIDTH, 1);
        batch.draw(pixel, X0, bottom, 1, height);
        batch.draw(pixel, X0 + WIDTH - 1, bottom, 1, height);
        if (height > 3) {
            batch.setColor(HIGHLIGHT.r, HIGHLIGHT.g, HIGHLIGHT.b, frame);
            batch.draw(pixel, X0 + 1, bottom + height - 2, WIDTH - 2, 1);
        }
        batch.setColor(Color.WHITE);
        if (open < 1) {
            return;
        }
        float x = Math.round(X0 + (WIDTH - Fonts.width(font, text)) / 2f);
        float top = Math.round(CENTRE_Y + font.getCapHeight() / 2);
        font.setColor(SHADOW.r, SHADOW.g, SHADOW.b, opacity);
        font.draw(batch, text, x + 1, top - 1);
        font.setColor(AMBER.r, AMBER.g, AMBER.b, opacity);
        font.draw(batch, text, x, top);
        font.setColor(Color.WHITE);
    }
}
