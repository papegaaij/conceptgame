package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Gate 7: replays a run recorded from the real game (62 s of autopilot, recorded with
 * {@code --autopilot --record}) and compares the final state hash with the one the game printed.
 * CI runs this on Linux, Windows and macOS, so equal hashes mean the simulation is deterministic
 * across operating systems and CPUs.
 */
class ReplayTest {
    private static final String RECORDING = "/replay/autopilot-2185.rec";
    /** Printed by the game at the end of the recorded run. */
    private static final long EXPECTED_HASH = 0xe17610307e1c81f0L;
    private static final int EXPECTED_STEPS = 3720;

    @Test
    void recordedRunReplaysToTheSameStateHash() throws IOException {
        InputRecording recording;
        try (var in = getClass().getResourceAsStream(RECORDING)) {
            recording = InputRecording.read(new InputStreamReader(in, StandardCharsets.US_ASCII));
        }

        World world = recording.replay(SimConfig.gateLoad(recording.seed()));

        assertEquals(EXPECTED_STEPS, world.tick());
        assertEquals(Long.toHexString(EXPECTED_HASH), Long.toHexString(world.stateHash()));
    }
}
