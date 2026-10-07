package vanguard.game.render;

import vanguard.sim.SimStep;

/**
 * Which of a trigger look's {@code -glow} frames shows (design/tech/architecture, ground targets): one
 * frame is a still light (Level 06's terminal); more loop at {@value #FRAMES_PER_SECOND} frames a
 * second (Level 08's billboard flicker, its irregular steps baked into the frames). A look with a
 * hit frame (three frames: intact, hit, spent) splits an even loop in halves: the first plays until
 * its first hit, the second after it. Presentation only: it reads the simulation's step count.
 */
final class GroundGlow {
    /** A looping glow's frame rate. */
    static final int FRAMES_PER_SECOND = 8;

    private GroundGlow() {}

    /**
     * @param tick the simulation step
     * @param glowFrames the look's glow frames
     * @param lookFrames the look's own frames (three with a hit frame)
     * @param hit whether the object has taken a hit
     */
    static int frame(long tick, int glowFrames, int lookFrames, boolean hit) {
        if (glowFrames <= 1) {
            return 0;
        }
        boolean halves = lookFrames > 2 && glowFrames % 2 == 0;
        int loop = halves ? glowFrames / 2 : glowFrames;
        int step = (int) (tick * FRAMES_PER_SECOND / SimStep.PER_SECOND % loop);
        return halves && hit ? loop + step : step;
    }
}
