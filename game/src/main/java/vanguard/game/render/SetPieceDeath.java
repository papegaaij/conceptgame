package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A huge set piece's break-up at its death, from the {@code death} entry of its pivot file
 * (tools/art/leviathan_death.py): the body stays until the swap, then the chunks it is cut into
 * drift apart from their offsets, sinking (drawn smaller), darkening and fading out; the blasts
 * over the body are a table of sprites, steps and offsets. Times are simulation steps from the
 * death, offsets px from the unit centre facing down (dx right, dy up). Pure presentation.
 */
public final class SetPieceDeath {
    /** A chunk: its frames (in place, then tumbling, each a render under the fixed light), centres and drift. */
    record Chunk(Array<AtlasRegion> frames, int[][] offsets, int driftX, int driftY) {}

    /** A blast: an explosion or cloud started {@code at} steps after the death at an offset. */
    public record Blast(int at, Array<AtlasRegion> frames, int ticksPerFrame, int dx, int dy) {}

    /** The step the body is replaced by the chunks. */
    public final int swap;
    /** The step the chunks are gone. */
    final int end;
    /** Steps per chunk frame. */
    final int frameSteps;
    /** How much smaller, darker the wreck is drawn by the end, and from which fraction of the break-up it fades. */
    final float sink;

    final float darken;
    final float fadeFrom;
    final List<Chunk> chunks = new ArrayList<>();
    public final List<Blast> blasts = new ArrayList<>();

    private SetPieceDeath(Sprites sprites, JsonValue death) {
        swap = death.getInt("swap");
        end = death.getInt("end");
        frameSteps = death.getInt("frame_steps");
        sink = death.getFloat("sink");
        darken = death.getFloat("darken");
        fadeFrom = death.getFloat("fade_from");
        for (JsonValue chunk : death.get("chunks")) {
            JsonValue offsets = chunk.get("offsets");
            int[][] centres = new int[offsets.size][];
            for (int f = 0; f < offsets.size; f++) {
                centres[f] = offsets.get(f).asIntArray();
            }
            int[] drift = chunk.get("drift").asIntArray();
            chunks.add(new Chunk(sprites.frames(chunk.getString("sprite")), centres, drift[0], drift[1]));
        }
        JsonValue steps = death.get("blast_frame_steps");
        for (JsonValue blast : death.get("blasts")) {
            String name = blast.getString("sprite");
            blasts.add(new Blast(
                    blast.getInt("at"),
                    sprites.frames(name),
                    steps.getInt(name),
                    blast.getInt("dx"),
                    blast.getInt("dy")));
        }
    }

    /** The break-up in a parsed pivot file, or null when it has none. */
    static SetPieceDeath of(Sprites sprites, JsonValue pivots) {
        JsonValue death = pivots == null ? null : pivots.get("death");
        return death == null ? null : new SetPieceDeath(sprites, death);
    }

    /** 0 at the swap to 1 at the end of the break-up, for {@code age} steps after the death. */
    float progress(float age) {
        return Math.clamp((age - swap) / (end - swap), 0f, 1f);
    }
}
