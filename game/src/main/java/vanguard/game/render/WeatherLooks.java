package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import java.util.List;
import java.util.SplittableRandom;
import vanguard.content.LevelData;
import vanguard.sim.PlayField;
import vanguard.sim.Wind;

/**
 * M5 part F, a level's weather looks (design/campaign Level 12, Weather; the props of
 * tools/art/l12_props.py and the screen-fixed looks of tools/art/backdrop_l12.py, drawn as its review
 * composite does): the rain on {@code high-air} by the data's density keys, the lightning (a bolt and
 * a flat sky flash of at most {@value #SKY_SECONDS} s, the cloud banks lit from inside and seen through
 * for the reveal, the bodies under them drawn again over them; never more than three flashes a second, softened by the flash-reduction option), the
 * gust telegraph (an arrow at the upwind edge and wind streaks), the faint glow of a {@code low-air}
 * body hidden under a bank (user decision F1 = a), and the eye: its cloud wall turning clockwise, its
 * light shaft, and the breakout's light at the top edge. Everything keyed to time follows the
 * simulation: the script clock for what the data times, the real clock of the steps for the rain's
 * fall and the animations, so pause and {@code --debug-speed} keep in step. Nothing is allocated per
 * frame; a level without weather draws nothing.
 */
final class WeatherLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final int WIDTH = PlayField.WIDTH;
    private static final int HEIGHT = PlayField.HEIGHT;

    /** The sky flash lasts at most this long (design/art-direction, flash rules). */
    static final double SKY_SECONDS = 0.1;
    /** Never more than three flashes a second: a flash this soon after the last shown one is dropped. */
    static final double MIN_FLASH_GAP = 1.0 / 3;
    /** The sky flash's flat tint (tools/art/backdrop_l12.py's composite), added at this strength. */
    static final Color SKY = new Color(150 / 255f, 165 / 255f, 190 / 255f, 1);

    static final float SKY_STRENGTH = 0.22f;
    /** The banks lit from inside toward this pale blue at {@value #LIT_STRENGTH}, seen through by {@value #SEE_THROUGH}. */
    static final Color LIT = new Color(190 / 255f, 205 / 255f, 235 / 255f, 1);

    static final float LIT_STRENGTH = 0.45f;
    static final float SEE_THROUGH = 0.6f;
    /** The reveal holds at full for this share of its time, then fades out over the rest. */
    static final double REVEAL_HOLD = 0.4;
    /** Where the bolts strike, px from the left: even flashes on the left, odd on the right. */
    static final float[] BOLT_X = {84, 396};
    /** The lit cloud's centre below the top edge. */
    private static final float CLOUD_DOWN = 120;
    /** The bolt's top 10 px above the top edge. */
    private static final float BOLT_TOP_OVER = 10;

    /**
     * The rain: streaks at density 1, falling at {@value #RAIN_FALL} px/s with a 1:4 slant, additive so
     * the brightest streak pixel adds at most 40 % of white (the high-air cap), stretched by up to half
     * their length as the rain thickens (the wall's motion-blurred sheets).
     */
    static final int RAIN_DROPS = 480;

    static final double RAIN_FALL = 480;
    static final double RAIN_SLANT = 120;
    private static final double RAIN_SPAN = 960;
    private static final double RAIN_ABOVE = 200;
    /** The rain images' brightest pixel (tools/art/l12_props.py's streaks), of 255. */
    static final float RAIN_PEAK = 160 / 255f;

    static final float RAIN_OPACITY = 0.4f / RAIN_PEAK;
    /** How much longer the streaks are drawn at density 1 than at 0. */
    static final float RAIN_STRETCH = 0.5f;
    /**
     * The gust telegraph: the arrow at twice its image's size, its centre {@value #ARROW_IN} px from the
     * upwind edge and {@value #ARROW_DOWN} px below the top edge (above the side edge warnings' bars,
     * 120 to 420 px down, with their glow, and the wave banner's band, 149 to 179 px down, so neither
     * covers it), drawn
     * twice additive for a bright, steady look unlike the warnings' amber pulse; its frames' rate; the
     * streaks' speed, rows and thickness.
     */
    static final float ARROW_SCALE = 2;

    static final float ARROW_IN = 72;
    static final float ARROW_DOWN = 60;
    static final int ARROW_PASSES = 2;
    private static final double ARROW_FPS = 12;
    static final double STREAK_SPEED = 216;
    private static final double STREAK_WRAP = 640;
    static final float[] STREAK_ROWS = {60, 120, 190, 260, 330, 400, 470};
    /** The wind streaks' brightest pixel, of 255; drawn at the high-air cap, twice as thick as the image. */
    static final float STREAK_PEAK = 144 / 255f;

    static final float STREAK_OPACITY = 0.4f / STREAK_PEAK;
    static final float STREAK_THICKNESS = 2;
    /** A hidden body's glow pulses at this rate (4 frames). */
    private static final double GLOW_FPS = 4;
    /** The eye's wall turns clockwise at this many degrees a second from the eye's start. */
    static final double SPIN = 3;
    /** The breakout's light: from this long into the last section, fading in over {@value #LIGHT_RAMP} s, at 40 %. */
    static final double LIGHT_AFTER = 15;

    static final double LIGHT_RAMP = 10;
    static final float LIGHT_OPACITY = 0.4f;

    private final boolean present;
    private final double[] flashes;
    private final double reveal;
    private final LevelData.Weather weather;
    private final double ramp;
    /** The eye: the section with the clear atmosphere (Level 12's section 5), NaN without one. */
    private final double eyeFrom;

    private final double eyeTo;
    private final double lightFrom;

    private final Array<AtlasRegion> rain;
    private final Array<AtlasRegion> bolts;
    private final AtlasRegion cloud;
    private final Array<AtlasRegion> arrow;
    private final Array<AtlasRegion> streaks;
    private final Array<AtlasRegion> glow;
    private final AtlasRegion wall;
    private final AtlasRegion shaft;
    private final AtlasRegion light;
    private final TextureRegion pixel;

    private final float[] dropX = new float[RAIN_DROPS];
    private final float[] dropY = new float[RAIN_DROPS];
    private final float[] dropSpeed = new float[RAIN_DROPS];
    private final float[] dropGate = new float[RAIN_DROPS];
    private final int[] dropLook = new int[RAIN_DROPS];
    /** The flash state at the last {@link #at}: the sky flash and the reveal, 0..1, and the flash's index. */
    private float sky;

    private float lit;
    private int flash = -1;

    WeatherLooks(Sprites sprites, LevelData level, String levelKey) {
        weather = level.weather().orElse(null);
        present = weather != null;
        ramp = level.backdrop().ramp();
        pixel = sprites.pixel;
        List<Double> at = weather == null
                ? List.of()
                : weather.lightning().map(LevelData.Lightning::at).orElse(List.of());
        flashes = accepted(at);
        reveal = weather == null
                ? 0
                : weather.lightning().map(LevelData.Lightning::reveal).orElse(0.0);
        double[] eye = eye(level);
        eyeFrom = eye[0];
        eyeTo = eye[1];
        lightFrom = lastSectionStart(level) + LIGHT_AFTER;
        rain = optional(sprites, present, "weather-rain");
        bolts = optional(sprites, present, "weather-bolt");
        Array<AtlasRegion> clouds = optional(sprites, present, "weather-flash");
        cloud = clouds.isEmpty() ? null : clouds.first();
        arrow = optional(sprites, present, "weather-gust-arrow");
        streaks = optional(sprites, present, "weather-gust-streak");
        glow = optional(sprites, present, "weather-glow");
        String folder = Backdrop.folder(levelKey, level);
        wall = backdrop(sprites, present, folder + "eye-wall");
        shaft = backdrop(sprites, present, folder + "eye-shaft");
        light = backdrop(sprites, present, folder + "breakout-light");
        SplittableRandom random = new SplittableRandom(1260);
        for (int i = 0; i < RAIN_DROPS; i++) {
            dropX[i] = (float) random.nextDouble(WIDTH);
            dropY[i] = (float) random.nextDouble(RAIN_SPAN);
            dropLook[i] = random.nextInt(4);
            dropSpeed[i] = (float) random.nextDouble(0.85, 1.15);
            dropGate[i] = (float) random.nextDouble();
        }
    }

    private static Array<AtlasRegion> optional(Sprites sprites, boolean present, String name) {
        return present && sprites.has(name) ? sprites.frames(name) : new Array<>(0);
    }

    private static AtlasRegion backdrop(Sprites sprites, boolean present, String name) {
        return present && sprites.hasBackdrop(name) ? sprites.backdrop(name, 1).first() : null;
    }

    /** Whether the level has weather. */
    boolean present() {
        return present;
    }

    /** Whether the level's cloud banks hide its {@code low-air} bodies (F1 = a). */
    boolean occludes() {
        return present && weather.occludes();
    }

    /**
     * The flash times shown: the data's, dropping any within {@value #MIN_FLASH_GAP} s of the last one
     * kept, so no more than three flash a second (the photosensitivity limit).
     */
    static double[] accepted(List<Double> at) {
        double[] kept = new double[at.size()];
        int n = 0;
        for (double t : at) {
            if (n == 0 || t - kept[n - 1] >= MIN_FLASH_GAP) {
                kept[n++] = t;
            }
        }
        return java.util.Arrays.copyOf(kept, n);
    }

    /** The eye's span: the first section with the clear atmosphere; NaN for none. */
    static double[] eye(LevelData level) {
        double start = 0;
        for (LevelData.Section section : level.sections()) {
            if (section.atmosphere() == LevelData.Atmosphere.CLEAR) {
                return new double[] {start, section.end()};
            }
            start = section.end();
        }
        return new double[] {Double.NaN, Double.NaN};
    }

    private static double lastSectionStart(LevelData level) {
        List<LevelData.Section> sections = level.sections();
        return sections.size() < 2 ? 0 : sections.get(sections.size() - 2).end();
    }

    /** A smooth step 0 to 1. */
    private static double smooth(double p) {
        double c = Math.clamp(p, 0, 1);
        return c * c * (3 - 2 * c);
    }

    /**
     * A look's strength at {@code t}: faded in over the ramp centred on {@code from}, out over the one
     * centred on {@code to} (tools/art/backdrop_l12.py's {@code window}).
     */
    static double window(double t, double from, double to, double ramp) {
        if (Double.isNaN(from)) {
            return 0;
        }
        return smooth((t - from) / ramp + 0.5) * (1 - smooth((t - to) / ramp + 0.5));
    }

    /** The eye wall's rotation at {@code t}, degrees as libGDX turns (counter-clockwise): clockwise on the screen. */
    static float wallDegrees(double t, double from) {
        return (float) (-SPIN * (t - from));
    }

    /**
     * The flash state at script time {@code t} into {@code out}: the sky flash (1 at the flash, 0 after
     * {@value #SKY_SECONDS} s), the reveal (1 to 0 over its seconds) and the flash's index (-1 none).
     */
    static void flashAt(double[] flashes, double reveal, double t, float[] out) {
        float skyNow = 0;
        float revealNow = 0;
        int index = -1;
        for (int i = 0; i < flashes.length; i++) {
            double age = t - flashes[i];
            if (age < 0) {
                break;
            }
            if (age < SKY_SECONDS) {
                skyNow = Math.max(skyNow, (float) (1 - age / SKY_SECONDS));
            }
            if (age < reveal) {
                revealNow = Math.max(revealNow, revealAt(age, reveal));
                index = i;
            }
        }
        out[0] = skyNow;
        out[1] = revealNow;
        out[2] = index;
    }

    /** The reveal {@code age} s into it: full for its first {@link #REVEAL_HOLD}, then fading to 0 at its end. */
    static float revealAt(double age, double reveal) {
        double hold = reveal * REVEAL_HOLD;
        if (age < hold) {
            return 1;
        }
        return (float) Math.clamp(1 - (age - hold) / (reveal - hold), 0, 1);
    }

    private final float[] state = new float[3];

    /** Moves the flash state to script time {@code seconds}; call once a frame before the draws. */
    void at(double seconds) {
        if (!present) {
            return;
        }
        flashAt(flashes, reveal, seconds, state);
        sky = state[0];
        lit = state[1];
        flash = (int) state[2];
    }

    /** How strongly the banks are lit from inside now (the reveal), 0..1. */
    float lit() {
        return lit;
    }

    /**
     * The low-air layer's opacity now: the banks seen through by {@value #SEE_THROUGH} at a flash's
     * reveal (F1 = a: the bodies under them show).
     */
    float bankOpacity() {
        return 1 - SEE_THROUGH * lit;
    }

    /**
     * How strongly the bodies under the banks are drawn again over them now, 0..1: the banks seen
     * through by {@value #SEE_THROUGH} at the reveal, however many bank layers lie over a body (the
     * low-air tiles, the banks and the tall banks each drawn at {@link #bankOpacity} still hid them).
     */
    float revealed() {
        return occludes() ? SEE_THROUGH * lit : 0;
    }

    /** The banks' tint toward {@link #LIT} now, with the flash reduction's {@code strength}. */
    float bankTint(float strength) {
        return LIT_STRENGTH * lit * strength;
    }

    /** The rain's density at script time {@code t}, 0..1: the data's keys. */
    double rain(double t) {
        return present ? weather.rainAt(t) : 0;
    }

    /** How many streaks fall at {@code density}: those whose gate lies below it, of {@value #RAIN_DROPS}. */
    static int drops(float[] gates, double density) {
        int n = 0;
        for (float gate : gates) {
            if (gate < density) {
                n++;
            }
        }
        return n;
    }

    /** How much longer than their images the streaks are drawn at {@code density}. */
    static float stretch(double density) {
        return 1 + RAIN_STRETCH * (float) Math.clamp(density, 0, 1);
    }

    /** The streaks' gates, for the tests. */
    float[] gates() {
        return dropGate;
    }

    /** The eye's light shaft over the calm sea, additive, after the ground layer (from the script time). */
    void drawShaft(SpriteBatch batch, double seconds) {
        double strength = shaft == null ? 0 : window(seconds, eyeFrom, eyeTo, ramp);
        if (strength <= 0) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1, 1, 1, (float) strength);
        batch.draw(shaft, X0 + (WIDTH - shaft.getRegionWidth()) / 2f, (HEIGHT - shaft.getRegionHeight()) / 2f);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * After the low-air layer: the eye's cloud wall turned clockwise about the play field's centre
     * (linear filtering for this one draw: nearest sampling crawls on a turned image), then the bolt
     * of a sky flash and the lit cloud of its reveal, additive.
     */
    void drawOverBanks(SpriteBatch batch, double seconds, float strength) {
        if (!present) {
            return;
        }
        double eye = wall == null ? 0 : window(seconds, eyeFrom, eyeTo, ramp);
        if (eye > 0) {
            Texture texture = wall.getTexture();
            Texture.TextureFilter min = texture.getMinFilter();
            Texture.TextureFilter mag = texture.getMagFilter();
            batch.flush();
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            float size = wall.getRegionWidth();
            batch.setColor(1, 1, 1, (float) eye);
            batch.draw(
                    wall,
                    X0 + (WIDTH - size) / 2f,
                    (HEIGHT - wall.getRegionHeight()) / 2f,
                    size / 2f,
                    wall.getRegionHeight() / 2f,
                    size,
                    wall.getRegionHeight(),
                    1,
                    1,
                    wallDegrees(seconds, eyeFrom));
            batch.setColor(Color.WHITE);
            batch.flush();
            texture.setFilter(min, mag);
        }
        if (flash < 0) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float x = X0 + BOLT_X[flash % 2];
        if (sky > 0 && !bolts.isEmpty()) {
            AtlasRegion bolt = bolts.get(flash % bolts.size);
            batch.setColor(1, 1, 1, strength);
            batch.draw(bolt, x - bolt.getRegionWidth() / 2f, HEIGHT + BOLT_TOP_OVER - bolt.getRegionHeight());
        }
        if (lit > 0 && cloud != null) {
            batch.setColor(1, 1, 1, lit * strength);
            batch.draw(cloud, x - cloud.getRegionWidth() / 2f, HEIGHT - CLOUD_DOWN - cloud.getRegionHeight() / 2f);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** A hidden {@code low-air} body's faint glow over the banks at (x, y), additive; on the real clock. */
    void drawGlow(SpriteBatch batch, double x, double y, double real) {
        if (glow.isEmpty()) {
            return;
        }
        AtlasRegion frame = glow.get((int) Math.floorMod((long) Math.floor(real * GLOW_FPS), (long) glow.size));
        batch.draw(
                frame, Math.round(X0 + x - frame.getRegionWidth() / 2f), Math.round(y - frame.getRegionHeight() / 2f));
    }

    /** Whether a hidden body's glow can be drawn. */
    boolean glows() {
        return occludes() && !glow.isEmpty();
    }

    /**
     * On {@code high-air}, over the play plane and under the bullets: the rain at the script time's
     * density falling on the real clock, the breakout's light, the gust telegraph (the arrow at the
     * upwind edge and the streaks blowing downwind) and the sky flash's flat tint.
     *
     * @param levelTick the level clock's step, for the gust telegraph
     * @param strength the flashes' strength (the flash-reduction option)
     */
    void drawFront(SpriteBatch batch, double seconds, double real, Wind wind, int levelTick, float strength) {
        if (!present) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        double density = rain(seconds);
        if (density > 0 && !rain.isEmpty()) {
            batch.setColor(1, 1, 1, RAIN_OPACITY);
            float stretch = stretch(density);
            for (int i = 0; i < RAIN_DROPS; i++) {
                if (dropGate[i] >= density) {
                    continue;
                }
                double down = (dropY[i] + RAIN_FALL * dropSpeed[i] * real) % RAIN_SPAN - RAIN_ABOVE;
                if (down < -30 || down > HEIGHT + 30) {
                    continue;
                }
                double across = (dropX[i] + RAIN_SLANT * dropSpeed[i] * real) % WIDTH;
                AtlasRegion streak = rain.get(dropLook[i] % rain.size);
                float w = streak.getRegionWidth();
                float h = Math.round(streak.getRegionHeight() * stretch);
                batch.draw(streak, Math.round(X0 + across - w / 2f), Math.round(HEIGHT - down - h / 2f), w, h);
            }
        }
        double breakout = light == null ? 0 : smooth((seconds - lightFrom) / LIGHT_RAMP);
        if (breakout > 0) {
            batch.setColor(1, 1, 1, (float) (LIGHT_OPACITY * breakout));
            batch.draw(light, X0 + (WIDTH - light.getRegionWidth()) / 2f, HEIGHT - light.getRegionHeight());
        }
        int gust = wind == null ? 0 : wind.telegraph(levelTick);
        if (gust != 0) {
            drawTelegraph(batch, gust, real);
        }
        if (sky > 0) {
            batch.setColor(SKY.r, SKY.g, SKY.b, SKY_STRENGTH * sky * strength);
            batch.draw(pixel, X0, 0, WIDTH, HEIGHT);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The gust telegraph from the side {@code gust} (1 left, -1 right): the arrow at that edge, the streaks downwind. */
    private void drawTelegraph(SpriteBatch batch, int gust, double real) {
        if (!streaks.isEmpty()) {
            batch.setColor(1, 1, 1, STREAK_OPACITY);
            for (int j = 0; j < STREAK_ROWS.length; j++) {
                AtlasRegion streak = streaks.get(j % streaks.size);
                double along = (real * STREAK_SPEED + j * 90) % STREAK_WRAP - 80;
                double x = gust > 0 ? along : WIDTH - along;
                float w = streak.getRegionWidth();
                float h = streak.getRegionHeight() * STREAK_THICKNESS;
                batch.draw(streak, Math.round(X0 + x - w / 2f), Math.round(HEIGHT - STREAK_ROWS[j] - h / 2f), w, h);
            }
        }
        if (!arrow.isEmpty()) {
            AtlasRegion frame = arrow.get((int) Math.floorMod((long) Math.floor(real * ARROW_FPS), (long) arrow.size));
            float w = frame.getRegionWidth() * ARROW_SCALE;
            float h = frame.getRegionHeight() * ARROW_SCALE;
            float centre = arrowCentre(gust);
            batch.setColor(Color.WHITE);
            // The arrow points right, the wind's way from the left; mirrored for a gust from the right.
            for (int pass = 0; pass < ARROW_PASSES; pass++) {
                batch.draw(frame, X0 + centre - gust * w / 2f, HEIGHT - ARROW_DOWN - h / 2f, gust * w, h);
            }
        }
    }

    /** The arrow's centre from the play field's left edge for a gust from the side {@code gust} (1 left, -1 right). */
    static float arrowCentre(int gust) {
        return gust > 0 ? ARROW_IN : WIDTH - ARROW_IN;
    }
}
