package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import vanguard.game.level.LowArmour;
import vanguard.sim.PlayField;
import vanguard.sim.WeaponSpec;
import vanguard.sim.Wingman;

/**
 * Rook's craft in the level (design/player/wingmen): his banking frames ({@code rook_0..4}, hard left
 * .. hard right), his two warm engine flames ({@code rook-flame}, additive, its top centre on each
 * engine mount of {@code pivots/rook.json}), his runtime drop shadow ({@link Shadows}), the muzzle
 * flash of his gun at his nose, a white hit flash, a smoke trail below 30 % armour (denser below
 * 15 %, the player's {@code ship-smoke} puffs), and after his ejection the pod ({@code rook-pod})
 * drifting off to a side edge (the sim keeps it clear of the ship); the medium explosion is the
 * level's effect. The pod pops out under the explosion and after {@link #POD_OVER_TICKS} steps is
 * drawn over it ({@link #drawOver}), with a red glow around it as its beacon blinks and a thin trail
 * of light smoke behind it, so it reads on any ground.
 *
 * <p>Until the production sprites exist (tools/art/, concept round 28) he flies the chosen round-09
 * Ember concept frames that {@code :pipeline:cutPlaceholderSprites} cuts as {@code rook_0..4}; the
 * flame falls back to a small warm glow drawn per engine, the engine mounts to the
 * concept's, and the pod to a small capsule drawn with a blinking beacon. Without any {@code rook}
 * frames he is the player's craft tinted in his colours, so a missing sprite never stops a level.
 */
final class WingmanLooks {
    /** The production sprite's edge, px (design/player/wingmen); the concept frames are 40. */
    static final int SIZE = 42;
    /** The banking frames, hard left .. hard right. */
    static final int BANKS = 5;
    /** Each flame frame shows this many steps (the player's 20 fps). */
    private static final int FLAME_FRAME_TICKS = 3;
    /** The fallback flame: a warm glow (the Ember scheme's engines), a core in a halo, flickering in length. */
    private static final Color FLAME_CORE = new Color(1f, 0.92f, 0.55f, 0.95f);

    private static final Color FLAME_HALO = new Color(1f, 0.45f, 0.1f, 0.55f);
    private static final int[] FLAME_FLICKER = {4, 5, 3, 5};
    /** The fallback hull: the player's craft in Rook's dark slate and orange. */
    private static final Color EMBER = new Color(0.62f, 0.5f, 0.42f, 1);
    /** The concept's engine mounts, px from the sprite's centre (y down), for every banking frame. */
    private static final int[] FALLBACK_ENGINE_LEFT = {-3, 13};

    private static final int[] FALLBACK_ENGINE_RIGHT = {3, 13};
    /** A hit flashes his hull white this many steps (the enemies' flash). */
    static final int HIT_FLASH_TICKS = 2;
    /** Puffs leave his engines every this many steps below 30 %, every {@link #SMOKE_HEAVY_TICKS} below 15 %. */
    private static final int SMOKE_TICKS = 5;

    private static final int SMOKE_HEAVY_TICKS = 3;
    private static final int SMOKE_FRAME_TICKS = 6;
    /** The puffs hang in the air while he flies on, px per step. */
    private static final float SMOKE_DRIFT = 1.6f;
    /** The pod's frames (a beacon blink or a slow tumble) show this many steps each. */
    private static final int POD_FRAME_TICKS = 8;
    /** The drawn fallback pod: a capsule this big, its beacon blinking at this rate. */
    private static final int POD_WIDTH = 6;

    private static final int POD_HEIGHT = 9;
    private static final int BEACON_TICKS = 12;
    private static final Color POD_HULL = new Color(0.32f, 0.33f, 0.4f, 1);
    private static final Color POD_EDGE = new Color(0.14f, 0.15f, 0.2f, 1);
    private static final Color BEACON = new Color(1f, 0.55f, 0.1f, 1);
    /** The pod is drawn over the explosion from this many steps after the eject (0.1 s). */
    static final int POD_OVER_TICKS = 6;
    /** The beacon's additive glow around the pod while it blinks: a soft halo and a brighter core. */
    private static final Color GLOW_HALO = new Color(1f, 0.18f, 0.08f, 0.35f);

