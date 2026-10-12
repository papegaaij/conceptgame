package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartGSpecs.APPROACH;
import static vanguard.sim.PartGSpecs.runTo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part G's timed halt (design/campaign Level 13, The halt; the stated defaults of 2026-10-11): a
 * section of speed 0 without an arena, the level clock running on one step per step, the scroll easing
 * into it over the last 2 s before it and out of it over the first 2 s after it.
 */
class TimedHaltTest {
    private static LevelScript level(List<WaveSpec> waves) {
        return PartGSpecs.level(Map.of(), waves, List.of());
    }

    @Test
    void theScrollEasesToAHaltOverTheLastTwoSecondsAndTheClockRunsOn() {
        List<LevelScript.Section> sections = PartGSpecs.sections();
        int halt = SimStep.ticks(40);
        int ease = SimStep.ticks(2);
        assertEquals(APPROACH, Sortie.timedHaltSpeed(sections, 0));
        assertEquals(APPROACH, Sortie.timedHaltSpeed(sections, halt - ease - 1));
        double before = APPROACH;
        for (int tick = halt - ease; tick < halt; tick++) {
            double speed = Sortie.timedHaltSpeed(sections, tick);
            assertTrue(speed < before, "easing down at step " + tick);
            before = speed;
        }
        assertEquals(0, Sortie.timedHaltSpeed(sections, halt - 1), "the last step before the halt is at rest");
        assertEquals(0, Sortie.timedHaltSpeed(sections, halt));
        assertEquals(0, Sortie.timedHaltSpeed(sections, SimStep.ticks(219.9)));

        Sortie sortie = PartGSpecs.sortie(level(List.of()));
        runTo(sortie, 100);
        assertEquals(SimStep.ticks(100), sortie.tick(), "the clock runs one step per step in the halt");
        assertEquals(1, sortie.scriptRate());
        assertTrue(sortie.timedHalted());
        double scroll = sortie.groundScroll();
        runTo(sortie, 200);
        assertEquals(scroll, sortie.groundScroll(), "nothing scrolls in the halt");
        assertEquals(0, sortie.groundSpeed());
    }

    @Test
    void theScrollEasesBackUpOverTheFirstTwoSecondsAfterTheHalt() {
        List<LevelScript.Section> sections = PartGSpecs.sections();
        int end = SimStep.ticks(220);
        int ease = SimStep.ticks(2);
        double before = 0;
        for (int tick = end; tick < end + ease; tick++) {
            double speed = Sortie.timedHaltSpeed(sections, tick);
            assertTrue(speed > before, "easing up at step " + tick);
            before = speed;
        }
        assertEquals(APPROACH, Sortie.timedHaltSpeed(sections, end + ease - 1));
        assertEquals(APPROACH, Sortie.timedHaltSpeed(sections, end + ease));

        Sortie sortie = PartGSpecs.sortie(level(List.of()));
        runTo(sortie, 223);
        assertEquals(APPROACH, sortie.groundSpeed());
        assertFalse(sortie.timedHalted());
    }

    @Test
    void theApproachScrollsAboutFiveThousandFourHundredAndSixtyPixelsWithItsEase() {
        LevelScript level = level(List.of());
        double approach = Sortie.scrollAt(level, 40);
        // 38 s at 140 px/s and half of 2 s at 140 px/s in the smoothstep ease (the README's 5,460 px).
        assertEquals(5460, approach, 1.5);

        Sortie sortie = PartGSpecs.sortie(level);
        runTo(sortie, 40);
        assertEquals(approach, sortie.groundScroll(), 1e-9, "the sortie scrolls exactly as the sum says");
        runTo(sortie, 235);
        // The exit: 15 s at 140 px/s less half the 2 s ease.
        assertEquals(approach + 15 * APPROACH - APPROACH, sortie.groundScroll(), 1.5);
    }

    @Test
    void wavesInTheHaltKeepTheirAuthoredTimes() {
        WaveSpec late = TestSpecs.wave(
                150, WaveSpec.Formation.SINGLE, TestSpecs.NEEDLER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Sortie sortie = PartGSpecs.untouchable(level(List.of(late)));
        runTo(sortie, 149.98);
        assertEquals(0, sortie.enemyCount());
        runTo(sortie, 150.02);
        assertEquals(1, sortie.enemyCount(), "the wave of t=150 enters at 150 s of real time");
    }

    @Test
    void aTimedHaltComesWithoutHoldsOrAnArenaAndTellsWhatLiesInIt() {
        LevelScript level = level(List.of());
        assertTrue(level.timedHalts());
        assertTrue(level.timedHaltAt(40));
        assertTrue(level.timedHaltAt(219.9));
        assertFalse(level.timedHaltAt(39.9));
        assertFalse(level.timedHaltAt(220));
        assertThrows(
                IllegalArgumentException.class,
                () -> PartGSpecs.level(
                        List.of(
                                new LevelScript.Section(40, APPROACH),
                                new LevelScript.Section(220, 0),
                                new LevelScript.Section(260, 0, true)),
                        List.of(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        LevelScript.Secondary.NONE,
                        List.of()));
        assertEquals(LevelScript.Section.TIMED_HALT_RAMP_SECONDS, new LevelScript.Section(10, 0).rampSeconds());
    }

    @Test
    void aLevelWithoutATimedHaltScrollsAtItsSectionsSpeeds() {
        List<LevelScript.Section> sections =
                List.of(new LevelScript.Section(10, 120), new LevelScript.Section(20, 150));
        assertEquals(120, Sortie.timedHaltSpeed(sections, SimStep.ticks(9.99)));
        assertEquals(150, Sortie.timedHaltSpeed(sections, SimStep.ticks(10)));
        assertEquals(150, Sortie.timedHaltSpeed(sections, SimStep.ticks(25)), "the last section's speed past the end");
    }

    @Test
    void steppingThroughTheHaltAndItsEasesDoesNotAllocate() {
        LevelScript level = level(List.of());
        long allocated = Allocations.least(() -> PartGSpecs.sortie(level), sortie -> {
            for (int i = 0; i < SimStep.ticks(45); i++) {
                sortie.step(Command.NONE);
            }
        });
        assertEquals(0, allocated);
    }
}
