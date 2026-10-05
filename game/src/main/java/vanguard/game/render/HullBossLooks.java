package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import vanguard.sim.LevelScript;
import vanguard.sim.SetPiece;

/**
 * A boss with one long hull that turns (the Brood Carrier, design/enemies/bosses/brood-carrier, D1 of
 * M4 part G): drawn at the altitude's scale (1.25 on {@code high-air}, 1 on the play plane) and, like
 * the Leviathan, at the altitude's opacity (75 % on {@code high-air}, easing to opaque as it descends:
 * {@link LevelRenderer#highAirOpacity}, user decision 2026-10-05, so the ship and its shots show under
 * the hull), with its shadow cast down-right from the sprite's alpha while it is off the plane, in its
 * pose ({@link BossPose#turn}): nose-down, the pre-rendered turn frames, broadside; nothing lit is
 * rotated. At the pose's ends the parts lie over the hull at their offsets: each bay sac in its stage
 * (closed, opening, open, a burst stump once destroyed; a hit taking a short white tint, see
 * {@link #hitTint}), the core's plate iris opening
 * over a second once the core is exposed with the core's lime glow pulsing, and the head turrets at
 * the nearest of their headings toward the ship. During the turn the frame alone shows them.
 *
 * <p>Sprites (Level 07's atlas; production art by {@code tools/art/brood_carrier.py}, step 3 of part
 * G): {@code <slug>-hull} (frame 0 nose-down, then the turn frames, the last broadside),
 * {@code <slug>-sac-down} and {@code <slug>-sac-side} (closed, opening…, open, burst),
 * {@code <slug>-iris} (closed … open), {@code <slug>-core-glow}, {@code <slug>-turret} (a heading set,
 * {@code headings} in assets/pivots/{@code <slug>}.json as the frigate's). Without them it draws a
 * placeholder in the concept's colours: the hull capsule turned in code (a flat shape, not a lit
 * sprite), the parts as squares.
 */
final class HullBossLooks implements Disposable {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /**
     * A hit part takes a white tint of this strength, held this many steps and then fading over this
     * many: the slowest gun (5 shots/s, 12 steps apart) hits again before the tint is gone, so a part
     * under steady fire keeps a steady tint instead of strobing between a solid white flash and its
     * sprite (round 25's capture); a single hit shows a short tint (about 0.27 s).
     */
    static final float HIT_TINT = 0.45f;

    static final int HIT_HOLD_TICKS = 4;
    static final int HIT_FADE_TICKS = 12;
    /** A sac opens and closes over this long. */
    static final double SAC_OPEN_SECONDS = 0.25;
    /** The plate iris opens over this long when the core is exposed (BC, phase 3). */
    static final double IRIS_OPEN_SECONDS = 1;
    /** The core glow's pulse period, s. */
    private static final double PULSE_SECONDS = 0.8;
    /** The shadow on the play plane at full altitude: down-right (the key light at the top left), px. */
    static final float SHADOW_DX = 48;

    static final float SHADOW_DY = -68;
    /** The shadow's opacity at full altitude; it fades out as the boss reaches the plane. */
    static final float SHADOW_ALPHA = 0.45f;

    private static final Color TEAL_BLACK = Color.valueOf("0E2A2A");
    private static final Color TEAL_RIM = Color.valueOf("2E8C80");
    private static final Color HEAD = Color.valueOf("4FB8A6");
    private static final Color SAC_CLOSED = Color.valueOf("3E6A1A");
    private static final Color SAC_OPEN = Color.valueOf("B6FF3A");
    private static final Color SAC_BURST = Color.valueOf("5A1E14");
    private static final Color IRIS_PLATE = Color.valueOf("1C3C38");
    private static final Color CORE = Color.valueOf("C8FF50");
    private static final Color TURRET = Color.valueOf("6E8A84");
    private static final int PART_SQUARE = 26;
    private static final int TURRET_SQUARE = 18;

    private final TextureRegion pixel;
    private final FlashShader flash;
    private final int width;
    private final int length;
    /** The production sprites; null for the placeholder. */
    private final Array<AtlasRegion> hull;

    private final Array<AtlasRegion> sacDown;
    private final Array<AtlasRegion> sacSide;
    private final Array<AtlasRegion> iris;
    private final AtlasRegion coreGlow;
    private final Array<AtlasRegion> turrets;
    /** The turret heading set: this many steps per turn, the first frame's step (clockwise from down). */
    private final int steps;

    private final int firstStep;
    /** The placeholder hull, nose-down; null with the production sprites. */
    private final Texture placeholder;