    private static final Color GLOW_CORE = new Color(1f, 0.4f, 0.2f, 0.45f);
    /** The pod's smoke trail: a puff every this many steps, each frame shown this many steps. */
    private static final int POD_PUFF_TICKS = 4;

    private static final int POD_PUFF_FRAME_TICKS = 4;
    /** A puff starts this far behind the pod's centre, px. */
    private static final float POD_PUFF_BEHIND = 6;
    /** Muzzle flash frames show this many steps each (the player's). */
    private static final int MUZZLE_FRAME_TICKS = 2;

    private final Sprites sprites;
    private final FlashShader flash;
    /** His banking frames, or the player's while he has none of his own. */
    private final Array<AtlasRegion> hull;
    /** Whether {@link #hull} are the player's frames, drawn tinted. */
    private final boolean fallbackHull;

    /** His engine flame's frames, its top centre on an engine mount; {@code null} while a glow is drawn instead. */
    private final Array<AtlasRegion> flame;
    /** Per banking frame his engine mounts, px from the hull's centre, y down. */
    private final int[][] engineLeft = new int[BANKS][];

    private final int[][] engineRight = new int[BANKS][];
    private final Array<AtlasRegion> smoke;
    /** The pod's frames; {@code null} while it is drawn by code. */
    private final Array<AtlasRegion> pod;
    /** The pod's trail: the Hornet's light smoke (the player's darker puffs without it). */
    private final Array<AtlasRegion> podSmoke;

    private final float[] podPuffX;
    private final float[] podPuffY;
    private final long[] podPuffBorn;
    private int nextPodPuff;

    private final float[] puffX;
    private final float[] puffY;
    private final long[] puffBorn;
    private int nextPuff;
    private long lastTick = -1;
    private long hitTick = Long.MIN_VALUE / 2;

    WingmanLooks(Sprites sprites, Files files, FlashShader flash) {
        this.sprites = sprites;
        this.flash = flash;
        fallbackHull = !sprites.has("rook");
        hull = fallbackHull ? sprites.ship : sprites.frames("rook");
        flame = sprites.has("rook-flame") ? sprites.frames("rook-flame") : null;
        FileHandle pivots = files.internal("pivots/rook.json");
        JsonValue points = pivots.exists() ? new JsonReader().parse(pivots).get("points") : null;
        for (int bank = 0; bank < BANKS; bank++) {
            engineLeft[bank] = mount(points, "engine-left", bank, FALLBACK_ENGINE_LEFT);
            engineRight[bank] = mount(points, "engine-right", bank, FALLBACK_ENGINE_RIGHT);
        }
        smoke = sprites.frames("ship-smoke");
        pod = sprites.has("rook-pod") ? sprites.frames("rook-pod") : null;
        podSmoke = sprites.has("hornet-launcher-smoke") ? sprites.frames("hornet-launcher-smoke") : smoke;
        int podPuffs = POD_PUFF_FRAME_TICKS * podSmoke.size / POD_PUFF_TICKS + 1;
        podPuffX = new float[podPuffs];
        podPuffY = new float[podPuffs];
        podPuffBorn = new long[podPuffs];
        Arrays.fill(podPuffBorn, Long.MIN_VALUE / 2);
        int puffs = SMOKE_FRAME_TICKS * smoke.size / SMOKE_HEAVY_TICKS + 1;
        puffX = new float[puffs];
        puffY = new float[puffs];
        puffBorn = new long[puffs];
        Arrays.fill(puffBorn, Long.MIN_VALUE / 2);
    }

