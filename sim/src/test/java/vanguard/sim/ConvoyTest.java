package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.FULL_ARMOUR;
import static vanguard.sim.TestSpecs.LOADOUT;
import static vanguard.sim.TestSpecs.RULES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The escort objective's convoy (design/allies, civilian crawler; design/campaign, Level 04): five
 * units roll in from the bottom edge to their stations and follow the road; only shots the
 * target-the-objective hook aims at them and walkers' claws hurt them; losing the last fails the
 * primary objective as a wreck does, and the units home pay at the end.
 */
class ConvoyTest {
    /** The Level 04 column, px below the top edge. */
    private static final List<Double> COLUMN = List.of(150.0, 234.0, 318.0, 402.0, 486.0);

    private static final double SPEED = 120;
    /** A road that runs straight up at x = 240, bends right to 300 and back to 200. */
    private static final Road ROAD =
            new Road(56, new double[] {-1000, 600, 1200, 2000, 9000}, new double[] {240, 240, 300, 200, 200});
    /** Straight up the middle of the screen, for the claws. */
    private static final Road STRAIGHT = new Road(56, new double[] {-1000, 9000}, new double[] {240, 240});

    private static final EnemySpec WALKER = new EnemySpec(
            "scuttler",
            28,
            new Hitbox(46, 40),
            Layer.GROUND,
            10,
            false,
            25,
            120,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false);

    private static AllySpec crawler(double hp) {
        return new AllySpec("civilian-crawler", new Hitbox(40, 72), new Hitbox(32, 64), hp, true, 10, 0.5, 30);
    }

    private static LevelScript.Escort escort(double hp, List<String> hooked) {
        return new LevelScript.Escort(
                crawler(hp), COLUMN.stream().map(y -> PlayField.HEIGHT - y).toList(), 4, 1, 84, 30, hooked);
    }

