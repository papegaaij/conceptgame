package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Runs the real decoder and loop logic headless on the round-08 "Coalition Rising" track and
 * compares the stream across two loop boundaries with a straight decode of the file.
 */
class MusicLoopTest {
    private static final Path TRACK = Path.of(System.getProperty("spike.designDir", "../../design"),
            "audio/music/concept/coalition-rising-full-r08-a.ogg");
    private static final int SEAM_FRAMES = 2048;

    private static byte[] ogg;
    private static short[] straight;
    private static LoopPoints loop;

    @BeforeAll
    static void decodeTheWholeTrack() throws IOException {
        ogg = Files.readAllBytes(TRACK);
        try (VorbisFile file = new VorbisFile(ogg)) {
            loop = file.loopPoints();
            straight = new short[Math.toIntExact(file.frameCount() * file.channels())];
            int frame = 0;
            for (int read; (read = file.read(straight, frame * 2, 4096)) > 0; ) {
                frame += read;
            }
            assertEquals(file.frameCount(), frame, "straight decode length");
        }
    }

    @Test
    void readsTheLoopPointsFromTheVorbisComments() {
        assertEquals(new LoopPoints(340_772, 340_772 + 5_773_091), loop);
    }

    @Test
    void streamIsSampleExactAcrossTwoLoopBoundaries() {
        try (VorbisFile file = new VorbisFile(ogg)) {
            var stream = new LoopingStream(file, loop);
            short[] chunk = new short[2 * 4093]; // deliberately not a divisor of anything
            long total = loop.end() + loop.length() + 50_000;
            for (long frame = 0; frame < total; ) {
                stream.read(chunk);
                for (int i = 0; i < chunk.length / 2; i++, frame++) {
                    int source = Math.toIntExact(LoopingStreamTest.expectedSourceFrame(frame, loop));
                    if (chunk[2 * i] != straight[2 * source] || chunk[2 * i + 1] != straight[2 * source + 1]) {
                        throw new AssertionError("output frame " + frame + " differs from source frame " + source);
                    }
                }
            }
        }
    }

    /**
     * The file continues past the loop end with a repeat of the loop start (then fades out). That
     * repeat follows the loop instead of the intro, so release tails differ slightly, but it must
     * match the loop start clearly best at offset 0: the loop points line up with the decoded audio
     * to the sample.
     */
    @Test
    void loopPointsLineUpWithTheDecodedAudio() {
        double atZero = seamError(0);
        double signal = 0;
        for (int i = 0; i < SEAM_FRAMES; i++) {
            double sample = straight[2 * (int) (loop.end() + i)];
            signal += sample * sample;
        }
        assertTrue(atZero < 0.02 * signal, "seam error " + atZero + " vs signal energy " + signal);
        for (int shift : new int[] {-2, -1, 1, 2}) {
            double shifted = seamError(shift);
            assertTrue(atZero * 4 < shifted, "offset " + shift + " fits nearly as well: " + shifted + " vs " + atZero);
        }
    }

    /** Squared difference of the left channel after the loop end and after the loop start + shift. */
    private static double seamError(int shift) {
        double error = 0;
        for (int i = 0; i < SEAM_FRAMES; i++) {
            double diff = straight[2 * (int) (loop.end() + i)] - straight[2 * (int) (loop.start() + i + shift)];
            error += diff * diff;
        }
        return error;
    }
}
