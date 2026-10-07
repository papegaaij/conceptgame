package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vanguard.sim.SimStep;

class GroundGlowTest {
    /** Simulation steps per glow frame at the loop's 8 fps (7.5 steps). */
    private static final double STEPS_PER_FRAME = (double) SimStep.PER_SECOND / GroundGlow.FRAMES_PER_SECOND;

    @Test
    void aSingleGlowFrameIsAStillLight() {
        for (long tick = 0; tick < 200; tick += 7) {
            assertEquals(0, GroundGlow.frame(tick, 1, 2, false));
            assertEquals(0, GroundGlow.frame(tick, 1, 3, true));
        }
    }

    @Test
    void severalGlowFramesLoopAtEightFramesASecond() {
        assertEquals(0, GroundGlow.frame(0, 6, 2, false));
        assertEquals(0, GroundGlow.frame(7, 6, 2, false));
        assertEquals(1, GroundGlow.frame(8, 6, 2, false));
        assertEquals(1, GroundGlow.frame(14, 6, 2, false));
        assertEquals(2, GroundGlow.frame(15, 6, 2, false));
        assertEquals(5, GroundGlow.frame((long) Math.ceil(5 * STEPS_PER_FRAME), 6, 2, false));
        assertEquals(0, GroundGlow.frame(SimStep.ticks(6.0 / GroundGlow.FRAMES_PER_SECOND), 6, 2, false));
        // Without a hit frame the loop is whole, hit or not.
        assertEquals(1, GroundGlow.frame(8, 6, 2, true));
    }

    @Test
    void aLookWithAHitFramePlaysTheSecondHalfAfterItsFirstHit() {
        long loop = SimStep.ticks(16.0 / GroundGlow.FRAMES_PER_SECOND);
        for (long tick = 0; tick < 3 * loop; tick++) {
            int intact = GroundGlow.frame(tick, 32, 3, false);
            int hit = GroundGlow.frame(tick, 32, 3, true);
            assertEquals((int) (tick / STEPS_PER_FRAME) % 16, intact);
            assertEquals(intact + 16, hit);
        }
    }

    @Test
    void anOddLoopIsNotSplit() {
        assertEquals(2, GroundGlow.frame(15, 5, 3, true));
    }
}
