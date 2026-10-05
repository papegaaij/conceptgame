package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import vanguard.game.level.LowArmour;
import vanguard.sim.Defences;
import vanguard.sim.Ship;
import vanguard.sim.ShipSpec;

/**
 * The Stormhawk's effects around its hull (design/player/ship, Sprite requirements): the two engine
 * flames (tools/art/stormhawk.py: cruise, at speed and moving back, 3-frame loops, additive) at the
 * engine mounts of each banking frame; below 30 % armour a smoke trail left behind the engines, and
 * below 15 % a denser one with sparks flying off the hull (tools/art/ship_fx.py); and the hex-shimmer
 * ring over the ship on a shield hit. The trail lives in screen space and is driven by the
 * simulation's steps, so it plays the same at any frame rate; nothing is allocated per frame.
 */
public final class ShipLooks {
    /** Below this share of its armour the ship trails smoke; below {@link #SPARKS_BELOW} also sparks. */
    static final double SMOKE_BELOW = LowArmour.LOW_SHARE;

    static final double SPARKS_BELOW = LowArmour.CRITICAL_SHARE;
    /** Each flame frame shows this many steps (20 fps). */
    private static final int FLAME_FRAME_TICKS = 3;
    /** The ship counts as moving forward or back above this vertical speed, px/s. */
    private static final double FLAME_SPEED = 60;
    /** A puff leaves the engines every this many steps, and every {@link #SMOKE_HEAVY_TICKS} below 15 %. */
    private static final int SMOKE_TICKS = 4;

    private static final int SMOKE_HEAVY_TICKS = 3;
    /** Each of a puff's frames shows this many steps; it is gone after the last. */
    private static final int SMOKE_FRAME_TICKS = 6;
    /** The puffs fall behind the ship (it flies on; they hang in the air), px per step. */
    private static final float SMOKE_DRIFT = 1.6f;
    /** Below the engines' mid point where a puff appears, px. */
    private static final int SMOKE_START_DY = 4;
    /** A puff appears up to this far to either side of the engines' mid point, px, and drifts outward on that side. */
    private static final int SMOKE_JITTER = 4;

    private static final float SMOKE_SPREAD = 0.05f;
    /** A spark burst's frames show this many steps; a new burst starts every 18-41 steps. */
    private static final int SPARK_FRAME_TICKS = 2;

    private static final int SPARK_MIN_GAP = 18;
    private static final int SPARK_GAP_SPREAD = 24;
    /** Where on the 48x48 hull sparks fly from (centre-relative, y up): wing roots, spine, engines. */
    private static final int[][] SPARK_SPOTS = {{-9, -4}, {10, 1}, {-2, -12}, {5, 8}, {-14, 2}, {13, -6}};

    private final Array<AtlasRegion> flame;
    /** The shield ring's frames follow the shimmer as it fades (1 at the hit, 0 at its end). */
    private final Array<AtlasRegion> ring;

    private final Array<AtlasRegion> smoke;
    private final Array<AtlasRegion> sparks;
    /** The flame frames per length: cruise, at speed, moving back. */
    private final int[][] flameFrames = new int[3][];
    /** Where the flame's top centre attaches to an engine mount. */
    private final int flameAttachX;

    private final int flameAttachY;
    /** Per banking frame the left and right engine mounts, px from the hull's top left, y down. */
    private final int[][] engineLeft;

    private final int[][] engineRight;

    /** The trail's puffs: where (play field px) and at which step each appeared; a ring buffer. */
    private final float[] puffX;

    private final float[] puffY;
    /** A puff's sideways drift, px per step. */
    private final float[] puffSpread;

    private final long[] puffBorn;
    private int nextPuff;
    private long lastTick = -1;
    private long sparkStart = Long.MIN_VALUE;
    private long sparkNext;
    private int sparkSpot;

    public ShipLooks(Sprites sprites, Files files) {
        flame = sprites.frames("engine-flame");
        ring = sprites.frames("ship-shield");
        smoke = sprites.frames("ship-smoke");
        sparks = sprites.frames("ship-sparks");
        JsonValue pods = new JsonReader().parse(files.internal("pivots/pods.json"));
        JsonValue flameData = pods.get("engine-flame");
        int[] attach = flameData.get("attach").asIntArray();
        flameAttachX = attach[0];
        flameAttachY = attach[1];
        String[] lengths = {"cruise", "speed", "back"};
        for (int i = 0; i < lengths.length; i++) {
            flameFrames[i] = flameData.get("frames").get(lengths[i]).asIntArray();
        }
        JsonValue points =
                new JsonReader().parse(files.internal("pivots/ship.json")).get("points");
        engineLeft = mounts(points.get("engine-left"));
        engineRight = mounts(points.get("engine-right"));
        int puffs = SMOKE_FRAME_TICKS * smoke.size / SMOKE_HEAVY_TICKS + 1;
        puffX = new float[puffs];
        puffY = new float[puffs];
        puffSpread = new float[puffs];
        puffBorn = new long[puffs];
        Arrays.fill(puffBorn, Long.MIN_VALUE / 2);
    }

    private static int[][] mounts(JsonValue frames) {
        int[][] out = new int[frames.size][];
        for (int i = 0; i < frames.size; i++) {
            out[i] = frames.get(i).asIntArray();
        }
        return out;
    }

