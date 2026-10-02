package vanguard.game.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Disposable;
import java.util.SplittableRandom;

/**
 * The transmission static over a portrait (design/ui/briefing, design/ui/hud): a burst of noise
 * that fades out as a transmission opens and fades in as it closes. The noise is one texture of
 * random pixels generated at start-up; each frame draws it from a random offset with a few brighter
 * rolling bands, so nothing is allocated while it plays.
 */
public final class TransmissionStatic implements Disposable {
    /** How long a burst lasts. */
    public static final float SECONDS = 0.35f;

    private static final int SIZE = 256;
    private static final int BANDS = 3;
    private static final int BAND_HEIGHT = 2;

    private final Texture noise;
    private final SplittableRandom random = new SplittableRandom(2185);

    public TransmissionStatic() {
        Pixmap pixels = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float grey = 0.15f + 0.85f * random.nextFloat();
                pixels.drawPixel(x, y, rgba(0.85f * grey, 0.9f * grey, grey));
            }
        }
        noise = new Texture(pixels);
        pixels.dispose();
    }

    private static int rgba(float r, float g, float b) {
        return Math.round(r * 255) << 24 | Math.round(g * 255) << 16 | Math.round(b * 255) << 8 | 0xFF;
    }

    /**
     * The static's strength, 0 (none) to 1 (only noise): the larger of the opening burst and the
     * closing one.
     *
     * @param sinceOpened seconds since the transmission opened
     * @param untilClosed seconds until it closes; infinite while that is not known yet
     */
    public static float strength(float sinceOpened, float untilClosed) {
        return Math.max(0, 1 - Math.min(sinceOpened, untilClosed) / SECONDS);
    }

    /** Draws the static at {@code strength} over a portrait whose bottom left corner is ({@code x}, {@code y}). */
    public void draw(Batch batch, float x, float y, int width, int height, float strength) {
        if (strength <= 0) {
            return;
        }
        float colour = batch.getPackedColor();
        batch.setColor(1, 1, 1, strength);
        batch.draw(
                noise,
                x,
                y,
                width,
                height,
                random.nextInt(SIZE - width),
                random.nextInt(SIZE - height),
                width,
                height,
                false,
                false);
        batch.setColor(1, 1, 1, Math.min(1, 1.6f * strength));
        for (int i = 0; i < BANDS; i++) {
            float bandY = y + random.nextInt(height - BAND_HEIGHT);
            batch.draw(
                    noise,
                    x,
                    bandY,
                    width,
                    BAND_HEIGHT,
                    random.nextInt(SIZE - width),
                    random.nextInt(SIZE),
                    width,
                    1,
                    false,
                    false);
        }
        batch.setPackedColor(colour);
    }

    @Override
    public void dispose() {
        noise.dispose();
    }
}
