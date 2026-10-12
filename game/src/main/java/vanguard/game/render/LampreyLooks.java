package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import vanguard.sim.SimStep;

/**
 * M5 part F, a latching chaser's own poses (design/enemies/air/lamprey; tools/art/lamprey.py): its
 * latched pose at its hull spot ({@code lamprey-latched}, {@value #LATCHED_FRAMES} frames a spot,
 * indexed {@code spot * 4 + frame}), drawn over the ship with latch look a's "gulp": the ship's own
 * shield ring pulsing with each swallow (the {@code gulp_pulse} of its pivot file, 0.55, 0.3, 0.12, 0
 * of the ring's strength on the gulp's frames); and its stun, thrown off and tumbling a full turn
 * ({@code lamprey-stunned}, the frame by the stun's progress). Chasing, flying on and fleeing it is
 * drawn with its 16-angle frames, and it dies in its {@code small} wet pop ({@link EnemyLooks}).
 *
 * @param latched the latched frames, {@value #LATCHED_FRAMES} per spot
 * @param stunned the stun's tumble
 * @param gulp the shield ring's strength on each latched frame
 */
record LampreyLooks(Array<AtlasRegion> latched, Array<AtlasRegion> stunned, float[] gulp) {
    /** The latched pose's frames a spot (a 0.4 s gulp at 10 fps). */
    static final int LATCHED_FRAMES = 4;

    static final double FPS = 10;
    /** Latch look a's gulp (tools/art/lamprey.py), used when the pivot file does not give it. */
    static final float[] GULP = {0.55f, 0.3f, 0.12f, 0};

    /** The looks of a unit with them ({@code <slug>-latched} and {@code -stunned}); null for another. */
    static LampreyLooks of(Sprites sprites, String slug, JsonValue pivots) {
        if (!sprites.has(slug + "-latched") || !sprites.has(slug + "-stunned")) {
            return null;
        }
        float[] gulp = GULP;
        if (pivots != null && pivots.has("gulp_pulse")) {
            gulp = pivots.get("gulp_pulse").asFloatArray();
        }
        return new LampreyLooks(sprites.frames(slug + "-latched"), sprites.frames(slug + "-stunned"), gulp);
    }

    /** The latched frame's index at {@code spot} at step {@code tick}, each unit at its {@code phase}. */
    static int latchedIndex(int spot, long tick, int phase) {
        return spot * LATCHED_FRAMES + gulpFrame(tick, phase);
    }

    /** The gulp's frame (0 to 3) at step {@code tick}. */
    static int gulpFrame(long tick, int phase) {
        return (int) Math.floorMod((long) Math.floor(tick * FPS / SimStep.PER_SECOND) + phase, (long) LATCHED_FRAMES);
    }

    /** The stun's frame at its progress (0 thrown off, 1 the stun's end): a full turn over the stun. */
    static int stunnedIndex(double progress, int frames) {
        return Math.clamp((int) Math.floor(progress * frames), 0, frames - 1);
    }

    /** The shield ring's strength on gulp frame {@code frame}. */
    float gulpStrength(int frame) {
        return gulp[Math.floorMod(frame, gulp.length)];
    }
}
