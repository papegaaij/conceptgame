package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vanguard.sim.LevelScript;

/**
 * A huge set piece's sprites (tools/art/leviathan.py): the second pass's body facing down in its
 * sway frames with every shootable part as a sprite of its own, intact and wrecked, centred on its
 * data offset (the fluke per sway frame on its pivot from assets/pivots/{@code <slug>}.json), the
 * additive glow over the vital part, and the first pass's whole unit already drawn at its diagonal
 * heading and at the high-air scale. Lit sprites are never rotated or mirrored.
 */
final class SetPieceLooks {
    /** The sway plays its three frames 0-1-2-1, 0.3 s each (the review loop's timing). */
    private static final int[] SWAY = {0, 1, 2, 1};

    private static final double SWAY_FRAME_SECONDS = 0.3;

    /** The second pass facing down: the body per sway frame. */
    final Array<AtlasRegion> down;
    /** The first pass crossing on high-air, per sway frame. */
    final Array<AtlasRegion> cross;
    /** Per part: its frames intact and wrecked (one, or one per sway frame), or null for a part drawn as a glow. */
    private final List<Array<AtlasRegion>> intact = new ArrayList<>();

    private final List<Array<AtlasRegion>> wrecked = new ArrayList<>();
    /** Per part and sway frame: the sprite's centre from the unit's, px right and up; null for the data offset. */
    private final int[][][] pivots;
    /** The glow over the vital part, additive; null for none. */
    final AtlasRegion glow;
    /** The part the glow sits on. */
    final int glowPart;

    /** @param pivots the parsed pivot file ({@code {"down": {"<part>": [[dx, dy], ...]}}}), or null */
    SetPieceLooks(Sprites sprites, LevelScript.SetPieceSpec spec, JsonValue pivots) {
        String slug = spec.slug();
        down = sprites.frames(slug + "-down");
        cross = sprites.frames(slug + "-cross");
        List<LevelScript.PartSpec> parts = spec.parts();
        this.pivots = new int[parts.size()][][];
        AtlasRegion glowRegion = null;
        int glowAt = -1;
        for (int p = 0; p < parts.size(); p++) {
            String name = slug + "-" + spriteName(parts.get(p).name());
            if (sprites.has(name)) {
                intact.add(sprites.frames(name));
                wrecked.add(sprites.frames(name + "-wrecked"));
            } else if (sprites.has(name + "-glow")) {
                intact.add(null);
                wrecked.add(null);
                glowRegion = sprites.region(name + "-glow");
                glowAt = p;
            } else {
                throw new IllegalStateException(
                        "no sprite '" + name + "' or '" + name + "-glow' for a part of " + slug);
            }
            JsonValue pivot = pivots == null
                    ? null
                    : pivots.get("down").get(spriteName(parts.get(p).name()));
            if (pivot != null) {
                this.pivots[p] = new int[pivot.size][];
                for (int k = 0; k < pivot.size; k++) {
                    this.pivots[p][k] = pivot.get(k).asIntArray();
                }
            }
        }
        glow = glowRegion;
        glowPart = glowAt;
    }

    /** A part's sprite name: {@code vent 1} is {@code vent-1}, {@code left fin} is {@code fin-left} (the screen side last). */
    static String spriteName(String part) {
        String[] words = part.toLowerCase(Locale.ROOT).split(" ");
        if (words.length == 2 && (words[0].equals("left") || words[0].equals("right"))) {
            return words[1] + "-" + words[0];
        }
        return String.join("-", words);
    }

    /** The sway frame at {@code seconds}. */
    static int sway(double seconds) {
        return SWAY[(int) Math.floorMod((long) Math.floor(seconds / SWAY_FRAME_SECONDS), (long) SWAY.length)];
    }

    /** Whether part {@code p} has sprites of its own (rather than a glow). */
    boolean drawn(int p) {
        return intact.get(p) != null;
    }

    /** Part {@code p}'s sprite in sway frame {@code k}. */
    AtlasRegion part(int p, int k, boolean isWrecked) {
        Array<AtlasRegion> frames = isWrecked ? wrecked.get(p) : intact.get(p);
        return frames.get(k % frames.size);
    }

    /** Whether part {@code p} sits on a pivot per sway frame rather than on its data offset. */
    boolean pivoted(int p) {
        return pivots[p] != null;
    }

    /** Part {@code p}'s centre in sway frame {@code k}, px right and up from the unit centre. */
    int[] pivot(int p, int k) {
        return pivots[p][k % pivots[p].length];
    }
}
