package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vanguard.sim.InputRecording;
import vanguard.sim.LevelResult;
import vanguard.sim.Sortie;

/**
 * Replays a recorded run of the whole of Level 01 at medium (seed 2185, flown by the
 * {@link Autopilot}) with the specs built from the design data, and compares the final state
 * hash, kills and credits with the recorded run. CI runs this on Linux, Windows and macOS, so
 * equal hashes mean the simulation is deterministic across operating systems and CPUs.
 *
 * <p>Re-record deliberately when the rules or the numbers change:
 * {@code ./gradlew :content:test --tests vanguard.content.ReplayTest -Dvanguard.recordDir=content/src/test/resources/replay},
 * then update the expected values from the test output.
 */
class ReplayTest {
    private static final String RECORDING = "level-01-medium-2185.rec";
    private static final long SEED = 2185;
    private static final long EXPECTED_HASH = 0xf3ae19deb416eeadL;
    private static final int EXPECTED_KILLS = 90;
    private static final int EXPECTED_CREDITS = 916;

    @Test
    void recordedRunReplaysToTheSameStateHash() throws IOException {
        InputRecording recording;
        try (var in = getClass().getResourceAsStream("/replay/" + RECORDING)) {
            recording = InputRecording.read(new InputStreamReader(in, StandardCharsets.US_ASCII));
        }
        Sortie sortie = Level01Test.sortie(ContentLoader.fromClasspath(), recording.seed(), Difficulty.MEDIUM);

        for (int commands : recording.commands()) {
            sortie.step(commands);
        }

        LevelResult result = sortie.result();
        System.out.printf(
                "replay: %d steps, kills %d, credits %d, hash %s%n",
                sortie.tick(), result.kills(), result.credits().total(), Long.toHexString(sortie.stateHash()));
        assertTrue(sortie.complete());
        assertEquals(EXPECTED_KILLS, result.kills());
        assertEquals(EXPECTED_CREDITS, result.credits().total());
        assertEquals(Long.toHexString(EXPECTED_HASH), Long.toHexString(sortie.stateHash()));
    }

    @Test
    @EnabledIfSystemProperty(named = "vanguard.recordDir", matches = ".+")
    void recordAnew() throws IOException {
        Sortie sortie = Level01Test.sortie(ContentLoader.fromClasspath(), SEED, Difficulty.MEDIUM);
        var recorder = new InputRecording.Recorder(SEED);
        while (!sortie.complete()) {
            int commands = Autopilot.commands(sortie);
            recorder.record(commands);
            sortie.step(commands);
        }
        Path file = Path.of(System.getProperty("vanguard.recordDir")).resolve(RECORDING);
        recorder.writeTo(Files.newBufferedWriter(file, StandardCharsets.US_ASCII));
        System.out.println("recorded " + file.toAbsolutePath());
    }
}
