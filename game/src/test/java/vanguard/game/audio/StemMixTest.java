package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StemMixTest {
    private static final Path MUSIC = Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "music");

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
    void theBaseStemPlaysAloneUntilTheMixMoves() {
        var mix = new StemMix(new Constant((short) 1000, 100), new Constant((short) 3000, 100), 1);
        short[] out = new short[50];

        mix.read(out);

        assertEquals(1000, out[0]);
        assertEquals(1000, out[49]);
    }

    @Test
    void theCrossfadeRampsSampleBySampleOverTheFadeTime() {
        var mix = new StemMix(new Constant((short) 1000, 100), new Constant((short) 3000, 100), 1);
        mix.fadeTo(1);
        short[] out = new short[100];

        mix.read(out);

        for (int i = 1; i < out.length; i++) {
            int step = out[i] - out[i - 1];
            assertTrue(step >= 0 && step <= 21, "a step of " + step + " at " + i);
        }
        assertEquals(3000, out[99], "full after the fade time of 100 frames");
        mix.read(out);
        assertEquals(3000, out[0]);
    }

    @Test
    void theStemsMustShareTheirFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StemMix(new Constant((short) 0, 100), new Constant((short) 0, 200), 1));
    }

    @Test
    void coalitionRisingsStemsAreSampleAligned() throws IOException {
        var base = new VorbisFile(Files.readAllBytes(MUSIC.resolve("coalition-rising-base.ogg")));
        var full = new VorbisFile(Files.readAllBytes(MUSIC.resolve("coalition-rising.ogg")));
        assertEquals(full.frameCount(), base.frameCount());
        assertEquals(full.loopPoints(), base.loopPoints());
        // The mix owns the two files and closes them.
        StemMix.of(base, full, 1, 0).close();
    }
}