    private static LevelScript level(
            double seconds,
            LevelScript.Escort escort,
            Road road,
            List<WaveSpec> waves,
            List<LevelScript.GroundUnit> units,
            List<LevelScript.RadioCue> radio) {
        return new LevelScript(
                4,
                1,
                0,
                List.of(new LevelScript.Section(seconds, SPEED)),
                waves,
                List.of(),
                units,
                0,
                radio,
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(),
                List.of(),
                Optional.of(escort),
                Optional.of(road));
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, LOADOUT, level, RULES, FULL_ARMOUR);
    }

    private static void run(Sortie sortie, double seconds) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(Command.NONE);
        }
    }

    @Test
    void theRoadRunsStraightBetweenItsPointsAndStraightUpBeyondThem() {
        assertEquals(240, ROAD.x(600), 1e-9);
        assertEquals(270, ROAD.x(900), 1e-9);
        assertEquals(300, ROAD.x(1200), 1e-9);
        assertEquals(240, ROAD.x(-5000), 1e-9);
        assertEquals(200, ROAD.x(20_000), 1e-9);
        assertEquals(0, ROAD.slope(0), 1e-9);
        assertEquals(0.1, ROAD.slope(900), 1e-9);
        assertEquals(Math.toDegrees(Math.atan(0.1)), ROAD.headingDegrees(700), 1e-9, "bending right going up");
        assertTrue(ROAD.headingDegrees(1600) < 0, "bending left going up");
        assertEquals(0, ROAD.headingDegrees(-2000), 1e-9);
        // The arc length anchors the texture: straight up before the first point, longer on a bend.
        assertEquals(-200, ROAD.arc(-1200), 1e-9);
        assertEquals(1600, ROAD.arc(600), 1e-9);
        assertEquals(1600 + Math.sqrt(600 * 600 + 60 * 60), ROAD.arc(1200), 1e-9);
    }

    @Test
    void theConvoyRollsInOneBySecondAndHoldsItsStationsOnTheRoad() {
        Sortie sortie = sortie(level(60, escort(60, List.of()), ROAD, List.of(), List.of(), List.of()));
        assertEquals(5, sortie.allyCount());

        run(sortie, 3.9);
        for (int k = 0; k < 5; k++) {
            assertEquals(Ally.State.WAITING, sortie.ally(k).state());
        }
        run(sortie, 1.2);
        assertEquals(Ally.State.ROLLING, sortie.ally(0).state());
        assertEquals(Ally.State.ROLLING, sortie.ally(1).state());
        assertEquals(Ally.State.WAITING, sortie.ally(2).state());

        run(sortie, 6);
        double before = sortie.groundScroll();
        for (int k = 0; k < 5; k++) {
            Ally ally = sortie.ally(k);
            assertEquals(Ally.State.STATION, ally.state(), "unit " + k);
            assertEquals(PlayField.HEIGHT - COLUMN.get(k), ally.y(), 1e-9);
            double along = sortie.groundScroll() + ally.y() - PlayField.HEIGHT / 2.0;
            assertEquals(ROAD.x(along), ally.x(), 1e-9, "on the road");
            assertEquals(ROAD.headingDegrees(along), ally.headingDegrees(), 1e-9, "along the road");
        }
        run(sortie, 3);
        assertTrue(sortie.groundScroll() > before + 300);
        assertEquals(PlayField.HEIGHT - COLUMN.get(4), sortie.ally(4).y(), 1e-9, "it holds its height");
        assertEquals(5, sortie.alliesAlive());
    }

    @Test
    void onlyShotsTheHookAimsAtTheConvoyHurtIt() {
        var units = List.of(new LevelScript.GroundUnit(6, 150, GroundAndDiveTest.TURRET, -1));
        Sortie hooked = sortie(level(60, escort(60, List.of("spine-turret")), ROAD, List.of(), units, List.of()));
        Sortie ignored = sortie(level(60, escort(60, List.of()), ROAD, List.of(), units, List.of()));

        run(hooked, 14);
        run(ignored, 14);

        assertTrue(lowest(hooked) < 1, "the turret's crawler-aimed shots hit");
        assertEquals(1, lowest(ignored), 1e-9, "shots aimed at the ship never hurt a crawler");
    }

    private static double lowest(Sortie sortie) {
        double lowest = 1;
        for (int k = 0; k < sortie.allyCount(); k++) {
            lowest = Math.min(lowest, sortie.ally(k).hpShare());
        }
        return lowest;
    }

    @Test
    void aWalkerClawsEveryUnitItPassesOver() {
        var walker = TestSpecs.wave(12, WaveSpec.Formation.SINGLE, WALKER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Sortie sortie = sortie(level(60, escort(60, List.of()), STRAIGHT, List.of(walker), List.of(), List.of()));

        run(sortie, 20);

        // 40 + 64 px of overlap at 120 px/s: 52 steps at 10 per second.
        for (int k = 0; k < 5; k++) {
            assertEquals(60 - 52 * 10 * SimStep.SECONDS, 60 * sortie.ally(k).hpShare(), 0.2, "unit " + k);
        }
    }

    @Test
    void losingTheLastUnitFailsThePrimaryObjectiveAndTheShipCannotBeHurt() {
        var walker = TestSpecs.wave(12, WaveSpec.Formation.SINGLE, WALKER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        var needlers = TestSpecs.wave(
                16, WaveSpec.Formation.SINGLE, TestSpecs.NEEDLER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        List<LevelScript.RadioCue> radio = List.of(
                cue(LevelScript.CueTrigger.FIRST_ALLY_HIT, "Crawler {ally} is hit!"),
                cue(LevelScript.CueTrigger.FIRST_ALLY_LOST, "We lost {ally}."),
                cue(LevelScript.CueTrigger.MISSION_FAILED, "The convoy is gone, Lancer. Pull back."),
                cue(LevelScript.CueTrigger.LEVEL_END, "Home."));
        Sortie sortie = sortie(level(25, escort(5, List.of()), STRAIGHT, List.of(walker, needlers), List.of(), radio));
        List<LevelScript.CueTrigger> cues = new ArrayList<>();
        int failures = 0;
        double armourAtFailure = -1;
        for (int i = 0; i < SimStep.ticks(30); i++) {
            sortie.step(Command.NONE);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.RADIO) {
                    cues.add(radio.get(events.value(e)).trigger());
                } else if (events.type(e) == SimEvents.Type.PRIMARY_FAILED) {
                    failures++;
                    armourAtFailure = sortie.ship().defences().armour();
                }
            }
        }

        assertTrue(sortie.primaryFailed());
        assertEquals(0, sortie.alliesAlive());
        assertEquals(1, failures);
        assertEquals(
                List.of(
                        LevelScript.CueTrigger.FIRST_ALLY_HIT,
                        LevelScript.CueTrigger.FIRST_ALLY_LOST,
                        LevelScript.CueTrigger.MISSION_FAILED),
                cues);
        assertEquals(0, sortie.firstAllyHit());
        assertEquals(0, sortie.firstAllyLost());
        assertTrue(sortie.flying(), "no wreck: the ship flies on");
        assertEquals(armourAtFailure, sortie.ship().defences().armour(), "nothing hurts it any more");
        assertFalse(sortie.complete(), "a failed level never completes");
        assertEquals(Ally.State.WRECK, sortie.ally(0).state());

        sortie.retry(40);
        sortie.step(Command.NONE);

        assertFalse(sortie.primaryFailed());
        assertEquals(5, sortie.alliesAlive());
        assertEquals(-1, sortie.firstAllyHit());
        assertEquals(Ally.State.WAITING, sortie.ally(0).state());
        assertEquals(0, sortie.credits());
    }

    private static LevelScript.RadioCue cue(LevelScript.CueTrigger trigger, String line) {
        return new LevelScript.RadioCue(trigger, 0, "", "Okafor", line, false, "neutral");
    }

    private static LevelScript.RadioCue end(int min, int max, String line) {
        return new LevelScript.RadioCue(
                LevelScript.CueTrigger.LEVEL_END, 0, "", "Okafor", line, false, "neutral", "Okafor", false, min, max);
    }

    @Test
    void eachUnitHomePaysAndTheLevelEndLineFitsTheOutcome() {
        var walker = TestSpecs.wave(12, WaveSpec.Formation.SINGLE, WALKER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        List<LevelScript.RadioCue> radio = List.of(end(5, 5, "All five."), end(1, 4, "Most."));
        Sortie all = sortie(level(20, escort(60, List.of()), ROAD, List.of(), List.of(), radio));
        // 8 HP: the walker destroys every unit it passes.
        Sortie none = sortie(level(20, escort(8, List.of()), STRAIGHT, List.of(walker), List.of(), radio));

        List<Integer> allLines = endLines(all);

        assertTrue(all.complete());
        assertEquals(List.of(0), allLines, "the all-home line only");
        LevelResult result = all.result();
        assertEquals(new LevelResult.Escort("civilian-crawler", 5, 5, 150), result.escort());
        assertEquals(150, result.credits().objectives(), "paid as an objective");

        endLines(none);
        assertTrue(none.primaryFailed());
        assertFalse(none.complete());
    }

    private static List<Integer> endLines(Sortie sortie) {
        List<Integer> lines = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(22); i++) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.RADIO) {
                    lines.add(sortie.events().value(e));
                }
            }
        }
        return lines;
    }

    @Test
    void someUnitsHomeTakeTheOtherLine() {
        // A slow walker down the middle meets the trailing units where the road has come back to x = 240,
        // the leading ones where it still runs at x = 340.
        EnemySpec slow = new EnemySpec(
                "scuttler",
                28,
                new Hitbox(46, 40),
                Layer.GROUND,
                10,
                false,
                25,
                60,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false);
        var walker = TestSpecs.wave(12, WaveSpec.Formation.SINGLE, slow, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Road split = new Road(56, new double[] {-1000, 2000, 2150, 9000}, new double[] {340, 340, 240, 240});
        List<LevelScript.RadioCue> radio = List.of(end(5, 5, "All five."), end(1, 4, "Most."));
        Sortie sortie = sortie(level(21, escort(8, List.of()), split, List.of(walker), List.of(), radio));

        List<Integer> lines = endLines(sortie);

        int home = sortie.alliesAlive();
        assertTrue(home > 0 && home < 5, "some lost, some home: " + home);
        assertEquals(List.of(1), lines.subList(lines.size() - 1, lines.size()));
        assertEquals(30 * home, sortie.result().escort().credits());
    }

    @Test
    void aCueThatRequiresASpecialStaysSilentWithoutOne() {
        var hammer = new LevelScript.RadioCue(
                LevelScript.CueTrigger.TIME,
                1,
                "",
                "Okafor",
                "Hammer flight.",
                false,
                "neutral",
                "Okafor",
                true,
                0,
                Integer.MAX_VALUE);
        var plain = cue(LevelScript.CueTrigger.TIME, "Five crawlers.");
        Sortie sortie = sortie(level(10, escort(60, List.of()), ROAD, List.of(), List.of(), List.of(hammer, plain)));

        List<Integer> lines = endLines(sortie);

        assertEquals(List.of(1), lines);
    }

    @Test
    void theSameRunGivesTheSameStateHash() {
        var units = List.of(new LevelScript.GroundUnit(6, 150, GroundAndDiveTest.TURRET, -1));
        var walker = TestSpecs.wave(12, WaveSpec.Formation.SINGLE, WALKER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        LevelScript script = level(30, escort(60, List.of("spine-turret")), ROAD, List.of(walker), units, List.of());
        Sortie first = sortie(script);
        Sortie second = sortie(script);
        Sortie without = sortie(level(30, escort(60, List.of()), ROAD, List.of(walker), units, List.of()));

        for (int i = 0; i < SimStep.ticks(25); i++) {
            first.step(SortieTest.Pilot.commands(i));
            second.step(SortieTest.Pilot.commands(i));
        }
        Sortie parked = sortie(script);
        for (int i = 0; i < SimStep.ticks(25); i++) {
            parked.step(Command.NONE);
            without.step(Command.NONE);
        }

        assertEquals(first.stateHash(), second.stateHash());
        assertNotEquals(parked.stateHash(), without.stateHash(), "the hook changes where the shots go");
    }

    @Test
    void theConvoyDoesNotAllocate() {
        var units = List.of(
                new LevelScript.GroundUnit(6, 150, GroundAndDiveTest.TURRET, -1),
                new LevelScript.GroundUnit(9, 330, GroundAndDiveTest.TURRET, -1));
        var walker = TestSpecs.wave(12, WaveSpec.Formation.SINGLE, WALKER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        LevelScript script = level(30, escort(60, List.of("spine-turret")), ROAD, List.of(walker), units, List.of());
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(script);
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(20); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertTrue(lowest(flown.getLast()) < 1, "the convoy was hit");
        assertEquals(0, allocated, "20 s allocated " + allocated + " bytes");
    }
}
