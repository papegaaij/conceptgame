package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.Arrays;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.WarningEdge;

/**
 * The edge warnings in the play field (design/ui/hud, chosen look A "big pulse",
 * edge-warnings-r11-a): at a warned edge a 300 px amber bar (340 px at the rear) with a soft glow,
 * three 20 px chevrons fading inwards and the edge's name. A warning grows in from the middle of
 * its edge over 0.2 s, then pulses smoothly between 45 % and 100 % on the 0.267 s flash cycle.
 * The geometry is the concept generator's ({@code tools/concept/ui_r11.py}, {@code variant_a}),
 * in play-field pixels.
 */
public final class EdgeWarnings implements Disposable {
    static final Color WARN = Color.valueOf("FFC800");
    private static final Color SHADOW = new Color(40 / 255f, 26 / 255f, 0, 1);
    static final double GROW_SECONDS = 0.2;
    static final double FLASH_SECONDS = 16 / 60.0;
    static final float LOW = 0.45f;

    private static final int BAR = 6;
    private static final int SIDE_HALF = 150;
    private static final int REAR_HALF = 170;
    private static final int CHEVRON = 20;
    /** The chevron rows sit this far either side of the edge's middle. */
    private static final int SIDE_ROWS = 32;

    private static final int REAR_ROWS = 40;
    private static final float[] CHEVRON_ALPHA = {1, 0.7f, 0.42f};
    /** The label's left (or right) edge from the warned edge; the rear label sits right of the middle. */
    private static final int LABEL_INSET = 84;

    private static final int REAR_LABEL_DX = 92;
    /** The glow: the bar grown by 6 px, at 150 / 255, blurred with a radius of 5 px. */
    private static final int GLOW_GROW = 6;

    private static final float GLOW_ALPHA = 150 / 255f;
    private static final double GLOW_SIGMA = 5;
    private static final int GLOW_MARGIN = 15;
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final int H = PlayField.HEIGHT;
    private static final int W = PlayField.WIDTH;

    private final TextureRegion pixel;
    private final BitmapFont font;
    private final Texture chevronLeft = triangle(true);
    private final Texture chevronDown = triangle(false);
    private final Texture sideGlow = glow(BAR, 2 * SIDE_HALF + 1);
    private final Texture rearGlow = glow(2 * REAR_HALF + 1, BAR);
    /** The tick each edge's warning started, or -1 while it shows none. */
    private final long[] since = new long[WarningEdge.values().length];

    /** @param pixel a white pixel; @param font the label's font */
    public EdgeWarnings(TextureRegion pixel, BitmapFont font) {
        this.pixel = pixel;
        this.font = font;
        Arrays.fill(since, -1);
    }

    /**
     * Follows the simulation's warning bits after a step.
     *
     * @return the edges whose warning started in this step, as {@link WarningEdge} bits
     */
    public int step(int edges, long tick) {
        int started = 0;
        for (WarningEdge edge : WarningEdge.values()) {
            int i = edge.ordinal();
            if (!edge.in(edges)) {
                since[i] = -1;
            } else if (since[i] < 0) {
                since[i] = tick;
                started |= edge.bit();
            }
        }
        return started;
    }

    /** Forgets every warning, as when the level restarts. */
    public void clear() {
        Arrays.fill(since, -1);
    }

    /** How far a warning has grown in, 0..1, {@code seconds} after it started. */
    static float grow(double seconds) {
        return (float) Math.clamp(seconds / GROW_SECONDS, 0, 1);
    }

    /** The warning's opacity: full while it grows in, then 45..100 % on the flash cycle, brightest at its start. */
    static float pulse(double seconds) {
        if (seconds < GROW_SECONDS) {
            return 1;
        }
        return (float) (LOW + (1 - LOW) * (0.5 + 0.5 * Math.cos(2 * Math.PI * seconds / FLASH_SECONDS)));
    }

