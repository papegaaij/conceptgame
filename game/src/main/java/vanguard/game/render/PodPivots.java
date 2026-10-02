package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Where the wing pods sit on the ship (assets/pivots/pods.json, written by tools/art/stormhawk.py):
 * per pod sprite and banking frame, its top-left on the 48x48 hull sprite, y down.
 */
public final class PodPivots {
    private final JsonValue offsets;

    public PodPivots(Files files) {
        offsets = new JsonReader().parse(files.internal("pivots/pods.json")).get("offsets");
    }

    /** The offsets of a pod sprite, hard left .. hard right, each {@code [x, y]}. */
    int[][] offsets(String pod) {
        JsonValue frames = offsets.get(pod);
        if (frames == null) {
            throw new IllegalStateException("no pivots for '" + pod + "' in pods.json");
        }
        int[][] out = new int[frames.size][];
        for (int i = 0; i < frames.size; i++) {
            out[i] = frames.get(i).asIntArray();
        }
        return out;
    }
}
