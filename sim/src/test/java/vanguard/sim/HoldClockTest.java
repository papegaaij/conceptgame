package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.LevelClockSpecs.NODE;
import static vanguard.sim.LevelClockSpecs.indexOf;
import static vanguard.sim.LevelClockSpecs.until;
import static vanguard.sim.PartCSpecs.count;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The level clock in a hold zone (design/campaign Level 09; M5 part C user decisions D2 and D3 =
 * a): when the first unit of its groups reaches its depth the scroll eases over 1 s to the hold's
 * speed, the level clock (script time) slows with it at the current speed ÷ the section's, waves
 * wait for it, the real clock runs on, and once its groups are gone the scroll eases back.
 */
class HoldClockTest {
    private static final double SPEED = 150;
    private static final double HOLD_SPEED = 30;
    /** The node enters at t = 2 (its centre 28 px above the top edge) and starts the hold 200 px below it. */
    private static final double DEPTH = 200;

    private static final int RAMP = SimStep.ticks(1);

    private static LevelScript level() {
        return level(Optional.empty());
    }

    private static LevelScript level(Optional<LevelScript.Collapse> collapse) {
        return LevelClockSpecs.level(
                40,
                SPEED,
                List.of(TestSpecs.wave(
                        6, WaveSpec.Formation.SINGLE, TestSpecs.SKITTER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE)),
                List.of(new LevelScript.GroundUnit(2, 240, NODE, 0)),
                List.of("Node A"),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(new LevelScript.Hold(List.of(0), DEPTH, HOLD_SPEED, 1)),
                collapse);
    }

