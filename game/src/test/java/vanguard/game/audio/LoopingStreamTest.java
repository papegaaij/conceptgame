package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoopingStreamTest {
    /** Stereo source whose frame {@code n} holds the samples {@code n} and {@code -n}. */
    private static final class CountingSource implements PcmSource {
        private final int length;
        private int position;

        CountingSource(int length) {
            this.length = length;
        }

        @Override
        public int read(short[] out, int offset, int frames) {
            int count = Math.min(frames, length - position);
            for (int i = 0; i < count; i++, position++) {
                out[offset + 2 * i] = (short) position;
                out[offset + 2 * i + 1] = (short) -position;
            }
            return count;
        }

        @Override
        public void seek(long frame) {
            position = (int) frame;
        }

        @Override
        public int channels() {
            return 2;
        }

        @Override
        public int sampleRate() {
            return 44_100;
        }

        @Override
        public long frameCount() {
            return length;
        }

        @Override
        public void close() {}
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 64, 1000, 5000})
    void playsTheIntroOnceThenRepeatsTheLoopWithoutGapOrDuplicate(int framesPerRead) {
        var loop = new LoopPoints(300, 1300);
        var stream = new LoopingStream(new CountingSource(1500), loop);
        short[] chunk = new short[framesPerRead * 2];

        int frame = 0;
        while (frame < 5000) {
            stream.read(chunk);
            for (int i = 0; i < framesPerRead; i++, frame++) {
                short expected = (short) expectedSourceFrame(frame, loop);
                assertEquals(expected, chunk[2 * i], "left channel of output frame " + frame);
                assertEquals((short) -expected, chunk[2 * i + 1], "right channel of output frame " + frame);
            }
        }
    }

    /** Which source frame output frame {@code n} must come from. */
    static long expectedSourceFrame(long n, LoopPoints loop) {
        return n < loop.end() ? n : loop.start() + (n - loop.start()) % loop.length();
    }
}
