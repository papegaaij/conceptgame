package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import vanguard.sim.InputRecording;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;

/**
 * Replays a recorded minute of the test sortie (seed 2185; an aiming autopilot that also drifts
 * into Skitters now and then), flown with the specs built from the design data, and compares the
 * final state hash and the combat totals with the recorded run. CI runs this on Linux, Windows and
 * macOS, so equal hashes mean the simulation is deterministic across operating systems and CPUs.
 * Re-record deliberately when the rules or the numbers change.
 */
class ReplayTest {
    private static final String RECORDING = "/replay/test-sortie-2185.rec";
    private static final long EXPECTED_HASH = 0x6d6181b0b974e1bbL;
    private static final int EXPECTED_STEPS = 3600;

    @Test
    void recordedRunReplaysToTheSameStateHash() throws IOException {
        InputRecording recording;
        try (var in = getClass().getResourceAsStream(RECORDING)) {
            recording = InputRecording.read(new InputStreamReader(in, StandardCharsets.US_ASCII));
        }
        Content content = ContentLoader.fromClasspath();
        var sortie = new Sortie(recording.seed(), SimSpecs.starterLoadout(content), SimSpecs.skitter(content));
        int destroyed = 0;
        int armourHits = 0;

        for (int commands : recording.commands()) {
            sortie.step(commands);
            destroyed += count(sortie, SimEvents.Type.ENEMY_DESTROYED);
            armourHits += count(sortie, SimEvents.Type.ARMOUR_HIT);
        }

        assertEquals(EXPECTED_STEPS, sortie.tick());
        assertEquals(34, destroyed);
        assertEquals(3, armourHits);
        assertEquals(Long.toHexString(EXPECTED_HASH), Long.toHexString(sortie.stateHash()));
    }

    private static int count(Sortie sortie, SimEvents.Type type) {
        int count = 0;
        for (int i = 0; i < sortie.events().size(); i++) {
            if (sortie.events().type(i) == type) {
                count++;
            }
        }
        return count;
    }
}