    private static Sortie sortie() {
        return new Sortie(1, TestSpecs.LOADOUT, level(), TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    @Test
    void outsideAHoldTheClockRunsOneWholeStepPerStep() {
        Sortie sortie = sortie();
        while (count(sortie, SimEvents.Type.HOLD_START) == 0) {
            sortie.step(Command.NONE);
            if (count(sortie, SimEvents.Type.HOLD_START) == 0) {
                assertEquals(sortie.tick() * SimStep.SECONDS, sortie.levelSeconds(), "script time is whole steps");
                assertEquals(sortie.realSeconds(), sortie.levelSeconds());
                assertEquals(1, sortie.scriptRate());
                assertEquals(SPEED, sortie.groundSpeed());
            }
        }
    }

    @Test
    void aHoldStartsWhenTheFirstUnitOfItsGroupsReachesItsDepth() {
        Sortie sortie = sortie();
        until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);

        Enemy node = sortie.enemy(indexOf(sortie, "node"));
        assertTrue(node.y() <= PlayField.HEIGHT - DEPTH, "at its depth: " + node.y());
        assertTrue(node.y() > PlayField.HEIGHT - DEPTH - SPEED * SimStep.SECONDS, "the step it got there");
        assertTrue(sortie.holdActive());
        assertEquals(0, sortie.activeHold());
        assertEquals(SPEED, sortie.groundSpeed(), "it eases from the next step");
    }

    @Test
    void theScrollEasesOverItsRampAndTheLevelClockSlowsWithIt() {
        Sortie sortie = sortie();
        until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        List<Double> speeds = new ArrayList<>();
        for (int step = 0; step < RAMP; step++) {
            sortie.step(Command.NONE);
            speeds.add(sortie.groundSpeed());
            assertEquals(sortie.groundSpeed() / SPEED, sortie.scriptRate(), 1.0 / Sortie.CLOCK_ONE);
        }

        for (int i = 1; i < speeds.size(); i++) {
            assertTrue(speeds.get(i) < speeds.get(i - 1), "easing down at " + i);
        }
        assertEquals((SPEED + HOLD_SPEED) / 2, speeds.get(RAMP / 2 - 1), 0.5, "halfway through the ease");
        assertEquals(HOLD_SPEED, speeds.getLast(), 1e-9);
        assertEquals(1, sortie.holdEase());
        assertEquals(HOLD_SPEED / SPEED, sortie.scriptRate(), 1.0 / Sortie.CLOCK_ONE);
    }

    @Test
    void inAHoldScriptTimeRunsAtTheScrollsShareOfRealTime() {
        Sortie sortie = sortie();
        until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        for (int step = 0; step < RAMP; step++) {
            sortie.step(Command.NONE);
        }
        double script = sortie.levelSeconds();
        double real = sortie.realSeconds();
        double scroll = sortie.groundScroll();
        for (int step = 0; step < SimStep.ticks(5); step++) {
            sortie.step(Command.NONE);
        }

        assertEquals(5, sortie.realSeconds() - real, 1e-9, "the real clock");
        assertEquals(1, sortie.levelSeconds() - script, 1e-3, "0.2 of it in script time");
        assertEquals(HOLD_SPEED * 5, sortie.groundScroll() - scroll, 1e-6, "the ground at 30 px/s");
    }

    @Test
    void wavesWaitForTheLevelClock() {
        Sortie sortie = sortie();
        int steps = until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        while (indexOf(sortie, "skitter") < 0) {
            sortie.step(Command.NONE);
            steps++;
            assertTrue(steps < SimStep.ticks(40), "the wave came");
        }

        assertEquals(6, sortie.levelSeconds(), SimStep.SECONDS, "at its script time");
        assertTrue(sortie.realSeconds() > 12, "well after 6 s of real time: " + sortie.realSeconds());
    }

    @Test
    void theHoldEndsWhenItsGroupsAreGoneAndTheScrollEasesBack() {
        Sortie sortie = sortie();
        until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        for (int step = 0; step < SimStep.ticks(3); step++) {
            sortie.step(Command.NONE);
        }
        sortie.destroyEnemy(indexOf(sortie, "node"));
        sortie.step(Command.NONE);

        assertEquals(1, count(sortie, SimEvents.Type.HOLD_END));
        assertFalse(sortie.holdActive());
        List<Double> speeds = new ArrayList<>();
        for (int step = 0; step < RAMP; step++) {
            sortie.step(Command.NONE);
            speeds.add(sortie.groundSpeed());
        }
        for (int i = 1; i < speeds.size(); i++) {
            assertTrue(speeds.get(i) > speeds.get(i - 1), "easing back at " + i);
        }
        assertEquals(SPEED, speeds.getLast(), 1e-9);
        assertEquals(0, sortie.holdEase());
        for (int step = 0; step < 10; step++) {
            double before = sortie.levelSeconds();
            sortie.step(Command.NONE);
            assertEquals(1, sortie.scriptRate(), "whole steps again");
            assertEquals(SimStep.SECONDS, sortie.levelSeconds() - before, 1e-12);
        }
        assertEquals(0, count(sortie, SimEvents.Type.HOLD_START), "it does not start again");
    }

    @Test
    void aHoldWhoseGroupsAreGoneBeforeItsDepthNeverStarts() {
        Sortie sortie = sortie();
        while (indexOf(sortie, "node") < 0) {
            sortie.step(Command.NONE);
        }
        sortie.destroyEnemy(indexOf(sortie, "node"));
        for (int step = 0; step < SimStep.ticks(5); step++) {
            sortie.step(Command.NONE);
            assertEquals(0, count(sortie, SimEvents.Type.HOLD_START));
            assertEquals(1, sortie.scriptRate());
        }
    }

    @Test
    void aTargetThatGetsAwayFailsThePrimaryAndEndsTheHold() {
        // User decision D1 = a: no timeout; the node leaving the screen alive fails the mission at once.
        Sortie sortie = new Sortie(1, TestSpecs.LOADOUT, level(), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        until(sortie, SimEvents.Type.PRIMARY_FAILED, SimStep.ticks(30), Command.NONE);

        assertTrue(sortie.primaryFailed());
        assertEquals(0, sortie.failedGroup());
        sortie.step(Command.NONE);
        assertFalse(sortie.holdActive(), "the hold is over");
    }

    @Test
    void aHoldOverACollapsesGroupsEndsAtOnceWhenATargetGetsAway() {
        // The hold waits for the collapse's dust only when the collapse starts (its groups cleared).
        LevelScript level =
                level(Optional.of(new LevelScript.Collapse(List.of(0), 1.5, 1.5, 1, 100, 390, 6, 240, 420, 620)));
        Sortie sortie = new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        until(sortie, SimEvents.Type.PRIMARY_FAILED, SimStep.ticks(30), Command.NONE);
        sortie.step(Command.NONE);

        assertFalse(sortie.holdActive(), "the hold is over");
        assertFalse(sortie.collapseStarted(), "no collapse for a node that got away");
    }

    @Test
    void aHiveNodeThatGetsAwayFailsTheMissionAtOnceWithItsLine() {
        // D1 = a for a ground enemy with a spawner (C1's Hive Node), as Level 05's batteries.
        LevelScript level = LevelClockSpecs.level(
                40,
                SPEED,
                List.of(),
                List.of(new LevelScript.GroundUnit(2, 240, PartCSpecs.hiveNode(2, 96), 0)),
                List.of("Node A1"),
                new LevelScript.Secondary(0.8, 50),
                List.of(LevelClockSpecs.cue(LevelScript.CueTrigger.MISSION_FAILED, 0, 0)),
                List.of(),
                Optional.empty());
        Sortie sortie = new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        until(sortie, SimEvents.Type.PRIMARY_FAILED, SimStep.ticks(20), Command.NONE);

        assertTrue(sortie.flying(), "the ship lived");
        assertEquals(0, sortie.failedGroup());
        assertEquals(1, count(sortie, SimEvents.Type.GROUP_LOST));
        assertTrue(LevelClockSpecs.radioed(sortie, 0), "the failed screen's line");
        assertFalse(sortie.complete());
    }

    @Test
    void aSortieThroughAHoldStepsDeterministicallyWithoutAllocating() {
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie();
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(8); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                    int node = indexOf(sortie, "node");
                    if (node >= 0) {
                        sortie.destroyEnemy(node);
                    }
                    for (int i = 0; i < SimStep.ticks(4); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "allocated " + allocated + " bytes");
        assertEquals(flown.getFirst().stateHash(), flown.getLast().stateHash());
        assertTrue(flown.getFirst().stateHash() != sortie().stateHash());
    }
}
