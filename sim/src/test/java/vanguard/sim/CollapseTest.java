package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.LevelClockSpecs.NODE;
import static vanguard.sim.LevelClockSpecs.groundUnit;
import static vanguard.sim.LevelClockSpecs.indexOf;
import static vanguard.sim.LevelClockSpecs.radioed;
import static vanguard.sim.PartCSpecs.count;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The collapse (design/campaign Level 09; M5 part C user decision D5 = a, round 31's look c): when
 * its group is cleared its tower leans for 1.5 s (the warning), then drops for 1.5 s; at the impact
 * (3.0 s) its blast rolls out from the tower's foot, a ring growing from 100 to 390 px in 1 s (eased
 * 1 − (1 − t)²), destroying the ground units in the band as it reaches them, each paid and scored as
 * an Airstrike kill; air units, units outside the band and the player are untouched. Its dust
 * settles 6 s after the impact: a hold over its group lasts until then (9.0 s after the warning
 * starts; user decision, 2026-10-07).
 */
class CollapseTest {
    /** A slow scroll, so the units stay on the screen through the warning, the drop and the blast. */
    private static final double SPEED = 40;

    private static final EnemySpec NEAR = groundUnit("hut", Layer.GROUND, 30, new Hitbox(30, 30), 12);
    private static final EnemySpec FAR = groundUnit("shed", Layer.GROUND, 30, new Hitbox(30, 30), 12);
    private static final EnemySpec BEYOND = groundUnit("kiosk", Layer.GROUND, 30, new Hitbox(30, 30), 12);
    private static final EnemySpec OUTSIDE = groundUnit("shack", Layer.GROUND, 30, new Hitbox(30, 30), 12);
    private static final EnemySpec FLYER = groundUnit("drone", Layer.AIR, 30, new Hitbox(30, 30), 12);
    private static final int WARNING = SimStep.ticks(1.5);
    private static final int DROP = SimStep.ticks(1.5);
    private static final int BLAST = SimStep.ticks(1);
    /** The dust settles this long after the impact (Level 09's heavy dust's ramp out). */
    private static final int SETTLE = SimStep.ticks(6);
    /** Level 09's hold C's speed. */
    private static final double HOLD_SPEED = 30;

    /**
     * The collapse's band on the ground (ground positions, px from the bottom edge at the level start):
     * at t = 7 (280 px of scroll) 200–400 px below the top edge.
     */
    private static final double BAND_BOTTOM = 420;

    private static final double BAND_TOP = 620;
    /** The tower's foot: its centre 40 px from the left edge, half way up the band. */
    private static final double FOOT_X = 40;

    private static final double FOOT = (BAND_BOTTOM + BAND_TOP) / 2;

    private static LevelScript.Collapse collapse(double bottom, double top) {
        return new LevelScript.Collapse(List.of(0), 1.5, 1.5, 1, 100, 390, 6, FOOT_X, bottom, top);
    }

    /**
     * The node (its group the collapse's) and units entering together at t = 1 at ground position
     * 595 (75 px above the foot's centre): a ground unit 20 px right of the foot (inside the blast's
     * first radius), one at 400 (368 px away, reached 0.7 s into the blast), one at 470 (430 px away,
     * never reached) and an air unit in the middle; and a ground unit at x 60 entering at t = 4, 120
     * px further up the ground: outside the band, though within the blast's reach. 240 px of scroll
     * later (t = 7) the first ones lie 225 px below the top edge, inside the band.
     */
    private static LevelScript level(List<LevelScript.RadioCue> radio) {
        return LevelClockSpecs.level(
                40,
                SPEED,
                List.of(),
                List.of(
                        new LevelScript.GroundUnit(1, 240, NODE, 0),
                        new LevelScript.GroundUnit(1, 60, NEAR, -1),
                        new LevelScript.GroundUnit(1, 300, FLYER, -1),
                        new LevelScript.GroundUnit(1, 400, FAR, -1),
                        new LevelScript.GroundUnit(1, 470, BEYOND, -1),
                        new LevelScript.GroundUnit(4, 60, OUTSIDE, -1)),
                List.of("Node C1"),
                new LevelScript.Secondary(0.8, 50),
                radio,
                List.of(),
                Optional.of(collapse(BAND_BOTTOM, BAND_TOP)));
    }

    /** A sortie that flies to t = 7 and destroys the node: the collapse starts at the next step. */
    private static Sortie atTheCollapse(List<LevelScript.RadioCue> radio) {
        return atTheCollapse(radio, 7);
    }

