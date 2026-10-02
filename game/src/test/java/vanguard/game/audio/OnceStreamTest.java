package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OnceStreamTest {
    /** A mono source of {@code length} frames of the sample 7 at 10 frames a second. */
    private static final class Constant implements PcmSource {
        private final int length;
        private int position;

        Constant(int length) {
            this.length = length;
        }

        @Override
        public int read(short[] out, int offset, int frames) {
            int count = Math.min(frames, length - position);
            Arrays.fill(out, offset, offset + count, (short) 7);
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

    @Test
    void theSourcePlaysOnceThenSilenceAndItEndsHalfASecondLater() {
        OnceStream stream = new OnceStream(new Constant(3));
        short[] chunk = new short[4];

        stream.read(chunk);
        assertArrayEquals(new short[] {7, 7, 7, 0}, chunk);
        assertFalse(stream.ended(), "one frame of silence so far");

        stream.read(chunk);
        assertArrayEquals(new short[4], chunk);
        assertTrue(stream.ended(), "five frames of silence: half a second at 10 frames a second");
    }
}
