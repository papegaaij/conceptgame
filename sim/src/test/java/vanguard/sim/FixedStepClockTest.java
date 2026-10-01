package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FixedStepClockTest {
    private static final double STEP = 1.0 / 60;

    @Test
    void carriesTheRemainderIntoTheNextFrame() {
        var clock = new FixedStepClock(STEP, 5);
        assertEquals(0, clock.advance(STEP * 0.6));
        assertEquals(0.6f, clock.alpha(), 1e-5f);
        assertEquals(1, clock.advance(STEP * 0.6));
        assertEquals(0.2f, clock.alpha(), 1e-5f);
    }

    @Test
    void clampsLongStalls() {
        var clock = new FixedStepClock(STEP, 5);
        assertEquals(5, clock.advance(2.0));
    }
}