    private final TextureRegion placeholderHull;
    /** Per part: level seconds of its last open or close, whether it was open then. */
    private final double[] changedAt;

    private final boolean[] wasOpen;
    /** The batch's tint while the parts are drawn, and a scratch colour (no allocation per frame). */
    private final Color tint = new Color();

    private final Color scratch = new Color();

    HullBossLooks(Sprites sprites, FlashShader flash, LevelScript.SetPieceSpec spec, JsonValue pivots) {
        String slug = spec.slug();
        pixel = sprites.pixel;
        this.flash = flash;
        width = (int) spec.size().width();
        length = (int) spec.size().height();
        changedAt = new double[spec.parts().size()];
        wasOpen = new boolean[spec.parts().size()];
        if (sprites.has(slug + "-hull")) {
            hull = sprites.frames(slug + "-hull");
            sacDown = optional(sprites, slug + "-sac-down");
            sacSide = optional(sprites, slug + "-sac-side");
            iris = optional(sprites, slug + "-iris");
            coreGlow = sprites.has(slug + "-core-glow") ? sprites.region(slug + "-core-glow") : null;
            turrets = optional(sprites, slug + "-turret");
            JsonValue headings = pivots == null ? null : pivots.get("headings");
            steps = headings == null ? 32 : headings.getInt("steps");
            firstStep = headings == null ? 0 : headings.getInt("first");
            placeholder = null;
            placeholderHull = null;
        } else {
            hull = sacDown = sacSide = iris = turrets = null;
            coreGlow = null;
            steps = 32;
            firstStep = 0;
            placeholder = capsule(width, length);
            placeholderHull = new TextureRegion(placeholder);
        }
        reset();
    }

    private static Array<AtlasRegion> optional(Sprites sprites, String name) {
        return sprites.has(name) ? sprites.frames(name) : null;
    }

    /** Forgets the parts' open and close times, as when the level restarts. */
    void reset() {
        Arrays.fill(changedAt, Double.NEGATIVE_INFINITY);
        Arrays.fill(wasOpen, false);
    }

    /** Whether it draws the placeholder (no production sprites yet). */
    boolean placeholder() {
        return hull == null;
    }

    /**
     * Its shadow on the play plane while it flies above it: the hull's silhouette in black, offset
     * down-right and fading out by its altitude; drawn below the air layer.
     */
    void drawShadow(SpriteBatch batch, SetPiece piece, float alpha) {
        double altitude = piece.onPlane() ? 0 : piece.altitude(alpha);
        if (altitude <= 0) {
            return;
        }
        double turn = BossPose.turn(piece, alpha);
        double x = piece.renderX(alpha) + SHADOW_DX * altitude;
        double y = piece.renderY(alpha) + SHADOW_DY * altitude;
        batch.setColor(0, 0, 0, (float) (SHADOW_ALPHA * altitude));
        drawHull(batch, piece, turn, x, y, 1);
        batch.setColor(Color.WHITE);
    }

    /** The boss at its interpolated place, scale and pose with its parts. */
    void draw(
            SpriteBatch batch,
            SetPiece piece,
            float alpha,
            double seconds,
            float whiteFlash,
            double shipX,
            double shipY) {
        double altitude = piece.onPlane() ? 0 : piece.altitude(alpha);
        float scale = LevelRenderer.highAirScale(altitude);
        double turn = BossPose.turn(piece, alpha);
        double x = piece.renderX(alpha);
        double y = piece.renderY(alpha);
        follow(piece, seconds);
        // Translucent off the play plane (body, parts and glow alike), as the Leviathan.
        batch.setColor(1, 1, 1, LevelRenderer.highAirOpacity(altitude));
        drawHull(batch, piece, turn, x, y, scale);
        drawParts(batch, piece, turn, x, y, scale, seconds, whiteFlash, shipX, shipY, false);
        batch.setColor(Color.WHITE);
    }

    /**
     * The white tint of a part {@code ticksSinceHit} steps after its last hit: {@link #HIT_TINT} held
     * {@link #HIT_HOLD_TICKS} steps, then fading out over {@link #HIT_FADE_TICKS}; 0 long after.
     */
    static float hitTint(int ticksSinceHit) {
        if (ticksSinceHit < HIT_HOLD_TICKS) {
            return HIT_TINT;
        }
        int fading = ticksSinceHit - HIT_HOLD_TICKS;
        return fading >= HIT_FADE_TICKS ? 0 : HIT_TINT * (1 - (float) fading / HIT_FADE_TICKS);
    }

