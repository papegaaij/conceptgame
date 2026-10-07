package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vanguard.sim.SimStep;

/**
 * M5 part C (design/tech/architecture, the backdrop's real clock): a piece's path runs on real time
 * from the moment the level clock reached its first waypoint, so traffic keeps its speed while a hold
 * zone slows the level clock; outside holds the two clocks run together.
 */
class BackdropClockTest {
    /** Steps both clocks: {@code steps} steps with the level clock at {@code rate}, from (script, real). */
    private static double[] run(BackdropClock clock, double script, double real, int steps, double rate) {
        for (int i = 0; i < steps; i++) {
            script += rate * SimStep.SECONDS;
            real += SimStep.SECONDS;
            clock.record(script, real);
        }
        clock.at(script, real);
        return new double[] {script, real};
    }

    @Test
    void withoutAHoldPathsRunOnTheLevelClock() {
        BackdropClock clock = new BackdropClock();
        run(clock, 0, 0, SimStep.ticks(30), 1);
        assertEquals(30, clock.pathTime(20), 1e-6);
        assertEquals(30, clock.pathTime(40), 1e-6, "before its first waypoint it rests there");
        assertEquals(30, clock.real(), 1e-6);
    }

    @Test
    void aPathStartedBeforeAHoldRunsOnInRealTime() {
        BackdropClock clock = new BackdropClock();
        double[] at = run(clock, 0, 0, SimStep.ticks(50), 1);
        // A 10 s hold at a fifth of the speed: the level clock gains only 2 s.
        at = run(clock, at[0], at[1], SimStep.ticks(10), 0.2);
        assertEquals(52, clock.script(), 1e-6);
        assertEquals(60, clock.real(), 1e-6);
        assertEquals(60, clock.pathTime(45), 1e-6, "started at 45: 15 real seconds along");
        // One reached inside the hold, at script 51 (5 real seconds into it), is 5 real seconds along.
        assertEquals(56, clock.pathTime(51), 0.05);
        // After the hold the clocks run together again, 8 s apart.
        run(clock, at[0], at[1], SimStep.ticks(10), 1);
        assertEquals(70, clock.pathTime(45), 1e-6);
        assertEquals(62, clock.pathTime(60), 1e-6, "reached after the hold (real 68): 2 s along");
    }

    @Test
    void aRetryForgetsTheHolds() {
        BackdropClock clock = new BackdropClock();
        double[] at = run(clock, 0, 0, SimStep.ticks(20), 1);
        run(clock, at[0], at[1], SimStep.ticks(10), 0.2);
        // The attempt starts over: both clocks back at 0.
        run(clock, 0, 0, SimStep.ticks(5), 1);
        assertEquals(5, clock.pathTime(1), 1e-6);
        clock.reset();
        assertEquals(0, clock.offsetAt(10), 1e-9);
    }
}