    /** A pivot file's mount (px from the sprite's top-left) as px from its centre; the concept's without one. */
    private static int[] mount(JsonValue points, String name, int bank, int[] fallback) {
        if (points == null || points.get(name) == null) {
            return fallback;
        }
        int[] point = points.get(name).get(bank).asIntArray();
        return new int[] {point[0] - SIZE / 2, point[1] - SIZE / 2};
    }

    /** The level restarts: his smoke and hit flash are gone. */
    void reset() {
        Arrays.fill(puffBorn, Long.MIN_VALUE / 2);
        Arrays.fill(podPuffBorn, Long.MIN_VALUE / 2);
        lastTick = -1;
        hitTick = Long.MIN_VALUE / 2;
    }

    /** He was hit at step {@code tick}: his hull flashes white. */
    void hit(long tick) {
        hitTick = tick;
    }

    private AtlasRegion frame(Wingman rook) {
        int bank = Math.clamp(rook.bank() + BANKS / 2, 0, hull.size - 1);
        return hull.get(bank);
    }

    /** His craft's shadow (and his pod's) on the ground; between {@link Shadows#begin} and {@link Shadows#end}. */
    void drawShadow(SpriteBatch batch, Wingman rook, float alpha) {
        if (!rook.ejected()) {
            Shadows.draw(
                    batch,
                    frame(rook),
                    Math.round(PixelScreen.PLAY_FIELD_X + rook.renderX(alpha)),
                    Math.round(rook.renderY(alpha)),
                    Shadows.AIR_DX,
                    Shadows.AIR_DY,
                    1);
        } else if (pod != null && podVisible(rook, alpha)) {
            Shadows.draw(
                    batch,
                    pod.first(),
                    Math.round(PixelScreen.PLAY_FIELD_X + rook.podX(alpha)),
                    Math.round(rook.podY()),
                    Shadows.AIR_DX,
                    Shadows.AIR_DY,
                    1);
        }
    }

    /**
     * His craft: the smoke trail and the engine flames under the hull, the hull (white for a moment
     * after a hit); after his ejection only the pod while it is on the play field, for its first
     * {@link #POD_OVER_TICKS} steps (under the explosion; then {@link #drawOver}).
     *
     * @param whiteFlash how strongly the hit flash shows (the Gameplay tab's flash reduction)
     */
    void draw(SpriteBatch batch, Wingman rook, long tick, float alpha, float whiteFlash) {
        advanceSmoke(rook, tick);
        drawSmoke(batch, tick, alpha);
        if (rook.ejected()) {
            if (rook.ticksSinceEject() < POD_OVER_TICKS) {
                drawPod(batch, rook, tick, alpha);
            }
            return;
        }
        float x = Math.round(PixelScreen.PLAY_FIELD_X + rook.renderX(alpha));
        float y = Math.round(rook.renderY(alpha));
        int bank = Math.clamp(rook.bank() + BANKS / 2, 0, BANKS - 1);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        int step = (int) (tick / FLAME_FRAME_TICKS);
        drawFlame(batch, step, engineLeft[bank], x, y);
        drawFlame(batch, step + 1, engineRight[bank], x, y);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        AtlasRegion frame = frame(rook);
        if (tick - hitTick < HIT_FLASH_TICKS) {
            flash.draw(batch, frame, x, y, Color.WHITE, whiteFlash);
        } else {
            if (fallbackHull) {
                batch.setColor(EMBER);
            }
            batch.draw(frame, x - frame.getRegionWidth() / 2f, y - frame.getRegionHeight() / 2f);
            batch.setColor(Color.WHITE);
        }
    }