    /**
     * The destroyed boss where it died (its pose then, every sac a burst stump, the iris open over the
     * dark core), shaded by {@code shade}: the body under its chained death and the carcass after it.
     */
    void drawWreck(SpriteBatch batch, SetPiece piece, double x, double y, float scale, float shade, float opacity) {
        double turn = BossPose.turn(piece, 1);
        batch.setColor(shade, shade, shade, opacity);
        drawHull(batch, piece, turn, x, y, scale);
        drawParts(batch, piece, turn, x, y, scale, 0, 0, x, y - 100, true);
        batch.setColor(Color.WHITE);
    }

    /** Notes each part opening or closing, for its stage animation. */
    private void follow(SetPiece piece, double seconds) {
        for (int p = 0; p < piece.partCount(); p++) {
            boolean open = BossPose.open(piece, p);
            if (open != wasOpen[p]) {
                wasOpen[p] = open;
                changedAt[p] = seconds;
            }
        }
    }

    /** How far part {@code p} is open, 0..1, at {@code seconds}, opening or closing over {@code over} s. */
    private double opening(int p, double seconds, double over) {
        double share = Math.clamp((seconds - changedAt[p]) / over, 0, 1);
        return wasOpen[p] ? share : 1 - share;
    }

    private void drawHull(SpriteBatch batch, SetPiece piece, double turn, double x, double y, float scale) {
        if (hull != null) {
            AtlasRegion frame = hull.get((int) Math.round(turn * (hull.size - 1)));
            drawScaled(batch, frame, x, y, scale, 0);
            return;
        }
        drawScaled(batch, placeholderHull, x, y, scale, (float) Math.toDegrees(BossPose.angle(piece, turn)));
    }

    private void drawParts(
            SpriteBatch batch,
            SetPiece piece,
            double turn,
            double x,
            double y,
            float scale,
            double seconds,
            float whiteFlash,
            double shipX,
            double shipY,
            boolean wreck) {
        boolean between = turn > 0 && turn < 1;
        if (between && hull != null) {
            // The turn frames carry the parts.
            return;
        }
        double angle = BossPose.angle(piece, turn);
        tint.set(batch.getColor());
        for (int p = 0; p < piece.partCount(); p++) {
            LevelScript.PartSpec part = piece.spec().parts().get(p);
            // At the pose's ends the simulation's offsets: its hit boxes, already at the altitude's
            // scale; the placeholder's turn turns the data offsets.
            double px = x + (between ? BossPose.turnedX(part, angle) * scale : piece.partOffsetX(p));
            double py = y + (between ? BossPose.turnedY(part, angle) * scale : piece.partOffsetY(p));
            if (BossPose.turret(piece, p)) {
                drawTurret(batch, px, py, scale, Math.atan2(shipY - py, shipX - px));
            } else if (part.vital()) {
                drawCore(batch, piece, p, px, py, scale, seconds, whiteFlash, wreck);
            } else {
                drawSac(batch, piece, p, px, py, scale, turn, seconds, whiteFlash, wreck);
            }
        }
        batch.setColor(tint);
    }

