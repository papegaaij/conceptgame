package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BossCueTest {
    /** A mono source of {@code length} frames of one sample value. */
    private static final class Warning implements PcmSource {
        private final int length;
        private final short value;
        private int position;

        Warning(int length, short value) {
            this.length = length;
            this.value = value;
        }

        @Override
        public int read(short[] out, int offset, int frames) {
            int count = Math.min(frames, length - position);
            Arrays.fill(out, offset, offset + count, value);
            position += count;
            return count;
        }

        @Override
        public void seek(long frame) {
            position = (int) frame;
        }

        @Override
        public int channels() {
            return 1;
        }

        @Override
        public int sampleRate() {
            return 10;
        }

        @Override
        public long frameCount() {
            return length;
        }

        @Override
        public void close() {}
    }

    /** An endless mono stream counting up from 1 (its frame number plus one). */
    private static class Track implements PcmStream {
        private short next = 1;

        @Override
        public void read(short[] out) {
            for (int i = 0; i < out.length; i++) {
                out[i] = next++;
            }
        }

        @Override
        public int channels() {
            return 1;
        }

        @Override
        public int sampleRate() {
            return 10;
        }

        @Override
        public void close() {}
    }

    @Test
    void theBossTrackComesInOnTheFrameAfterTheWarningsBarsUnderItsTail() {
        var cue = new BossCue(new Warning(12, (short) 1000), new Track(), 8);
        short[] out = new short[5];

        cue.read(out);
        assertEquals(1000, out[4], "the warning alone");
        cue.read(out);
        // Frames 5–9: the warning; the track from frame 8 (its first two samples).
        assertEquals(1000, out[2]);
        assertEquals(1001, out[3], "the track's first frame on frame 8");
        assertEquals(1002, out[4]);
        cue.read(out);
        // Frames 10–14: the warning's tail ends after frame 11.
        assertEquals(1003, out[0]);
        assertEquals(1004, out[1]);
        assertEquals(5, out[2], "the track alone once the warning has ended");
        cue.read(out);
        assertEquals(8, out[0], "the track keeps its own frames across the chunks");
    }

    @Test
    void theMixIsClampedRatherThanWrappingAround() {
        var cue = new BossCue(new Warning(10, Short.MAX_VALUE), new Track(), 0);
        short[] out = new short[4];

        cue.read(out);

        assertEquals(Short.MAX_VALUE, out[3]);
    }

    @Test
    void aWarningAloneEndsInSilenceAndATrackAloneStartsAtOnce() {
        var warning = new BossCue(new Warning(3, (short) 50), null, 100);
        short[] out = new short[5];
        warning.read(out);
        assertEquals(50, out[2]);
        assertEquals(0, out[3]);

        var track = new BossCue(null, new Track(), 100);
        track.read(out);
        assertEquals(1, out[0], "no warning, so the track does not wait");
    }

    @Test
    void itHasAPartAndOneFormat() {
        assertThrows(IllegalArgumentException.class, () -> new BossCue(null, null, 0));
        PcmStream stereo = new Track() {
            @Override
            public int channels() {
                return 2;
            }
        };
        assertThrows(IllegalArgumentException.class, () -> new BossCue(new Warning(1, (short) 0), stereo, 0));
    }
}
