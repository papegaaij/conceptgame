package vanguard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BenchRunTest {
    @Test
    void endsOnceTheTimeIsUpAndReportsTheFrames() {
        var run = new BenchRun(0.5);

        assertFalse(run.frame(0.125f));
        assertFalse(run.frame(0.25f));
        assertTrue(run.frame(0.125f));

        assertEquals("3 frames in 0.5 s (6 fps), longest frame 250.0 ms", run.report());
    }
}