    /** A sortie that flies to {@code t} and destroys the node: the collapse starts at the next step. */
    private static Sortie atTheCollapse(List<LevelScript.RadioCue> radio, double t) {
        Sortie sortie = new Sortie(
                1, TestSpecs.LOADOUT, level(radio), TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        while (sortie.levelSeconds() < t) {
            sortie.step(Command.NONE);
        }
        sortie.destroyEnemy(indexOf(sortie, "node"));
        return sortie;
    }

    private static double distance(Sortie sortie, double x, double y) {
        return Math.hypot(x - sortie.collapseFootX(), y - sortie.collapseFootY());
    }

    @Test
    void itLeansDropsAndItsBlastKillsOutwardFromTheFoot() {
        Sortie sortie = atTheCollapse(List.of(LevelClockSpecs.cue(LevelScript.CueTrigger.COLLAPSE, 0, 0)));
        assertFalse(sortie.collapseStarted());
        sortie.step(Command.NONE);

        assertEquals(1, count(sortie, SimEvents.Type.COLLAPSE_WARNING));
        assertTrue(radioed(sortie, 0), "the radio's collapse cue");
        assertTrue(sortie.collapseStarted());
        assertTrue(sortie.collapseWarning());
        assertEquals(BAND_TOP - sortie.groundScroll(), sortie.collapseBandTop(), 1e-9);
        assertEquals(BAND_BOTTOM - sortie.groundScroll(), sortie.collapseBandBottom(), 1e-9);
        assertEquals(PlayField.HEIGHT - 200, sortie.collapseBandTop(), 3, "about 200 px below the top edge");
        assertEquals(FOOT_X, sortie.collapseFootX());
        assertEquals(FOOT - sortie.groundScroll(), sortie.collapseFootY(), 1e-9);
        List<String> killed = new ArrayList<>();
        int fall = -1;
        int impact = -1;
        int end = -1;
        double before = 0;
        for (int step = 1; step <= WARNING + DROP + BLAST + 10; step++) {
            sortie.step(Command.NONE);
            if (count(sortie, SimEvents.Type.COLLAPSE_FALL) > 0) {
                fall = step;
            }
            if (count(sortie, SimEvents.Type.COLLAPSE_IMPACT) > 0) {
                impact = step;
                assertEquals(100, sortie.collapseBlastRadius(), 1e-9, "the blast starts at the foot's edge");
            }
            if (count(sortie, SimEvents.Type.COLLAPSE_END) > 0) {
                end = step;
                assertEquals(390, sortie.collapseBlastRadius(), 1e-9, "the band's far corner");
            }
            if (step < WARNING + DROP) {
                assertFalse(sortie.collapseImpacted());
                assertEquals(0, sortie.collapseBlastRadius(), "no kills before the impact");
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.ENEMY_DESTROYED) {
                    double d = distance(sortie, events.x(i), events.y(i));
                    killed.add(events.x(i) < 200 ? "near" : "far");
                    assertTrue(d <= sortie.collapseBlastRadius(), "inside the ring: " + d);
                    assertTrue(d > before || before == 0, "reached by the ring in this step: " + d);
                }
            }
            before = sortie.collapseBlastRadius();
        }

        assertEquals(WARNING, fall, "the drop after the 1.5 s lean");
        assertEquals(WARNING + DROP, impact, "the impact 3 s after the warning starts");
        assertEquals(WARNING + DROP + BLAST, end, "the blast takes 1 s");
        assertEquals(List.of("near", "far"), killed, "outward from the foot");
        assertEquals(390, sortie.collapseBlastRadius(), 1e-9, "it keeps its reach once over");
        assertEquals(
                BAND_TOP - sortie.groundScroll(), sortie.collapseBandTop(), 1e-9, "the band scrolls with the ground");
        assertTrue(indexOf(sortie, "kiosk") >= 0, "beyond the ring's reach");
        assertTrue(indexOf(sortie, "shack") >= 0, "outside the band");
        assertTrue(indexOf(sortie, "drone") >= 0, "the air unit is untouched");
        assertEquals(TestSpecs.FULL_ARMOUR, sortie.ship().defences().armour(), "the player is untouched");
    }

