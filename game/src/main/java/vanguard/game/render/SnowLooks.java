package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.SplittableRandom;
import vanguard.content.LevelData;
import vanguard.sim.PlayField;

/**
 * M5 part G, the arctic snow on {@code high-air} (design/campaign Level 13, sections 3 and 4: "snow
 * streaks (at most 40 %) falling on the real clock"; whiteout look a, tools/art/whiteout_l13.py: fine
 * flakes drifting down with a light wind): a level in the Earth arctic setting snows with its
 * atmosphere, faintly in a {@code medium} one and fully in a {@code heavy} one (the whiteout), ramping
 * as the backdrop blends them. The flakes fall on the real clock, so they keep falling while the ground
 * stands in a timed halt; additive, each flake at most {@value #MAX_OPACITY} (the high-air cap). Nothing
 * is allocated per frame; another setting draws nothing.
 */
final class SnowLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final int WIDTH = PlayField.WIDTH;
    private static final int HEIGHT = PlayField.HEIGHT;
    /** The setting that snows (the threat profile's {@code setting}). */
    static final String ARCTIC = "Earth arctic";
    /** The flakes at full density; how many show goes with the density. */
    static final int FLAKES = 360;
    /** The snow's density by atmosphere (clear, light, medium, heavy). */
    static final double[] DENSITY = {0, 0, 0.3, 1};
    /** A flake falls at 40 to 90 px/s and drifts with the wind, swaying a little. */
    static final double FALL_MIN = 40;

    static final double FALL_MAX = 90;
    static final double WIND = -14;
    static final double SWAY = 5;
    static final float MAX_OPACITY = 0.4f;
    /** The flakes wrap over a field this much larger than the play field, so none pops in at an edge. */
    private static final int MARGIN = 16;

    private final List<LevelData.Stretch> stretches;
    private final double ramp;
    private final TextureRegion pixel;
    private final float[] x0 = new float[FLAKES];
    private final float[] y0 = new float[FLAKES];
    private final float[] speed = new float[FLAKES];
    private final float[] size = new float[FLAKES];
    private final float[] phase = new float[FLAKES];
    private final float[] bright = new float[FLAKES];

    private SnowLooks(List<LevelData.Stretch> stretches, double ramp, TextureRegion pixel) {
        this.stretches = stretches;
        this.ramp = ramp;
        this.pixel = pixel;
        SplittableRandom random = new SplittableRandom(1300);
        for (int i = 0; i < FLAKES; i++) {
            x0[i] = (float) random.nextDouble(WIDTH + 2 * MARGIN);
            y0[i] = (float) random.nextDouble(HEIGHT + 2 * MARGIN);
            double depth = random.nextDouble();
            speed[i] = (float) (FALL_MIN + (FALL_MAX - FALL_MIN) * depth);
            size[i] = depth > 0.7 ? 2 : 1;
            phase[i] = (float) random.nextDouble(Math.PI * 2);
            bright[i] = (float) (MAX_OPACITY * (0.45 + 0.55 * depth));
        }
    }

    /** The level's snow, or null for a level outside the arctic setting. */
    static SnowLooks of(LevelData level, boolean easy, TextureRegion pixel) {
        if (!ARCTIC.equalsIgnoreCase(level.threatProfile().setting())) {
            return null;
        }
        return new SnowLooks(level.atmosphereStretches(easy), level.backdrop().ramp(), pixel);
    }

    /** The snow's density at level time {@code seconds}, 0 to 1. */
    double density(double seconds) {
        return Math.clamp(AtmosphereRamp.density(stretches, ramp, seconds, DENSITY), 0, 1);
    }

    /** The flakes falling at real time {@code real}, as many as the density at level time {@code seconds}. */
    void draw(SpriteBatch batch, double seconds, double real) {
        int shown = (int) Math.round(FLAKES * density(seconds));
        if (shown == 0) {
            return;
        }
        int spanX = WIDTH + 2 * MARGIN;
        int spanY = HEIGHT + 2 * MARGIN;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < shown; i++) {
            double sway = SWAY * Math.sin(real * 1.7 + phase[i]);
            float x = (float) Math.floorMod(Math.round(x0[i] + WIND * real + sway), (long) spanX) - MARGIN;
            float y = (float) Math.floorMod(Math.round(y0[i] - speed[i] * real), (long) spanY) - MARGIN;
            batch.setColor(1, 1, 1, bright[i]);
            batch.draw(pixel, X0 + x, y, size[i], size[i]);
        }
        batch.setColor(1, 1, 1, 1);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
