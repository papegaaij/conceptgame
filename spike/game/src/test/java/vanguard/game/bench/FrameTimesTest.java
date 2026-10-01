package vanguard.game.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FrameTimesTest {
    @Test
    void summarisesPercentilesInMilliseconds() {
        var times = new FrameTimes(1000);
        for (int i = 1; i <= 1000; i++) {
            times.add(i * 20_000L); // 0.02 .. 20 ms
        }

        FrameTimes.Summary summary = times.summarise();

        assertEquals(1000, summary.frames());
        assertEquals(10.0, summary.p50(), 1e-9);
        assertEquals(19.8, summary.p99(), 1e-9);
        assertEquals(20.0, summary.max(), 1e-9);
        assertEquals(167, summary.framesOverBudget());
    }

    @Test
    void ignoresFramesBeyondCapacity() {
        var times = new FrameTimes(2);
        times.add(1);
        times.add(2);
        times.add(3);
        assertEquals(2, times.count());
    }
}