    /** The blast's front: eased out, three quarters of the way at half its time. */
    @Test
    void theBlastsRadiusEasesOut() {
        LevelScript.Collapse collapse = collapse(BAND_BOTTOM, BAND_TOP);
        assertEquals(100, collapse.blastRadius(0), 1e-9);
        assertEquals(100 + 290 * 0.75, collapse.blastRadius(0.5), 1e-9);
        assertEquals(390, collapse.blastRadius(1), 1e-9);
        assertEquals(390, collapse.blastRadius(5), 1e-9);
    }

    /** Its real-time count runs on after its end, so the drawn dust can ramp out over its seconds; nothing else moves. */
    @Test
    void itsSecondsRunOnAfterItsEnd() {
        Sortie sortie = atTheCollapse(List.of());
        sortie.step(Command.NONE);
        LevelClockSpecs.until(sortie, SimEvents.Type.COLLAPSE_END, WARNING + DROP + BLAST + 1, Command.NONE);
        assertEquals((WARNING + DROP + BLAST) * SimStep.SECONDS, sortie.collapseSeconds(), 1e-9);

        int after = SimStep.ticks(6);
        for (int step = 0; step < after; step++) {
            sortie.step(Command.NONE);
            assertEquals(0, count(sortie, SimEvents.Type.COLLAPSE_END), "it ends once");
            assertEquals(0, count(sortie, SimEvents.Type.COLLAPSE_FALL));
            assertEquals(0, count(sortie, SimEvents.Type.COLLAPSE_IMPACT));
            assertTrue(sortie.collapseImpacted());
            assertFalse(sortie.collapseWarning());
        }
        assertEquals(
                (WARNING + DROP + BLAST + after) * SimStep.SECONDS,
                sortie.collapseSeconds(),
                1e-9,
                "6 s after the end");
        assertTrue(sortie.collapseStarted());
    }

    /**
     * The band lies on the ground (the falling tower's), not on the screen: a group cleared early or
     * late starts it over the same ground, at another height on the screen.
     */
    @Test
    void theBandCoversTheSameGroundWhetherItsGroupDiesEarlyOrLate() {
        Sortie early = atTheCollapse(List.of(), 3);
        Sortie late = atTheCollapse(List.of(), 9);
        early.step(Command.NONE);
        late.step(Command.NONE);
        assertTrue(early.collapseStarted() && late.collapseStarted());

        for (Sortie sortie : List.of(early, late)) {
            assertEquals(BAND_TOP, sortie.collapseBandTop() + sortie.groundScroll(), 1e-9);
            assertEquals(BAND_BOTTOM, sortie.collapseBandBottom() + sortie.groundScroll(), 1e-9);
        }
        assertEquals(
                late.groundScroll() - early.groundScroll(),
                early.collapseBandTop() - late.collapseBandTop(),
                1e-9,
                "lower on the screen the later it starts");
    }

    @Test
    void itsKillsPayAndScoreAsTheGunsDo() {
        Sortie blasted = atTheCollapse(List.of());
        Sortie shot = atTheCollapse(List.of());
        long score = blasted.score();
        int credits = blasted.credits();
        for (int step = 0; step < WARNING + DROP + BLAST + 10; step++) {
            blasted.step(Command.NONE);
        }
        shot.destroyEnemy(indexOf(shot, "hut"));
        shot.destroyEnemy(indexOf(shot, "shed"));

        assertEquals(shot.kills(), blasted.kills(), "two kills");
        assertEquals(shot.credits(), blasted.credits(), "their bounties");
        assertTrue(blasted.credits() > credits);
        assertTrue(blasted.score() > score);
        assertEquals(-1, indexOf(blasted, "hut"));
        assertEquals(-1, indexOf(blasted, "shed"));
    }