    /**
     * The flame's frame {@code step} (looping) with its top centre on an engine mount (px from the
     * hull's centre, y down); without his flame sprites a warm glow, a core in a halo.
     */
    private void drawFlame(SpriteBatch batch, int step, int[] mount, float x, float y) {
        float top = y - mount[1];
        if (flame != null) {
            AtlasRegion frame = flame.get(step % flame.size);
            batch.draw(
                    frame,
                    Math.round(x + mount[0] - frame.getRegionWidth() / 2f),
                    Math.round(top - frame.getRegionHeight()));
            return;
        }
        int length = FLAME_FLICKER[step % FLAME_FLICKER.length];
        TextureRegion pixel = sprites.pixel;
        batch.setColor(FLAME_HALO);
        batch.draw(pixel, x + mount[0] - 2, top - length - 1, 4, length + 1);
        batch.setColor(FLAME_CORE);
        batch.draw(pixel, x + mount[0] - 1, top - length + 1, 2, length - 1);
    }

    /**
     * After the level's effects: from {@link #POD_OVER_TICKS} steps after his ejection the pod's smoke
     * trail and the pod, over the explosion.
     */
    void drawOver(SpriteBatch batch, Wingman rook, long tick, float alpha) {
        if (!rook.ejected() || rook.ticksSinceEject() < POD_OVER_TICKS) {
            return;
        }
        for (int i = 0; i < podPuffX.length; i++) {
            float age = tick - podPuffBorn[i] + alpha;
            int frame = (int) (age / POD_PUFF_FRAME_TICKS);
            if (age < 0 || frame >= podSmoke.size) {
                continue;
            }
            AtlasRegion puff = podSmoke.get(frame);
            batch.draw(
                    puff,
                    Math.round(PixelScreen.PLAY_FIELD_X + podPuffX[i] - puff.getRegionWidth() / 2f),
                    Math.round(podPuffY[i] - SMOKE_DRIFT * age - puff.getRegionHeight() / 2f));
        }
        drawPod(batch, rook, tick, alpha);
    }

    /** Whether the pod is still on the play field. */
    private static boolean podVisible(Wingman rook, float alpha) {
        double x = rook.podX(alpha);
        return x > -SIZE && x < PlayField.WIDTH + SIZE;
    }

    private void drawPod(SpriteBatch batch, Wingman rook, long tick, float alpha) {
        if (!podVisible(rook, alpha)) {
            return;
        }
        float x = Math.round(PixelScreen.PLAY_FIELD_X + rook.podX(alpha));
        float y = Math.round(rook.podY());
        if (pod != null) {
            int index = (int) (tick / POD_FRAME_TICKS % pod.size);
            if (index == 0) {
                // The frame whose beacon is lit.
                drawGlow(batch, x, y);
            }
            AtlasRegion frame = pod.get(index);
            batch.draw(frame, x - frame.getRegionWidth() / 2f, y - frame.getRegionHeight() / 2f);
            return;
        }
        if (tick / BEACON_TICKS % 2 == 0) {
            drawGlow(batch, x, y);
        }
        TextureRegion pixel = sprites.pixel;
        batch.setColor(POD_EDGE);
        batch.draw(pixel, x - POD_WIDTH / 2f - 1, y - POD_HEIGHT / 2f - 1, POD_WIDTH + 2, POD_HEIGHT + 2);
        batch.setColor(POD_HULL);
        batch.draw(pixel, x - POD_WIDTH / 2f, y - POD_HEIGHT / 2f, POD_WIDTH, POD_HEIGHT);
        if (tick / BEACON_TICKS % 2 == 0) {
            batch.setColor(BEACON);
            batch.draw(pixel, x - 1, y + POD_HEIGHT / 2f - 3, 2, 2);
        }
        batch.setColor(Color.WHITE);
    }

