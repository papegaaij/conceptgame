package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import vanguard.sim.Ally;
import vanguard.sim.AllySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * M5 part D: an air escort's units (design/allies, evacuation shuttle; Level 10's Lifelines), drawn
 * from the production sprites of tools/art/shuttle.py and their pivot file: on its pad the smallest
 * liftoff step; lifting off, the liftoff step nearest its scale (0.70 on the ground to 1.0 on air)
 * with the boost flare on its engines, scaled alike, and its shadow sliding out to the air offset;
 * flying its station, the bank frame of its sideways speed with the engine flames, a white flash on a
 * hit, and below its smoke share the damaged frames (the dead engine without a flame) and a smoke
 * puff every {@value #SMOKE_TICKS} steps; climbing out, the level frame with the flare; lost, the
 * powerless wreck's glide into {@code far} over its whole glide time ({@link #wreckStep}: the wreck
 * frames down to far's scale), sinking down the screen up to far's speed, darkening toward far's
 * tone ({@link #wreckShade}), fading out only at the very end ({@link #wreckFade}), trailing a steady
 * plume of large dark smoke puffs (one every {@value #WRECK_SMOKE_TICKS} steps), its shadow shrinking
 * and fading. A unit low in its
 * liftoff and a gliding wreck are drawn under the low-air layer ({@link #drawLow}), the others with
 * the flyers ({@link #drawAir}). Pure presentation: the smoke puffs live in a fixed ring.
 */
final class ShuttleLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The liftoff steps' scales (tools/art/shuttle.py, {@code LIFT_SCALES}); 1.0 is the level bank frame. */
    static final double[] LIFT_SCALES = {0.70, 0.775, 0.85, 0.925};
    /** Below this lift a lifting unit is drawn under the low-air layer (about its scale 0.85). */
    static final double LOW_LIFT = 0.5;
    /** The engine flame and the flare show each frame this many steps (20 fps). */
    private static final int FLAME_FRAME_TICKS = 3;

    private static final int HIT_FLASH_TICKS = 2;
    /** A damaged unit or a wreck leaves a smoke puff every this many steps. */
    static final int SMOKE_TICKS = 6;
    /** Each puff frame shows this many steps; a puff drifts down the screen this fast, px per step (far's 95 px/s). */
    private static final int SMOKE_FRAME_TICKS = 6;

    private static final float SMOKE_DRIFT = 1.6f;
    /** The wreck sinks into {@code far}: far scrolls at this share of the ground's speed. */
    static final double FAR_SHARE = 0.5;
    /**
     * The wreck slides out of the column over its glide, px, away from the band's middle (a unit left
     * of it, or on it, to the left), quickly at first: 2026-10-08, the round 32 capture's wreck of
     * Lifeline Three sank straight behind Lifeline Five, flying its station below it (the review's
     * yaw of 18 px to the left was not enough to clear it).
     */
    static final double WRECK_DRIFT_X = 64;
    /** The wreck's shadow is gone by this glide step. */
    static final int WRECK_SHADOW_STEPS = 5;
    /**
     * 2026-10-08: the glide plays the wreck frames down to far's scale only: frames 0–5 of the eight
     * (tools/art/shuttle.py: 1.0 to 0.47, far's 0.45), each half a second of the 3 s glide. The last
     * two (0.41 and 0.35, smaller than far) read as gone under the low-air haze, which made the
     * round 32 capture's glide look half as long as its data.
     */
    static final int WRECK_FAR_FRAMES = 6;
    /** The wreck's colour at the loss and at the glide's end (×, on top of the frames' own darkening): toward far's hazy tone. */
    static final float WRECK_SHADE_START = 0.75f;

    static final float WRECK_SHADE_END = 0.5f;
    /** The wreck fades out over the last this share of its glide. */
    static final double WRECK_FADE = 0.12;
    /** A gliding wreck leaves a smoke puff every this many steps, drawn this much larger and darker. */
    static final int WRECK_SMOKE_TICKS = 2;

    private static final float WRECK_PUFF_SCALE = 1.75f;
    private static final float WRECK_PUFF_SHADE = 0.55f;

    private static final int PUFFS = 128;

    private final boolean present;
    private final AllySpec spec;
    private final FlashShader flash;
    private final Array<AtlasRegion> banks;
    private final Array<AtlasRegion> damaged;
    private final Array<AtlasRegion> lift;
    private final Array<AtlasRegion> wreck;
    private final Array<AtlasRegion> flame;
    private final Array<AtlasRegion> flare;
    private final Array<AtlasRegion> smoke;
    /** The engine mounts, [engine][frame] = {x, y} px from the frame's top left: per bank, per lift step, per wreck step. */
    private final int[][][] bankMounts;

    private final int[][][] liftMounts;
    /** The smoke source (the dead engine) per bank frame and per wreck step. */
    private final int[][] smokeMounts;

    private final int[][] wreckSmoke;
    private final int[] flameAttach;
    private final int[] flareAttach;

    private final float[] puffX = new float[PUFFS];
    private final float[] puffY = new float[PUFFS];
    private final long[] puffBorn = new long[PUFFS];
    private final boolean[] puffLow = new boolean[PUFFS];
    private int nextPuff;
    private long lastTick = -1;

    /**
     * @param pivots the shuttle's pivot file ({@code assets/pivots/<slug>.json}), null without one
     *     (the frames' centres stand in for the mounts)
     */
    ShuttleLooks(Sprites sprites, FlashShader flash, LevelScript script, JsonValue pivots) {
        this.flash = flash;
        LevelScript.Escort escort = script.escort().orElse(null);
        present = escort != null && escort.air().isPresent();
        spec = present ? escort.ally() : null;
        String slug = present ? spec.slug() : "";
        banks = frames(sprites, slug, Math.max(1, present ? spec.bankFrames() : 1));
        damaged = sprites.has(slug + "-damaged") ? sprites.frames(slug + "-damaged") : banks;
        lift = sprites.has(slug + "-lift") ? sprites.frames(slug + "-lift") : new Array<>();
        wreck = sprites.has(slug + "-wreck") ? sprites.frames(slug + "-wreck") : new Array<>();
        flame = sprites.has(slug + "-flame") ? sprites.frames(slug + "-flame") : new Array<>();
        flare = sprites.has(slug + "-flare") ? sprites.frames(slug + "-flare") : flame;
        smoke = sprites.has("ship-smoke") ? sprites.frames("ship-smoke") : new Array<>();
        if (pivots != null) {
            bankMounts = mounts(pivots.get("points"));
            liftMounts = mounts(pivots.get("lift"));
            smokeMounts = points(pivots.get("smoke"));
            wreckSmoke = points(pivots.get("wreck-smoke"));
            flameAttach = attach(pivots, slug + "-flame");
            flareAttach = attach(pivots, slug + "-flare");
        } else {
            bankMounts = new int[0][][];
            liftMounts = new int[0][][];
            smokeMounts = new int[0][];
            wreckSmoke = new int[0][];
            flameAttach = new int[] {0, 0};
            flareAttach = new int[] {0, 0};
        }
        Arrays.fill(puffBorn, Long.MIN_VALUE / 2);
    }

    private static Array<AtlasRegion> frames(Sprites sprites, String slug, int count) {
        if (!slug.isEmpty() && sprites.has(slug)) {
            return sprites.frames(slug);
        }
        return Placeholders.frames(slug.isEmpty() ? "evacuation-shuttle" : slug, count);
    }

    /** The engines' mounts: [engine][frame] = {x, y}; none when the pivot file lacks the table. */
    private static int[][][] mounts(JsonValue table) {
        if (table == null) {
            return new int[0][][];
        }
        int[][][] out = new int[table.size][][];
        int e = 0;
        for (JsonValue engine = table.child; engine != null; engine = engine.next) {
            out[e++] = points(engine);
        }
        return out;
    }

    private static int[][] points(JsonValue list) {
        if (list == null) {
            return new int[0][];
        }
        int[][] out = new int[list.size][];
        for (int i = 0; i < list.size; i++) {
            out[i] = list.get(i).asIntArray();
        }
        return out;
    }

    private static int[] attach(JsonValue pivots, String name) {
        JsonValue entry = pivots.get(name);
        return entry == null || entry.get("attach") == null
                ? new int[] {0, 0}
                : entry.get("attach").asIntArray();
    }

    /** The level restarts: the smoke is gone. */
    void reset() {
        Arrays.fill(puffBorn, Long.MIN_VALUE / 2);
        lastTick = -1;
    }

    /**
     * The bank frame for a sideways speed of {@code velocity} px/s (positive to the right): the level
     * frame in the middle of {@code frames}, a full bank at {@code full} px/s (tools/art/shuttle.py:
     * {@code 2 + round(2 v / 60)} of five).
     */
    static int bankFrame(double velocity, double full, int frames) {
        int middle = (frames - 1) / 2;
        if (!(full > 0) || frames < 2) {
            return middle;
        }
        return Math.clamp(middle + Math.round(middle * velocity / full), 0, frames - 1);
    }

    /** The liftoff step at {@code lift} (0 on the pad to 1 on air): one of the {@value #LIFT_STEPS} steps, the scale's floor. */
    static int liftStep(double lift) {
        return Math.clamp((long) Math.floor(lift * LIFT_STEPS), 0, LIFT_STEPS - 1);
    }

    private static final int LIFT_STEPS = 4;

    /** The wreck's share of its glide after {@code ticks} steps (0 at the loss, 1 at its end). */
    static double glide(double ticks, double glideSeconds) {
        double total = Math.max(1, glideSeconds * SimStep.PER_SECOND);
        return Math.clamp(ticks / total, 0, 1);
    }

    /**
     * The wreck frame at glide share {@code u}: of the first {@value #WRECK_FAR_FRAMES} of {@code
     * steps} (down to far's scale), a frame per equal share of the glide.
     */
    static int wreckStep(double u, int steps) {
        int used = Math.min(steps, WRECK_FAR_FRAMES);
        return Math.clamp((long) Math.floor(u * used), 0, used - 1);
    }

    /** The wreck's colour factor at glide share {@code u}: dark from the loss on, darker toward far's tone at the end. */
    static float wreckShade(double u) {
        return (float) (WRECK_SHADE_START + (WRECK_SHADE_END - WRECK_SHADE_START) * Math.clamp(u, 0, 1));
    }

    /** The wreck's opacity at glide share {@code u}: whole until the last {@value #WRECK_FADE} of the glide. */
    static float wreckFade(double u) {
        return (float) Math.clamp((1 - u) / WRECK_FADE, 0, 1);
    }

    /**
     * How far the wreck has sunk down the screen at glide share {@code u}, px: its speed rises evenly
     * from 0 to far's ({@value #FAR_SHARE} of the ground's {@code groundSpeed}) over the glide.
     */
    static double sink(double u, double groundSpeed, double glideSeconds) {
        return FAR_SHARE * groundSpeed * glideSeconds * u * u / 2;
    }

    /**
     * How far the wreck of a unit lost at {@code x} has slid aside at glide share {@code u}, px: out
     * of the column, away from the play field's middle, easing out ({@code 1 - (1 - u)²}).
     */
    static double drift(double u, double x) {
        double v = Math.clamp(u, 0, 1);
        double side = x > PlayField.WIDTH / 2.0 ? 1 : -1;
        return side * WRECK_DRIFT_X * (1 - (1 - v) * (1 - v));
    }

    /** The wreck's shadow opacity at glide step {@code step}: half (the shadows') fading out by step {@value #WRECK_SHADOW_STEPS}. */
    static float wreckShadow(int step) {
        return (float) Math.max(0, 1 - step / (double) WRECK_SHADOW_STEPS);
    }

    /** Whether unit {@code ally} is drawn under the low-air layer: on its pad, low in its liftoff, or a wreck. */
    static boolean low(Ally ally, float alpha) {
        return switch (ally.state()) {
            case PAD, WRECK -> true;
            case LIFTING -> ally.lift(alpha) < LOW_LIFT;
            default -> false;
        };
    }

    /** The units on their pads and low in their liftoff, and the wrecks gliding down, with their smoke. */
    void drawLow(SpriteBatch batch, Sortie sortie, float alpha, float whiteFlash) {
        if (!present) {
            return;
        }
        advance(sortie);
        drawPuffs(batch, sortie.tick(), alpha, true);
        for (int k = 0; k < sortie.allyCount(); k++) {
            Ally ally = sortie.ally(k);
            if (low(ally, alpha)) {
                drawUnit(batch, sortie, ally, k, alpha, whiteFlash);
            }
        }
    }

    /** The units flying their stations, lifting off high, climbing out, with their smoke. */
    void drawAir(SpriteBatch batch, Sortie sortie, float alpha, float whiteFlash) {
        if (!present) {
            return;
        }
        advance(sortie);
        drawPuffs(batch, sortie.tick(), alpha, false);
        for (int k = 0; k < sortie.allyCount(); k++) {
            Ally ally = sortie.ally(k);
            if (!low(ally, alpha) && ally.state() != Ally.State.HOME) {
                drawUnit(batch, sortie, ally, k, alpha, whiteFlash);
            }
        }
    }

    /** Their shadows on the ground, between {@link Shadows#begin} and {@link Shadows#end}. */
    void drawShadows(SpriteBatch batch, Sortie sortie, float alpha) {
        if (!present) {
            return;
        }
        for (int k = 0; k < sortie.allyCount(); k++) {
            Ally ally = sortie.ally(k);
            double x = ally.renderX(alpha);
            double y = ally.renderY(alpha);
            switch (ally.state()) {
                case LIFTING -> {
                    double share = ally.lift(alpha);
                    AtlasRegion frame = liftFrame(share);
                    Shadows.draw(
                            batch,
                            frame,
                            (float) (X0 + x),
                            (float) y,
                            (int) Math.round(Shadows.AIR_DX * share),
                            (int) Math.round(Shadows.AIR_DY * share),
                            1);
                }
                case FLYING, CLIMBING ->
                    Shadows.draw(
                            batch,
                            bankFrameOf(ally, sortie),
                            (float) (X0 + x),
                            (float) y,
                            Shadows.AIR_DX,
                            Shadows.AIR_DY,
                            1);
                case WRECK -> {
                    if (wreck.isEmpty()) {
                        continue;
                    }
                    double u = glide(ally.ticksSinceLost() + alpha, spec.glideSeconds());
                    int step = wreckStep(u, wreck.size);
                    float shade = wreckShadow(step);
                    if (u >= 1 || shade <= 0) {
                        continue;
                    }
                    batch.setColor(0, 0, 0, Shadows.OPACITY * shade);
                    Shadows.draw(
                            batch,
                            wreck.get(step),
                            (float) (X0 + x + drift(u, x)),
                            (float) (y - sink(u, sortie.groundSpeed(), spec.glideSeconds())),
                            (int) Math.round(Shadows.AIR_DX * (1 - u)),
                            (int) Math.round(Shadows.AIR_DY * (1 - u)),
                            1);
                    batch.setColor(0, 0, 0, Shadows.OPACITY);
                }
                default -> {}
            }
        }
    }

    private AtlasRegion liftFrame(double share) {
        return lift.isEmpty() ? banks.get((banks.size - 1) / 2) : lift.get(Math.min(liftStep(share), lift.size - 1));
    }

    private boolean damaged(Ally ally) {
        return ally.hpShare() < spec.smokeBelow();
    }

    private int bankOf(Ally ally) {
        return ally.state() == Ally.State.FLYING
                ? bankFrame(ally.bankVelocity(), spec.bankFull(), banks.size)
                : (banks.size - 1) / 2;
    }

    private AtlasRegion bankFrameOf(Ally ally, Sortie sortie) {
        int bank = bankOf(ally);
        return (damaged(ally) && ally.state() == Ally.State.FLYING ? damaged : banks).get(bank);
    }

    private void drawUnit(SpriteBatch batch, Sortie sortie, Ally ally, int k, float alpha, float whiteFlash) {
        double x = ally.renderX(alpha);
        double y = ally.renderY(alpha);
        long tick = sortie.tick();
        switch (ally.state()) {
            case PAD -> draw(batch, liftFrame(0), x, y);
            case LIFTING -> {
                double share = ally.lift(alpha);
                int step = Math.min(liftStep(share), Math.max(0, lift.size - 1));
                AtlasRegion frame = liftFrame(share);
                drawEngines(batch, flare, flareAttach, liftMounts, step, -1, frame, x, y, tick + k, LIFT_SCALES[step]);
                draw(batch, frame, x, y);
            }
            case FLYING, CLIMBING -> {
                int bank = bankOf(ally);
                boolean hurt = damaged(ally) && ally.state() == Ally.State.FLYING;
                AtlasRegion frame = (hurt ? damaged : banks).get(bank);
                boolean climbing = ally.state() == Ally.State.CLIMBING;
                int dead = hurt && bank < smokeMounts.length ? deadEngine(bank) : -1;
                drawEngines(
                        batch,
                        climbing ? flare : flame,
                        climbing ? flareAttach : flameAttach,
                        bankMounts,
                        bank,
                        dead,
                        frame,
                        x,
                        y,
                        tick + k,
                        1);
                if (ally.ticksSinceHit() < HIT_FLASH_TICKS && ally.state() == Ally.State.FLYING) {
                    flash.draw(batch, frame, Math.round(X0 + x), Math.round(y), Color.WHITE, whiteFlash);
                } else {
                    draw(batch, frame, x, y);
                }
            }
            case WRECK -> {
                if (wreck.isEmpty()) {
                    return;
                }
                double u = glide(ally.ticksSinceLost() + alpha, spec.glideSeconds());
                if (u >= 1) {
                    return;
                }
                float shade = wreckShade(u);
                // Far's hazy rose-grey (Level 10's haze 54484e): a touch less green.
                batch.setColor(shade, shade * 0.92f, shade * 0.96f, wreckFade(u));
                draw(
                        batch,
                        wreck.get(wreckStep(u, wreck.size)),
                        x + drift(u, x),
                        y - sink(u, sortie.groundSpeed(), spec.glideSeconds()));
                batch.setColor(Color.WHITE);
            }
            default -> {}
        }
    }

    /** The engine whose mount is the smoke source of bank frame {@code bank} (dead in the damaged frames); -1 for none. */
    private int deadEngine(int bank) {
        for (int e = 0; e < bankMounts.length; e++) {
            if (bank < bankMounts[e].length && Arrays.equals(bankMounts[e][bank], smokeMounts[bank])) {
                return e;
            }
        }
        return -1;
    }

    /**
     * The engines' flames (or flares) of {@code frame} centred on (x, y), additive, each at its mount
     * of frame {@code index} in {@code mounts}, its attach point on it, scaled by {@code scale}; the
     * engine {@code dead} has none.
     */
    private void drawEngines(
            SpriteBatch batch,
            Array<AtlasRegion> set,
            int[] attachAt,
            int[][][] mounts,
            int index,
            int dead,
            AtlasRegion frame,
            double x,
            double y,
            long tick,
            double scale) {
        if (set.isEmpty() || mounts.length == 0) {
            return;
        }
        float left = Math.round(X0 + x - frame.getRegionWidth() / 2.0);
        float top = Math.round(y - frame.getRegionHeight() / 2.0) + frame.getRegionHeight();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int e = 0; e < mounts.length; e++) {
            if (e == dead || index >= mounts[e].length) {
                continue;
            }
            AtlasRegion fire = set.get((int) ((tick / FLAME_FRAME_TICKS + e) % set.size));
            int[] mount = mounts[e][index];
            float width = (float) (fire.getRegionWidth() * scale);
            float height = (float) (fire.getRegionHeight() * scale);
            float mx = left + mount[0];
            float my = top - mount[1];
            batch.draw(
                    fire,
                    mx - (float) (attachAt[0] * scale),
                    my - height + (float) (attachAt[1] * scale),
                    width,
                    height);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Advances the smoke to the sortie's step: a puff every {@value #SMOKE_TICKS} steps from each
     * damaged unit's dead engine and each gliding wreck, where it is now.
     */
    private void advance(Sortie sortie) {
        long tick = sortie.tick();
        if (tick < lastTick) {
            reset();
        }
        if (lastTick < 0) {
            lastTick = tick - 1;
        }
        if (tick == lastTick || smoke.isEmpty()) {
            lastTick = tick;
            return;
        }
        for (long t = Math.max(lastTick + 1, tick - PUFFS * WRECK_SMOKE_TICKS); t <= tick; t++) {
            for (int k = 0; k < sortie.allyCount(); k++) {
                Ally ally = sortie.ally(k);
                if (t % (ally.state() == Ally.State.WRECK ? WRECK_SMOKE_TICKS : SMOKE_TICKS) == 0) {
                    emit(sortie, ally, t);
                }
            }
        }
        lastTick = tick;
    }

    private void emit(Sortie sortie, Ally ally, long t) {
        AtlasRegion frame = banks.first();
        float left = (float) (ally.x() - frame.getRegionWidth() / 2.0);
        float top = (float) (ally.y() + frame.getRegionHeight() / 2.0);
        if (ally.state() == Ally.State.FLYING && damaged(ally)) {
            int bank = bankOf(ally);
            int[] at = bank < smokeMounts.length ? smokeMounts[bank] : centre(frame);
            puff(left + at[0], top - at[1], t, false);
        } else if (ally.state() == Ally.State.WRECK && !wreck.isEmpty()) {
            double u = glide(ally.ticksSinceLost(), spec.glideSeconds());
            if (u >= 1) {
                return;
            }
            int step = wreckStep(u, wreck.size);
            int[] at = step < wreckSmoke.length ? wreckSmoke[step] : centre(frame);
            puff(
                    (float) (left + drift(u, ally.x()) + at[0]),
                    (float) (top - sink(u, sortie.groundSpeed(), spec.glideSeconds()) - at[1]),
                    t,
                    true);
        }
    }

    private static int[] centre(AtlasRegion frame) {
        return new int[] {frame.getRegionWidth() / 2, frame.getRegionHeight() / 2};
    }

    private void puff(float x, float y, long born, boolean low) {
        puffX[nextPuff] = x;
        puffY[nextPuff] = y;
        puffBorn[nextPuff] = born;
        puffLow[nextPuff] = low;
        nextPuff = (nextPuff + 1) % PUFFS;
    }

    /** The puffs: the low ones (a gliding wreck's) larger and darker, a steady plume. */
    private void drawPuffs(SpriteBatch batch, long tick, float alpha, boolean low) {
        float scale = low ? WRECK_PUFF_SCALE : 1;
        if (low) {
            batch.setColor(WRECK_PUFF_SHADE, WRECK_PUFF_SHADE, WRECK_PUFF_SHADE, 1);
        }
        for (int i = 0; i < PUFFS; i++) {
            if (puffLow[i] != low) {
                continue;
            }
            float age = tick - puffBorn[i] + alpha;
            int frame = (int) (age / SMOKE_FRAME_TICKS);
            if (age < 0 || frame >= smoke.size) {
                continue;
            }
            AtlasRegion puff = smoke.get(frame);
            float width = puff.getRegionWidth() * scale;
            float height = puff.getRegionHeight() * scale;
            batch.draw(
                    puff,
                    Math.round(X0 + puffX[i] - width / 2f),
                    Math.round(puffY[i] - SMOKE_DRIFT * age - height / 2f),
                    width,
                    height);
        }
        if (low) {
            batch.setColor(Color.WHITE);
        }
    }

    private static void draw(SpriteBatch batch, AtlasRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