    @Test
    void aLostGroupStartsNoCollapse() {
        Sortie sortie = new Sortie(
                1, TestSpecs.LOADOUT, level(List.of()), TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        while (sortie.levelSeconds() < 30) {
            sortie.step(Command.NONE);
            assertFalse(sortie.collapseStarted(), "the node got away");
        }
    }

    /**
     * Level 09's hold C: the node's hold over the collapse's group, the scroll easing from 150 to
     * 30 px/s; the node is destroyed 2 s into the hold (the ease done), the collapse starting at the
     * next step.
     */
    private static Sortie inTheHold() {
        LevelScript level = LevelClockSpecs.level(
                40,
                150,
                List.of(),
                List.of(new LevelScript.GroundUnit(1, 240, NODE, 0)),
                List.of("Node C1"),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(new LevelScript.Hold(List.of(0), 200, HOLD_SPEED, 1)),
                // The band: about 80–260 px below the top edge when the node is destroyed, so it stays on
                // the screen through the 270 px the hold scrolls until the dust has settled.
                Optional.of(collapse(780, 960)));
        Sortie sortie =
                new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        LevelClockSpecs.until(sortie, SimEvents.Type.HOLD_START, SimStep.ticks(10), Command.NONE);
        for (int step = 0; step < SimStep.ticks(2); step++) {
            sortie.step(Command.NONE);
        }
        sortie.destroyEnemy(indexOf(sortie, "node"));
        return sortie;
    }

    /**
     * Hold C lasts until the collapse's dust has settled (user decision, 2026-10-07): the scroll stays
     * at the hold's 30 px/s through the lean, the drop, the blast and the dust's 6 s, 9.0 s after the
     * warning starts, so the heap stays in sight; then it eases back to the section's speed.
     */
    @Test
    void aHoldOverItsGroupsLastsUntilTheDustSettlesThenEasesBack() {
        Sortie sortie = inTheHold();
        sortie.step(Command.NONE);
        assertEquals(1, count(sortie, SimEvents.Type.COLLAPSE_WARNING));
        assertEquals(0, count(sortie, SimEvents.Type.HOLD_END), "the hold outlasts its node");
        assertTrue(sortie.holdActive());

        double scroll = sortie.groundScroll();
        int end = -1;
        int holdEnd = -1;
        for (int step = 1; step <= WARNING + DROP + SETTLE; step++) {
            sortie.step(Command.NONE);
            if (count(sortie, SimEvents.Type.COLLAPSE_END) > 0) {
                end = step;
            }
            if (count(sortie, SimEvents.Type.HOLD_END) > 0) {
                holdEnd = step;
            }
            if (step < WARNING + DROP + SETTLE) {
                assertTrue(
                        sortie.holdActive(),
                        "the hold runs through the lean, the drop, the blast and the dust at " + step);
                assertEquals(0, count(sortie, SimEvents.Type.HOLD_END));
            }
            assertEquals(HOLD_SPEED, sortie.groundSpeed(), 1e-9, "at the hold's speed at " + step);
            assertTrue(sortie.collapseBandBottom() >= 0, "the band's bottom on the screen at " + step);
            assertTrue(sortie.collapseBandTop() <= PlayField.HEIGHT, "the band's top on the screen at " + step);
        }
        assertEquals(WARNING + DROP + BLAST, end, "the blast ends");
        assertEquals(WARNING + DROP + SETTLE, holdEnd, "the hold ends once the dust has settled");
        assertEquals(9.0, holdEnd * SimStep.SECONDS, 1e-9, "9.0 s after the warning starts");
        assertFalse(sortie.holdActive());
        assertEquals(
                HOLD_SPEED * (WARNING + DROP + SETTLE) * SimStep.SECONDS,
                sortie.groundScroll() - scroll,
                1e-6,
                "the band moved only at the hold's speed");

        List<Double> speeds = new ArrayList<>();
        for (int step = 0; step < SimStep.ticks(1); step++) {
            sortie.step(Command.NONE);
            speeds.add(sortie.groundSpeed());
        }
        for (int i = 1; i < speeds.size(); i++) {
            assertTrue(speeds.get(i) > speeds.get(i - 1), "easing back at " + i);
        }
        assertEquals(150, speeds.getLast(), 1e-9);
        assertEquals(0, count(sortie, SimEvents.Type.HOLD_START), "it does not start again");
    }

    @Test
    void aHoldThroughTheCollapseStepsDeterministicallyWithoutAllocating() {
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = inTheHold();
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < WARNING + DROP + SETTLE + SimStep.ticks(2); i++) {
                        sortie.step(Command.NONE);
                    }
                });

        assertEquals(0, allocated, "allocated " + allocated + " bytes");
        assertEquals(flown.getFirst().stateHash(), flown.getLast().stateHash());
    }

    @Test
    void aCollapseStepsDeterministicallyWithoutAllocating() {
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = atTheCollapse(List.of());
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < WARNING + DROP + BLAST + 10; i++) {
                        sortie.step(Command.NONE);
                    }
                });

        assertEquals(0, allocated, "allocated " + allocated + " bytes");
        assertEquals(flown.getFirst().stateHash(), flown.getLast().stateHash());
    }
}