    /** The beacon's glow around the pod centred on (x, y): additive, a halo and a core. */
    private void drawGlow(SpriteBatch batch, float x, float y) {
        TextureRegion pixel = sprites.pixel;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(GLOW_HALO);
        batch.draw(pixel, x - 9, y - 3, 18, 6);
        batch.draw(pixel, x - 3, y - 9, 6, 18);
        batch.draw(pixel, x - 6, y - 6, 12, 12);
        batch.setColor(GLOW_CORE);
        batch.draw(pixel, x - 4, y - 4, 8, 8);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The muzzle flash of his gun (the mount {@code mount} of the weapon looks) at each muzzle of its pattern. */
    void drawMuzzle(SpriteBatch batch, Wingman rook, WeaponLooks weapons, int mount, float alpha, boolean glowing) {
        if (rook.ejected()) {
            return;
        }
        WeaponLooks.Look look = weapons.look(mount);
        int frame = rook.ticksSinceShot() / MUZZLE_FRAME_TICKS;
        if (look.glowingMuzzle != glowing || frame >= look.muzzle.size) {
            return;
        }
        AtlasRegion region = look.muzzle.get(frame);
        WeaponSpec gun = rook.gun();
        for (int i = 0; i < gun.muzzles().size(); i++) {
            WeaponSpec.Muzzle muzzle = gun.muzzles().get(i);
            batch.draw(
                    region,
                    Math.round(PixelScreen.PLAY_FIELD_X
                            + rook.renderX(alpha)
                            + muzzle.dx()
                            - region.getRegionWidth() / 2.0),
                    Math.round(rook.renderY(alpha) + muzzle.dy() - region.getRegionHeight() / 2.0));
        }
    }

    /**
     * Advances the smoke to step {@code tick}: below 30 % armour a puff between his engines every few
     * steps, more often below 15 % (none after the ejection: his craft is gone); after it, while the
     * pod is drawn over the explosion and on the field, a light puff just behind the pod every few
     * steps.
     */
    private void advanceSmoke(Wingman rook, long tick) {
        if (tick < lastTick) {
            reset();
        }
        if (lastTick < 0) {
            lastTick = tick - 1;
        }
        LowArmour stage = LowArmour.of(rook.armour(), rook.maxArmour());
        for (long t = Math.max(lastTick + 1, tick - (long) puffX.length * SMOKE_TICKS); t <= tick; t++) {
            int every = stage == LowArmour.CRITICAL ? SMOKE_HEAVY_TICKS : SMOKE_TICKS;
            if (!rook.ejected() && stage != LowArmour.NONE && t % every == 0) {
                long mix = t * 0x9E3779B97F4A7C15L;
                float jitter = Math.floorMod(mix >>> 40, 7L) - 3;
                puffX[nextPuff] = (float) rook.x() + jitter;
                puffY[nextPuff] = (float) rook.y() - SIZE / 2f + 4;
                puffBorn[nextPuff] = t;
                nextPuff = (nextPuff + 1) % puffX.length;
            }
            long sinceEject = rook.ticksSinceEject() - (tick - t);
            if (rook.ejected() && sinceEject >= POD_OVER_TICKS && t % POD_PUFF_TICKS == 0 && podVisible(rook, 0)) {
                double step = rook.podX(1) - rook.podX(0);
                double podX = rook.podX(0) - step * (tick - t);
                podPuffX[nextPodPuff] = (float) (podX - Math.signum(step) * POD_PUFF_BEHIND);
                podPuffY[nextPodPuff] = (float) rook.podY();
                podPuffBorn[nextPodPuff] = t;
                nextPodPuff = (nextPodPuff + 1) % podPuffX.length;
            }
        }
        lastTick = tick;
    }

    private void drawSmoke(SpriteBatch batch, long tick, float alpha) {
        for (int i = 0; i < puffX.length; i++) {
            float age = tick - puffBorn[i] + alpha;
            int frame = (int) (age / SMOKE_FRAME_TICKS);
            if (age < 0 || frame >= smoke.size) {
                continue;
            }
            AtlasRegion puff = smoke.get(frame);
            batch.draw(
                    puff,
                    Math.round(PixelScreen.PLAY_FIELD_X + puffX[i] - puff.getRegionWidth() / 2f),
                    Math.round(puffY[i] - SMOKE_DRIFT * age - puff.getRegionHeight() / 2f));
        }
    }
}