    /** The level restarts: the trail and sparks are gone. */
    public void reset() {
        Arrays.fill(puffBorn, Long.MIN_VALUE / 2);
        lastTick = -1;
        sparkStart = Long.MIN_VALUE;
        sparkNext = 0;
    }

    /**
     * Advances the trail to step {@code tick}: a puff for every step the smoke emits on, at the
     * engines of the ship as it is now (a step's puff lands where the ship was then, give or take
     * the frame), and the next spark burst when one is due.
     */
    private void advance(Ship ship, long tick) {
        if (tick < lastTick) {
            reset();
        }
        if (lastTick < 0) {
            lastTick = tick - 1;
        }
        double armour = armourShare(ship.defences());
        for (long t = Math.max(lastTick + 1, tick - puffX.length * SMOKE_TICKS); t <= tick; t++) {
            int every = armour <= SPARKS_BELOW ? SMOKE_HEAVY_TICKS : SMOKE_TICKS;
            if (armour <= SMOKE_BELOW && t % every == 0) {
                // From between the engines, a little to either side (a fixed hash of the step).
                int bank = ship.bank() + ShipSpec.HARD_BANK;
                long mix = t * 0x9E3779B97F4A7C15L;
                float jitter = Math.floorMod(mix >>> 40, (long) (2 * SMOKE_JITTER + 1)) - SMOKE_JITTER;
                puffX[nextPuff] = (float) (ship.x() + (engineLeft[bank][0] + engineRight[bank][0]) / 2.0 - 24 + jitter);
                puffY[nextPuff] = (float) (ship.y() + 24 - engineLeft[bank][1] - SMOKE_START_DY);
                puffSpread[nextPuff] = jitter * SMOKE_SPREAD;
                puffBorn[nextPuff] = t;
                nextPuff = (nextPuff + 1) % puffX.length;
            }
            if (armour <= SPARKS_BELOW && t >= sparkNext) {
                sparkStart = t;
                long mix = t * 0x9E3779B97F4A7C15L;
                sparkSpot = (int) Math.floorMod(mix >>> 17, (long) SPARK_SPOTS.length);
                sparkNext = t + SPARK_MIN_GAP + Math.floorMod(mix >>> 33, (long) SPARK_GAP_SPREAD);
            }
        }
        lastTick = tick;
    }

    private static double armourShare(Defences defences) {
        return defences.maxArmour() > 0 ? defences.armour() / defences.maxArmour() : 1;
    }

    /**
     * Below the hull: the smoke trail (normal blending) and the engine flames (additive), the hull's
     * centre at the screen point (x, y).
     */
    public void drawBelow(SpriteBatch batch, Ship ship, long tick, float alpha, float x, float y) {
        advance(ship, tick);
        for (int i = 0; i < puffX.length; i++) {
            float age = tick - puffBorn[i] + alpha;
            int frame = (int) (age / SMOKE_FRAME_TICKS);
            if (age < 0 || frame >= smoke.size) {
                continue;
            }
            AtlasRegion puff = smoke.get(frame);
            float wobble = puffSpread[i] * age;
            batch.draw(
                    puff,
                    Math.round(PixelScreen.PLAY_FIELD_X + puffX[i] + wobble - puff.getRegionWidth() / 2f),
                    Math.round(puffY[i] - SMOKE_DRIFT * age - puff.getRegionHeight() / 2f));
        }
        int bank = ship.bank() + ShipSpec.HARD_BANK;
        int length = ship.vy() > FLAME_SPEED ? 1 : ship.vy() < -FLAME_SPEED ? 2 : 0;
        int[] frames = flameFrames[length];
        AtlasRegion left = flame.get(frames[(int) (tick / FLAME_FRAME_TICKS % frames.length)]);
        AtlasRegion right = flame.get(frames[(int) ((tick / FLAME_FRAME_TICKS + 1) % frames.length)]);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawFlame(batch, left, engineLeft[bank], x, y);
        drawFlame(batch, right, engineRight[bank], x, y);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** A flame frame with its attach point on an engine mount of the hull centred on (x, y). */
    private void drawFlame(SpriteBatch batch, AtlasRegion frame, int[] mount, float x, float y) {
        batch.draw(frame, x - 24 + mount[0] - flameAttachX, y + 24 - mount[1] + flameAttachY - frame.getRegionHeight());
    }

    /**
     * Over the hull, additive: a spark burst while one plays (below 15 % armour), and the shield
     * ring while the shimmer of a shield hit lasts ({@code shimmer} 1 at the hit down to 0).
     */
    public void drawAbove(SpriteBatch batch, long tick, float alpha, float x, float y, float shimmer) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        int spark = (int) ((tick - sparkStart + alpha) / SPARK_FRAME_TICKS);
        if (sparkStart != Long.MIN_VALUE && spark >= 0 && spark < sparks.size) {
            AtlasRegion frame = sparks.get(spark);
            int[] spot = SPARK_SPOTS[sparkSpot];
            batch.draw(frame, x + spot[0] - frame.getRegionWidth() / 2f, y + spot[1] - frame.getRegionHeight() / 2f);
        }
        if (shimmer > 0) {
            int frame = Math.min(ring.size - 1, (int) ((1 - shimmer) * ring.size));
            AtlasRegion region = ring.get(frame);
            batch.draw(region, x - region.getRegionWidth() / 2f, y - region.getRegionHeight() / 2f);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
