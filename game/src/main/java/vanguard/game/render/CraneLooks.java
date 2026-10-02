package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Crane Four's sprites (tools/art/crane_four.py): its arm at every drawn angle with the pivot's
 * place in each frame (assets/pivots/crane-four.json), the warning light, the clamp light and the
 * canister on the hook.
 */
final class CraneLooks {
    private final Array<AtlasRegion> arm;
    private final double[] angles;
    private final int[][] pivots;
    private final int[] lights;
    final AtlasRegion light;
    final AtlasRegion clampLight;
    final AtlasRegion canister;

    /** @param pivots the parsed pivot file, or {@code null} in a level without a crane */
    CraneLooks(Sprites sprites, JsonValue pivots) {
        if (pivots == null) {
            arm = new Array<>();
            angles = new double[0];
            this.pivots = new int[0][];
            lights = new int[0];
            light = clampLight = canister = null;
            return;
        }
        arm = sprites.frames("crane-four-arm");
        float[] degrees = pivots.get("angles").asFloatArray();
        angles = new double[degrees.length];
        for (int i = 0; i < degrees.length; i++) {
            angles[i] = Math.toRadians(degrees[i]);
        }
        JsonValue points = pivots.get("pivot");
        this.pivots = new int[points.size][];
        for (int i = 0; i < points.size; i++) {
            this.pivots[i] = points.get(i).asIntArray();
        }
        lights = pivots.get("lights").asIntArray();
        if (arm.size != angles.length) {
            throw new IllegalStateException(arm.size + " arm frames for " + angles.length + " angles");
        }
        light = sprites.region("crane-four-light");
        clampLight = sprites.region("crane-four-clamp-light");
        canister = sprites.region("crane-four-canister");
    }

    /** The frame nearest {@code radians} (from straight down, positive to the right). */
    int index(double radians) {
        int best = 0;
        for (int i = 1; i < angles.length; i++) {
            if (Math.abs(angles[i] - radians) < Math.abs(angles[best] - radians)) {
                best = i;
            }
        }
        return best;
    }

    AtlasRegion arm(int index) {
        for (AtlasRegion region : arm) {
            if (region.index == index) {
                return region;
            }
        }
        throw new IllegalStateException("no arm frame " + index);
    }

    /** The pivot's px from the frame's top left. */
    int[] pivot(int index) {
        return pivots[index];
    }

    double angle(int index) {
        return angles[index];
    }

    /** Where the warning lights sit along the jib, px from the pivot. */
    int[] lights() {
        return lights;
    }
}