    /** Draws the warnings at the tick being shown ({@code tick} plus {@code alpha} of a step). */
    public void draw(SpriteBatch batch, long tick, float alpha) {
        for (WarningEdge edge : WarningEdge.values()) {
            long start = since[edge.ordinal()];
            if (start >= 0) {
                double seconds = (tick - start + alpha) * SimStep.SECONDS;
                draw(batch, edge, grow(seconds), pulse(seconds));
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void draw(SpriteBatch batch, WarningEdge edge, float grow, float opacity) {
        boolean rear = edge == WarningEdge.BOTTOM;
        int half = Math.round((rear ? REAR_HALF : SIDE_HALF) * grow);
        // Concept coordinates: x from the play field's left, y down from its top.
        int barX =
                switch (edge) {
                    case LEFT -> 0;
                    case RIGHT -> W - BAR;
                    case BOTTOM -> W / 2 - half;
                };
        int barY = rear ? H - BAR : H / 2 - half;
        int barWidth = rear ? 2 * half + 1 : BAR;
        int barHeight = rear ? BAR : 2 * half + 1;
        Texture glow = rear ? rearGlow : sideGlow;
        float glowOut = GLOW_GROW + GLOW_MARGIN;
        tint(batch, WARN, opacity);
        draw(batch, glow, barX - glowOut, barY - glowOut, barWidth + 2 * glowOut, barHeight + 2 * glowOut);
        batch.draw(pixel, X0 + barX, H - barY - barHeight, barWidth, barHeight);
        for (int i = 0; i < CHEVRON_ALPHA.length; i++) {
            tint(batch, WARN, opacity * CHEVRON_ALPHA[i] * grow);
            int depth = 12 + i * 20;
            switch (edge) {
                case LEFT -> {
                    chevron(batch, chevronLeft, depth, H / 2f - SIDE_ROWS - CHEVRON, false);
                    chevron(batch, chevronLeft, depth, H / 2f + SIDE_ROWS - CHEVRON, false);
                }
                case RIGHT -> {
                    chevron(batch, chevronLeft, W - 1 - depth - CHEVRON, H / 2f - SIDE_ROWS - CHEVRON, true);
                    chevron(batch, chevronLeft, W - 1 - depth - CHEVRON, H / 2f + SIDE_ROWS - CHEVRON, true);
                }
                case BOTTOM -> {
                    float top = H - 1 - depth - CHEVRON;
                    chevron(batch, chevronDown, W / 2f - REAR_ROWS - CHEVRON, top, false);
                    chevron(batch, chevronDown, W / 2f + REAR_ROWS - CHEVRON, top, false);
                }
            }
        }
        label(batch, edge, opacity);
    }

    private void label(SpriteBatch batch, WarningEdge edge, float opacity) {
        String text =
                switch (edge) {
                    case LEFT -> "! LEFT";
                    case RIGHT -> "! RIGHT";
                    case BOTTOM -> "! REAR";
                };
        float width = text.length() * font.getData().getGlyph('M').xadvance;
        float x =
                switch (edge) {
                    case LEFT -> LABEL_INSET;
                    case RIGHT -> W - LABEL_INSET - width;
                    case BOTTOM -> W / 2f + REAR_LABEL_DX;
                };
        float top = edge == WarningEdge.BOTTOM ? H - 44 : H / 2f - 7;
        font.setColor(SHADOW.r, SHADOW.g, SHADOW.b, opacity);
        font.draw(batch, text, X0 + x + 1, H - top - 1);
        font.setColor(WARN.r, WARN.g, WARN.b, opacity);
        font.draw(batch, text, X0 + x, H - top);
        font.setColor(Color.WHITE);
    }

    /** A chevron with its box's top-left corner at (x, y) in concept coordinates, mirrored for the right edge. */
    private static void chevron(SpriteBatch batch, Texture texture, float x, float y, boolean mirrored) {
        float width = texture.getWidth();
        float height = texture.getHeight();
        batch.draw(texture, X0 + x, H - y - height, width, height, 0, 0, (int) width, (int) height, mirrored, false);
    }

    private static void draw(SpriteBatch batch, Texture texture, float x, float y, float width, float height) {
        batch.draw(texture, X0 + x, H - y - height, width, height);
    }

    private static void tint(SpriteBatch batch, Color colour, float alpha) {
        batch.setColor(colour.r, colour.g, colour.b, alpha);
    }

    /** A white chevron pointing out of the left edge (21 x 41) or down (41 x 21), the concept's 20 px triangle. */
    private static Texture triangle(boolean left) {
        int length = 2 * CHEVRON + 1;
        var pixmap = new Pixmap(left ? CHEVRON + 1 : length, left ? length : CHEVRON + 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        if (left) {
            pixmap.fillTriangle(CHEVRON, 0, 0, CHEVRON, CHEVRON, 2 * CHEVRON);
        } else {
            pixmap.fillTriangle(0, 0, CHEVRON, CHEVRON, 2 * CHEVRON, 0);
        }
        var texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /** The bar's glow, white with the blurred alpha, for a bar of this size; it reaches GLOW_GROW + GLOW_MARGIN px out. */
    private static Texture glow(int barWidth, int barHeight) {
        int out = GLOW_GROW + GLOW_MARGIN;
        int width = barWidth + 2 * out;
        int height = barHeight + 2 * out;
        float[] alpha = new float[width * height];
        for (int y = GLOW_MARGIN; y < height - GLOW_MARGIN; y++) {
            for (int x = GLOW_MARGIN; x < width - GLOW_MARGIN; x++) {
                alpha[y * width + x] = GLOW_ALPHA;
            }
        }
        float[] kernel = gaussian();
        alpha = blur(alpha, width, height, kernel, 1, 0);
        alpha = blur(alpha, width, height, kernel, 0, 1);
        var pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.drawPixel(x, y, Color.rgba8888(1, 1, 1, alpha[y * width + x]));
            }
        }
        var texture = new Texture(pixmap);
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static float[] gaussian() {
        int radius = GLOW_MARGIN;
        float[] kernel = new float[2 * radius + 1];
        float sum = 0;
        for (int i = -radius; i <= radius; i++) {
            kernel[i + radius] = (float) Math.exp(-i * i / (2 * GLOW_SIGMA * GLOW_SIGMA));
            sum += kernel[i + radius];
        }
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    private static float[] blur(float[] in, int width, int height, float[] kernel, int dx, int dy) {
        int radius = kernel.length / 2;
        float[] out = new float[in.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int sx = x + k * dx;
                    int sy = y + k * dy;
                    if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                        sum += in[sy * width + sx] * kernel[k + radius];
                    }
                }
                out[y * width + x] = sum;
            }
        }
        return out;
    }

    @Override
    public void dispose() {
        chevronLeft.dispose();
        chevronDown.dispose();
        sideGlow.dispose();
        rearGlow.dispose();
    }
}
