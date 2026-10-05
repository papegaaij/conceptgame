package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

class EffectsTest {
    /** Level 04's ground scroll, px/s, over one simulation step at 60 steps a second. */
    private static final double SCROLL_PER_STEP = 120.0 / 60;

    private final Effects debris = Effects.solid();
    private final Effects glows = Effects.glowing();

    /** Frames whose pictures the positions do not need. */
    private static Array<AtlasRegion> frames(int count) {
        Array<AtlasRegion> frames = new Array<>();
        for (int i = 0; i < count; i++) {
            frames.add(null);
        }
        return frames;
    }

    @Test
    void aGroundUnitsDeathKeepsItsOffsetToTheHuskWhileTheGroundScrolls() {
        double scroll = 1000;
        // The Scuttler's husk on the ground and its burst a little above its centre.
        debris.startOnGround(frames(1), 600, 200, 300, 0, scroll);
        glows.startOnGround(frames(8), 3, 200, 310, 0, scroll);

        for (int step = 0; step < 20; step++) {
            scroll += SCROLL_PER_STEP;
            debris.step();
            glows.step();
            assertEquals(10, glows.y(0, scroll) - debris.y(0, scroll), 1e-9);
        }
        assertEquals(310 - 20 * SCROLL_PER_STEP, glows.y(0, scroll), 1e-9);
    }

    @Test
    void itFollowsTheArenasSlowScrollAndAnEarlyKillsJump() {
        glows.startOnGround(frames(8), 3, 200, 300, 4, 500);

        assertEquals(299.5, glows.y(0, 500.5), 1e-9);
        assertEquals(-100, glows.y(0, 900), 1e-9);
    }

    @Test
    void aFlyersDeathStaysAtItsPlayFieldPosition() {
        glows.start(frames(8), 3, 200, 300);
        glows.startOnGround(frames(8), 3, 200, 300, 0, 1000);

        assertEquals(2, glows.size());
        assertEquals(300, glows.y(0, 1240), 1e-9);
        assertEquals(60, glows.y(1, 1240), 1e-9);
    }
}