    private void drawSac(
            SpriteBatch batch,
            SetPiece piece,
            int p,
            double px,
            double py,
            float scale,
            double turn,
            double seconds,
            float whiteFlash,
            boolean wreck) {
        boolean burst = wreck || piece.partWrecked(p);
        double open = burst ? 0 : opening(p, seconds, SAC_OPEN_SECONDS);
        float hit = wreck || burst ? 0 : hitTint(piece.partTicksSinceHit(p)) * whiteFlash;
        Array<AtlasRegion> stages = turn >= 1 ? sacSide : sacDown;
        if (hull != null && stages != null) {
            int last = stages.size - 1;
            AtlasRegion frame = stages.get(burst ? last : (int) Math.round(open * (last - 1)));
            if (hit > 0 && scale == 1) {
                flash.draw(batch, frame, Math.round(X0 + px), Math.round(py), Color.WHITE, hit);
            } else {
                drawScaled(batch, frame, px, py, scale, 0);
                if (hit > 0) {
                    // Off the plane (scaled; the flash shader draws unscaled): the sprite added over itself.
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    batch.setColor(hit, hit, hit, tint.a);
                    drawScaled(batch, frame, px, py, scale, 0);
                    batch.setColor(tint);
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
            }
            return;
        }
        if (hull != null) {
            return;
        }
        Color colour = burst ? SAC_BURST : scratch.set(SAC_CLOSED).lerp(SAC_OPEN, (float) open);
        if (hit > 0) {
            colour = scratch.set(colour).lerp(Color.WHITE, hit);
        }
        square(batch, px, py, PART_SQUARE * scale, colour);
    }

    private void drawCore(
            SpriteBatch batch,
            SetPiece piece,
            int p,
            double px,
            double py,
            float scale,
            double seconds,
            float whiteFlash,
            boolean wreck) {
        double open = wreck ? 1 : opening(p, seconds, IRIS_OPEN_SECONDS);
        boolean exposed = !wreck && BossPose.open(piece, p);
        float hit = exposed ? hitTint(piece.partTicksSinceHit(p)) * whiteFlash : 0;
        float pulse = (float) (0.75 + 0.25 * Math.sin(2 * Math.PI * seconds / PULSE_SECONDS));
        if (hull != null) {
            if (iris != null) {
                drawScaled(batch, iris.get((int) Math.round(open * (iris.size - 1))), px, py, scale, 0);
            }
            if (exposed && coreGlow != null) {
                // A hit brightens the glow by its tint (held under steady fire, no strobe).
                float level = Math.min(1, (float) open * pulse + hit / HIT_TINT * (1 - (float) open * pulse));
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(level, level, level, tint.a);
                drawScaled(batch, coreGlow, px, py, scale, 0);
                if (hit > 0) {
                    batch.setColor(hit, hit, hit, tint.a);
                    drawScaled(batch, coreGlow, px, py, scale, 0);
                }
                batch.setColor(tint);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
            return;
        }
        float size = 2 * PART_SQUARE * scale;
        square(batch, px, py, size, IRIS_PLATE);
        if (open > 0) {
            Color core = wreck ? SAC_BURST : scratch.set(CORE).mul(pulse, pulse, pulse, 1);
            if (hit > 0) {
                core = scratch.set(core).lerp(Color.WHITE, hit);
            }
            square(batch, px, py, (float) (size * 0.7 * open), core);
        }
    }

    private void drawTurret(SpriteBatch batch, double px, double py, float scale, double toShip) {
        if (hull != null) {
            if (turrets != null) {
                drawScaled(batch, turrets.get(headingFrame(toShip, turrets.size)), px, py, scale, 0);
            }
            return;
        }
        square(batch, px, py, TURRET_SQUARE * scale, TURRET);
        // The barrel toward the ship.
        float barrel = 14 * scale;
        batch.setColor(tint.r * HEAD.r, tint.g * HEAD.g, tint.b * HEAD.b, tint.a);
        batch.draw(pixel, (float) (X0 + px), (float) (py - 2), 0, 2, barrel, 4, 1, 1, (float) Math.toDegrees(toShip));
    }

    /**
     * The frame of the turret heading set pointing at {@code angle} (radians counter-clockwise, y up):
     * the nearest step clockwise from straight down, clamped to the set's range (as the frigate's heads).
     */
    private int headingFrame(double angle, int count) {
        double clockwise = -Math.PI / 2 - angle;
        clockwise -= 2 * Math.PI * Math.floor((clockwise + Math.PI) / (2 * Math.PI));
        int step = (int) Math.round(clockwise / (2 * Math.PI / steps));
        return Math.clamp(step - firstStep, 0, count - 1);
    }

    /** A flat square of {@code colour} times the batch's tint, centred on (x, y). */
    private void square(SpriteBatch batch, double x, double y, float size, Color colour) {
        batch.setColor(tint.r * colour.r, tint.g * colour.g, tint.b * colour.b, tint.a * colour.a);
        batch.draw(pixel, Math.round(X0 + x - size / 2), Math.round(y - size / 2), size, size);
    }

    private static void drawScaled(
            SpriteBatch batch, TextureRegion region, double x, double y, float scale, float degrees) {
        float w = region.getRegionWidth();
        float h = region.getRegionHeight();
        batch.draw(
                region, Math.round(X0 + x - w / 2), Math.round(y - h / 2), w / 2, h / 2, w, h, scale, scale, degrees);
    }

    /**
     * The placeholder hull, nose-down: a teal-black capsule with a teal rim, the head end (at the
     * bottom of the screen) lighter.
     */
    private static Texture capsule(int width, int length) {
        Pixmap pixmap = new Pixmap(width, length, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        int r = width / 2;
        for (int py = 0; py < length; py++) {
            for (int px = 0; px < width; px++) {
                // Pixmap rows run down the screen: the head (row length - 1) is at the bottom.
                double cy = Math.clamp(py, r, length - 1 - r);
                double d = Math.hypot(px + 0.5 - r, py + 0.5 - cy);
                if (d > r) {
                    continue;
                }
                Color colour = d > r - 4 ? TEAL_RIM : py > length - r ? HEAD : TEAL_BLACK;
                pixmap.drawPixel(px, py, Color.rgba8888(colour));
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        if (placeholder != null) {
            placeholder.dispose();
        }
    }
}
