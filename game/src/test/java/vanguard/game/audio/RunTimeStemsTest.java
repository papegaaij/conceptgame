package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M5 part C, the music's run-time hook ({@code full_on}, design/audio/music): the full mix comes in
 * over a second as a hold zone starts and goes back to the base stem over 4 s after it ends.
 */
class RunTimeStemsTest {
    /** A mono stream of one constant sample value. */
    private record Constant(short value, int sampleRate) implements PcmStream {
        @Override
        public void read(short[] out) {
            java.util.Arrays.fill(out, value);
        }

        @Override
        public int channels() {
            return 1;
        }

        @Override
        public void close() {}
    }

    @Test
    void theHooksFullMixComesInOverASecondAndGoesOutOverFour() {
        assertEquals(1, LevelMusic.fadeSeconds(1, false));
        assertEquals(1, LevelMusic.fadeSeconds(1, true));
        assertEquals(LevelMusic.RUN_TIME_OUT_SECONDS, LevelMusic.fadeSeconds(0, true));
        assertEquals(4, LevelMusic.RUN_TIME_OUT_SECONDS);
        // A section's own change back to the base stem keeps the one-second crossfade.
        assertEquals(1, LevelMusic.fadeSeconds(0, false));
    }

    @Test
    void aFadeWithItsOwnTimeTakesThatLongAndKeepsItsPace() {
        var mix = new StemMix(new Constant((short) 1000, 100), new Constant((short) 3000, 100), 1);
        mix.fadeTo(1);
        short[] second = new short[100];
        mix.read(second);
        assertEquals(3000, second[99], "in over the default second");

        mix.fadeTo(0, 4);
        short[] out = new short[400];
        mix.read(out);
        for (int i = 1; i < out.length; i++) {
            int step = out[i - 1] - out[i];
            assertTrue(step >= 0 && step <= 6, "a step of " + step + " at " + i);
        }
        assertTrue(out[199] > 1900 && out[199] < 2100, "half way after 2 s: " + out[199]);
        assertEquals(1000, out[399], "the base stem alone after 4 s");

        mix.fadeTo(1);
        mix.read(second);
        assertEquals(3000, second[99], "the next plain change is back to a second");
    }
}
