package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Enemy;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * M5 part H: Level 14's root bursts (design/campaign/act-2-homefront/level-14-siege-spire, Hazards;
 * decision H2 = a; round 36's look a until the user picks, tools/art/root_burst.py). A burst is a point
 * on the ground that scrolls with it from its telegraph on: through the telegraph the paving cracks
 * outward ({@code root-burst-decal}, 8 frames over the telegraph, its last frame staying) and dust rises
 * out of the cracks ({@code root-burst-puff}, 12 frames over the telegraph and the eruption, 6 px above
 * the point, over the ground units); as the growths appear the root arches out of the street ({@code
 * root-burst-root}, 6 frames over {@value #ERUPT_SECONDS} s, its last frame staying as the crater). The
 * growths themselves are the units' own frames, grown in ({@link #growScale}, {@link #growTint}): scaled
 * 0.35 → 1 and tinted from the creep's dark violet to their own colours over their growth.
 */
final class RootBurstLooks {
    /** The root's eruption, s (tools/art/root_burst.py's ERUPT). */
    static final double ERUPT_SECONDS = 0.5;
    /** The dust rises this far above the burst's point, px (the review's 6 px). */
    static final int PUFF_RISE = 6;
    /** The growth's first scale (tools/art/root_burst.py's grow_in). */
    static final float GROW_FROM = 0.35f;
    /** The creep's dark violet a growth is tinted from, per channel. */
    private static final float[] CREEP = {0.42f, 0.3f, 0.6f};
    /** A burst is drawn while its point lies within this margin of the screen, px. */
    private static final double MARGIN = 80;

    private final Array<AtlasRegion> decal;
    private final Array<AtlasRegion> puff;
    private final Array<AtlasRegion> root;
    /** Per burst index its telegraph's start (level seconds) and length, s; NaN for an index not in the level. */
    private final double[] start;

    private final double[] telegraph;

    private RootBurstLooks(Sprites sprites, LevelScript script) {
        decal = sprites.frames("root-burst-decal");
        puff = sprites.frames("root-burst-puff");
        root = sprites.frames("root-burst-root");
        int count = 0;
        for (LevelScript.GroundUnit unit : script.groundUnits()) {
            if (unit.burst().isPresent()) {
                count = Math.max(count, unit.burst().get().index() + 1);
            }
        }
        start = new double[count];
        telegraph = new double[count];
        java.util.Arrays.fill(start, Double.NaN);
        for (LevelScript.GroundUnit unit : script.groundUnits()) {
            unit.burst().ifPresent(burst -> {
                start[burst.index()] = burst.t();
                telegraph[burst.index()] = burst.telegraphSeconds();
            });
        }
    }

    /** The looks of the level's root bursts, or null for a level without them (or without their frames). */
    static RootBurstLooks of(Sprites sprites, LevelScript script) {
        boolean bursts =
                script.groundUnits().stream().anyMatch(unit -> unit.burst().isPresent());
        if (!bursts
                || !sprites.has("root-burst-decal")
                || !sprites.has("root-burst-puff")
                || !sprites.has("root-burst-root")) {
            return null;
        }
        return new RootBurstLooks(sprites, script);
    }

    /**
     * On the ground, under the ground units: each burst's cracks from its telegraph on and its root once
     * its growths have appeared, at the render time's level {@code seconds} and ground {@code scroll}.
     */
    void drawGround(SpriteBatch batch, Sortie sortie, double seconds, double scroll) {
        for (int k = 0; k < sortie.burstCount(); k++) {
            if (!sortie.burstTelegraphing(k) && !sortie.burstGrown(k)) {
                continue;
            }
            double y = sortie.burstY(k) + sortie.groundScroll() - scroll;
            if (y < -MARGIN || y > vanguard.sim.PlayField.HEIGHT + MARGIN) {
                continue;
            }
            int index = sortie.burstIndex(k);
            double age = age(index, seconds);
            double x = sortie.burstX(k);
            centred(batch, decal.get(decalFrame(age, telegraph[index], decal.size)), x, y);
            if (sortie.burstGrown(k)) {
                centred(batch, root.get(rootFrame(age - telegraph[index], root.size)), x, y);
            }
        }
    }

    /** Over the ground units: each burst's dust while it rises (the telegraph and the eruption). */
    void drawDust(SpriteBatch batch, Sortie sortie, double seconds, double scroll) {
        for (int k = 0; k < sortie.burstCount(); k++) {
            if (!sortie.burstTelegraphing(k) && !sortie.burstGrown(k)) {
                continue;
            }
            int index = sortie.burstIndex(k);
            int frame = puffFrame(age(index, seconds), telegraph[index], puff.size);
            if (frame < 0) {
                continue;
            }
            double y = sortie.burstY(k) + sortie.groundScroll() - scroll;
            centred(batch, puff.get(frame), sortie.burstX(k), y + PUFF_RISE);
        }
    }

    /** Seconds since burst {@code index}'s telegraph started, at the level time {@code seconds}. */
    private double age(int index, double seconds) {
        return index < start.length && !Double.isNaN(start[index]) ? Math.max(0, seconds - start[index]) : 0;
    }

    /** The cracks' frame {@code age} s into the telegraph: across it, the last one staying. */
    static int decalFrame(double age, double telegraph, int frames) {
        if (!(telegraph > 0)) {
            return frames - 1;
        }
        return Math.clamp((int) Math.floor(age / telegraph * frames), 0, frames - 1);
    }

    /** The root's frame {@code sinceGrown} s after the growths appeared: its eruption, the last one (the crater) staying. */
    static int rootFrame(double sinceGrown, int frames) {
        return Math.clamp((int) Math.floor(sinceGrown / ERUPT_SECONDS * frames), 0, frames - 1);
    }

    /** The dust's frame {@code age} s into the telegraph, over the telegraph and the eruption; -1 once settled. */
    static int puffFrame(double age, double telegraph, int frames) {
        int frame = (int) Math.floor(age / (telegraph + ERUPT_SECONDS) * frames);
        return frame >= 0 && frame < frames ? frame : -1;
    }

    /** A growth's scale at {@code share} of its growth (0 as it appears, 1 grown): 0.35 → 1, eased. */
    static float growScale(double share) {
        return GROW_FROM + (1 - GROW_FROM) * ease(share);
    }

    /** A growth's tint channel {@code c} (r, g, b) at {@code share} of its growth: the creep's violet → its own. */
    static float growTint(double share, int c) {
        float k = ease(share);
        return CREEP[c] * (1 - k) + k;
    }

    /** A growing unit's frame at (x, y), scaled and tinted by its growth (it takes no hits while it grows). */
    static void drawGrowing(SpriteBatch batch, Enemy enemy, TextureRegion frame, double x, double y, float alpha) {
        double share = enemy.growShare(alpha);
        float scale = growScale(share);
        batch.setColor(growTint(share, 0), growTint(share, 1), growTint(share, 2), 1);
        float w = frame.getRegionWidth();
        float h = frame.getRegionHeight();
        batch.draw(
                frame,
                Math.round(PixelScreen.PLAY_FIELD_X + x - w / 2),
                Math.round(y - h / 2),
                w / 2,
                h / 2,
                w,
                h,
                scale,
                scale,
                0);
        batch.setColor(Color.WHITE);
    }

    private static float ease(double share) {
        float s = (float) Math.clamp(share, 0, 1);
        return s * s * (3 - 2 * s);
    }

    private static void centred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(PixelScreen.PLAY_FIELD_X + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
