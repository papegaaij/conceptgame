package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part E, step E2b: the naval convoy outside the objectives (design/allies, convoy cargo ship and
 * escort frigate; design/campaign Level 11; user decision E8 = a and the stated defaults of
 * 2026-10-08) and the {@code afloat} secondary. The hulls hold screen-space stations from the start;
 * when the scroll halts in the boss's arena the cargo ships glide to their lanes and the frigate off
 * the bottom edge, on the real steps; a slam in a lane hits the ships in it (two hits sink one); the
 * afloat objective is met at the boss's death with every cargo ship afloat and failed at the first
 * sinking; after the boss they glide back; the boss checkpoint restores them; the convoy never fails
 * the mission.
 */
class NavalConvoyTest {
    /** The boss arrives at 1 s; the arena runs from 2 s to 6 s, where the clock halts while it lives. */
    private static final double ARRIVE = 1;

    private static final double ARENA_END = 6;
    private static final int HALVORSEN = 0;
    private static final int MBEKI = 1;
    private static final int SAINT_LAURENT = 2;
    private static final int RUYTER = 3;
    /** The radio cues' indexes in {@link #level}. */
    private static final int CUE_ALLY_HIT = 0;

    private static final int CUE_ALLY_LOST = 1;
    private static final int CUE_SECONDARY = 2;
    private static final int CUE_END_AFLOAT = 3;
    private static final int CUE_END_LOST = 4;
    private static final int CUE_FIRST_HIT = 5;

    static final AllySpec CARGO = new AllySpec(
            "cargo-ship", new Hitbox(56, 120), new Hitbox(48, 112), 2, false, 0, 0.75, 0, false, false, 0, 0, 0, 1, 0);
    static final AllySpec FRIGATE = new AllySpec(
            "escort-frigate",
            new Hitbox(40, 110),
            new Hitbox(32, 102),
            1,
            false,
            0,
            0.5,
            0,
            false,
            false,
            0,
            0,
            0,
            0,
            2);

    /** Level 11's convoy: three cargo ships in lanes 1, 2 and 4 and the frigate that leaves; 3 s glides. */
    static LevelScript.Naval convoy(AllySpec cargo) {
        return new LevelScript.Naval(
                List.of(
                        new LevelScript.NavalUnit(cargo, "Halvorsen", 130, PlayField.HEIGHT - 380, 1),
                        new LevelScript.NavalUnit(cargo, "Mbeki", 240, PlayField.HEIGHT - 350, 2),
                        new LevelScript.NavalUnit(cargo, "Saint-Laurent", 350, PlayField.HEIGHT - 380, 4),
                        new LevelScript.NavalUnit(FRIGATE, "Ruyter", 240, PlayField.HEIGHT - 480, 0)),
                3,
                PlayField.HEIGHT - 450,
                4,
                120);
    }

    /** A one-part boss over the ship that does not fire, settled before the halt. */
    static LevelScript.SetPieceSpec boss() {
        var core = new LevelScript.PartSpec("core", 0, 0, new Hitbox(80, 80), 40, true, 100, Optional.empty(), 0, 1);
        var phase =
                new BossSpec.Phase("Core", List.of(0), 0, List.of(), false, Optional.empty(), List.of(), Double.NaN);
        return new LevelScript.SetPieceSpec(
                "test-kraken",
                new Hitbox(200, 200),
                new Hitbox(120, 120),
                25,
                List.of(core),
                List.of(),
                Optional.empty(),
                Optional.of(new BossSpec(
                        ARRIVE,
                        Ship.START_X,
                        PlayField.HEIGHT - 110,
                        400,
                        0,
                        6,
                        Layer.AIR,
                        true,
                        "TEST KRAKEN",
                        20,
                        List.of(),
                        List.of(),
                        List.of(phase))));
    }

    private static LevelScript.RadioCue cue(LevelScript.CueTrigger trigger, String line, int min, int max) {
        return new LevelScript.RadioCue(
                trigger, 0, "", "Atlas Control", line, false, "neutral", "Atlas Control", false, min, max);
    }

    static LevelScript level(LevelScript.Secondary secondary, AllySpec cargo, List<WaveSpec> waves) {
        return new LevelScript(
                        11,
                        2,
                        0,
                        List.of(
                                new LevelScript.Section(2, 130),
                                new LevelScript.Section(ARENA_END, 30, true),
                                new LevelScript.Section(ARENA_END + 10, 130)),
                        waves,
                        List.of(),
                        List.of(),
                        0,
                        List.of(
                                cue(LevelScript.CueTrigger.ALLY_HIT, "The {ally} is hit!", 0, Integer.MAX_VALUE),
                                cue(LevelScript.CueTrigger.ALLY_LOST, "We've lost the {ally}.", 0, Integer.MAX_VALUE),
                                cue(
                                        LevelScript.CueTrigger.SECONDARY_OBJECTIVE,
                                        "All three hulls afloat.",
                                        0,
                                        Integer.MAX_VALUE),
                                cue(LevelScript.CueTrigger.LEVEL_END, "Atlas-Seven is through.", 1, 3),
                                cue(LevelScript.CueTrigger.LEVEL_END, "We lost Atlas-Seven.", 0, 0),
                                cue(LevelScript.CueTrigger.FIRST_ALLY_HIT, "First hit.", 0, Integer.MAX_VALUE)),
                        secondary,
                        List.of(),
                        List.of(),
                        List.of(boss()))
                .withWater(true)
                .withConvoy(convoy(cargo));
    }

    static LevelScript level() {
        return level(LevelScript.Secondary.afloat(100, "CONVOY"), CARGO, List.of());
    }

    private static Sortie sortie(LevelScript level, Rules rules) {
        return new Sortie(1, TestSpecs.LOADOUT, level, rules, TestSpecs.FULL_ARMOUR);
    }

    private static Sortie sortie() {
        return sortie(level(), TestSpecs.RULES.withInvulnerableShip());
    }

    /** The radio cues started in the last step, by index. */
    private static List<Integer> radio(Sortie sortie) {
        List<Integer> cued = new ArrayList<>();
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == SimEvents.Type.RADIO) {
                cued.add(events.value(i));
            }
        }
        return cued;
    }

    /** Steps with {@code commands} until the level clock is halted in the arena. */
    private static void untilHalt(Sortie sortie) {
        for (int i = 0; i < SimStep.ticks(20) && !sortie.arenaHalted(); i++) {
            sortie.step(0);
        }
        assertTrue(sortie.arenaHalted(), "the boss outlived its arena");
    }

    private static void steps(Sortie sortie, double seconds, int commands) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(commands);
        }
    }

    /** Fires at the boss over the ship until it dies. */
    private static void killBoss(Sortie sortie) {
        for (int i = 0; i < SimStep.ticks(60) && !sortie.setPiece(0).destroyed(); i++) {
            sortie.step(Command.FIRE.bit());
        }
        assertTrue(sortie.setPiece(0).destroyed(), "the boss dies under fire");
    }

    @Test
    void theHullsHoldTheirStationsAsTheSeaStreamsPast() {
        Sortie sortie = sortie();
        assertTrue(sortie.navalConvoy());
        assertFalse(sortie.airEscort());
        assertEquals(4, sortie.allyCount());
        assertEquals(3, sortie.damageableAllies(), "the frigate cannot be damaged");
        assertEquals(
                List.of("Halvorsen", "Mbeki", "Saint-Laurent", "Ruyter"),
                List.of(sortie.allyName(0), sortie.allyName(1), sortie.allyName(2), sortie.allyName(3)));
        for (int i = 0; i < SimStep.ticks(4); i++) {
            sortie.step(0);
            assertEquals(130, sortie.ally(HALVORSEN).x());
            assertEquals(PlayField.HEIGHT - 380, sortie.ally(HALVORSEN).y());
            assertEquals(PlayField.HEIGHT - 480, sortie.ally(RUYTER).y());
            for (int k = 0; k < 4; k++) {
                assertEquals(Ally.State.STATION, sortie.ally(k).state());
            }
        }
        assertEquals(2, sortie.allyLane(HALVORSEN), "at its station x 130 lies in lane 2 (120–240)");
        assertEquals(3, sortie.allyLane(MBEKI), "x 240 lies on lane 2's right edge: lane 3");
    }

    @Test
    void atTheHaltTheCargoShipsGlideToTheirLanesAndTheFrigateLeavesOnTheRealSteps() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        double halted = sortie.levelSeconds();
        for (int k = 0; k < 4; k++) {
            assertEquals(Ally.State.GLIDING, sortie.ally(k).state(), "from the halt's step, unit " + k);
        }
        int glide = 1;
        while (sortie.ally(HALVORSEN).state() == Ally.State.GLIDING && glide < SimStep.ticks(10)) {
            sortie.step(0);
            glide++;
            assertEquals(halted, sortie.levelSeconds(), "the level clock is halted");
        }
        assertEquals(SimStep.ticks(3), glide, "a 3 s glide on the real steps");
        assertEquals(Ally.State.LANE, sortie.ally(HALVORSEN).state());
        assertEquals(60, sortie.ally(HALVORSEN).x(), 1e-9, "lane 1's centre");
        assertEquals(180, sortie.ally(MBEKI).x(), 1e-9, "lane 2's centre");
        assertEquals(420, sortie.ally(SAINT_LAURENT).x(), 1e-9, "lane 4's centre");
        for (int k = 0; k < 3; k++) {
            assertEquals(PlayField.HEIGHT - 450, sortie.ally(k).y(), 1e-9, "at lane_y");
        }
        assertEquals(1, sortie.allyLane(HALVORSEN));
        assertEquals(2, sortie.allyLane(MBEKI));
        assertEquals(4, sortie.allyLane(SAINT_LAURENT));
        assertEquals(Ally.State.AWAY, sortie.ally(RUYTER).state());
        assertTrue(sortie.ally(RUYTER).y() + FRIGATE.size().height() / 2 <= 0, "off the bottom edge");
        assertEquals(0, sortie.allyLane(RUYTER));
        steps(sortie, 5, 0);
        assertEquals(60, sortie.ally(HALVORSEN).x(), 1e-9, "they hold their lanes while the boss lives");
    }

    @Test
    void aSlamHitsTheShipsInItsLaneOnceAndTheSecondSinksOne() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        assertEquals(0, sortie.slamAllies(3), "nobody in lane 3");

        assertEquals(1, sortie.slamAllies(1));
        assertEquals(0.5, sortie.ally(HALVORSEN).hpShare(), 1e-9, "one of two hits");
        assertEquals(HALVORSEN, sortie.lastAllyHit());
        assertEquals(HALVORSEN, sortie.firstAllyHit());
        assertEquals(1, sortie.ally(HALVORSEN).hitsTaken(), 1e-9);
        assertEquals(3, sortie.alliesAfloat(), "all three afloat");

        // The events of a slam come in the step that runs it: the next step reports none of them, so
        // a slam's are read straight after it from the sortie's events (the step's are cleared first).
        Sortie stepped = sortie();
        untilHalt(stepped);
        steps(stepped, 3.5, 0);
        stepped.slamAllies(2);
        assertEquals(1, stepped.events().count(SimEvents.Type.ALLY_HIT));
        assertEquals(List.of(CUE_FIRST_HIT, CUE_ALLY_HIT), radio(stepped), "the attempt's first hit, Mbeki's first");
        stepped.slamAllies(4);
        assertEquals(List.of(CUE_FIRST_HIT, CUE_ALLY_HIT, CUE_ALLY_HIT), radio(stepped), "each unit's first hit");
        assertEquals(SAINT_LAURENT, stepped.lastAllyHit());

        stepped.step(0);
        stepped.slamAllies(2);
        assertEquals(Ally.State.WRECK, stepped.ally(MBEKI).state(), "sunk on the second slam");
        assertTrue(stepped.ally(MBEKI).lost());
        assertEquals(1, stepped.events().count(SimEvents.Type.ALLY_LOST));
        assertEquals(1, stepped.events().count(SimEvents.Type.OBJECTIVE_FAILED), "the afloat objective fails at once");
        assertEquals(List.of(CUE_ALLY_LOST), radio(stepped), "no second ally-hit line for Mbeki");
        assertEquals(MBEKI, stepped.lastAllyLost());
        assertTrue(stepped.afloatFailed());
        assertEquals(2, stepped.alliesAfloat());
        assertFalse(stepped.primaryFailed(), "the convoy never fails the mission");
        assertEquals(0, stepped.slamAllies(2), "a wreck takes no more slams");
    }

    @Test
    void theFrigateIsNeverDamagedAndOnlySlamsHurtTheShips() {
        Sortie sortie = sortie();
        // At their stations the frigate (x 240) is in lane 3 with Mbeki (240) and Saint-Laurent (350).
        steps(sortie, 1, 0);
        assertEquals(3, sortie.allyLane(RUYTER));
        assertEquals(2, sortie.slamAllies(3), "Mbeki and Saint-Laurent, not the frigate");
        assertEquals(1, sortie.ally(RUYTER).hpShare());
        assertFalse(sortie.ally(RUYTER).lost());
    }

    @Test
    void theAfloatObjectiveIsMetAtTheBossesDeathWithEveryShipAfloat() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        sortie.slamAllies(1);
        int before = sortie.credits();
        boolean metSeen = false;
        for (int i = 0; i < SimStep.ticks(60) && !sortie.setPiece(0).destroyed(); i++) {
            sortie.step(Command.FIRE.bit());
            assertFalse(sortie.secondaryMet() && !sortie.setPiece(0).destroyed(), "not before the boss dies");
            if (sortie.setPiece(0).destroyed()) {
                metSeen = sortie.events().count(SimEvents.Type.OBJECTIVE_MET) == 1
                        && radio(sortie).contains(CUE_SECONDARY);
            }
        }
        assertTrue(metSeen, "met, called and paid in the step the boss dies");
        assertTrue(sortie.secondaryMet(), "a damaged ship is still afloat");
        assertTrue(sortie.credits() > before, "the secondary's credits with the boss's");
    }

    @Test
    void oneShipSunkFailsTheAfloatObjectiveForGood() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        sortie.slamAllies(4);
        sortie.slamAllies(4);
        killBoss(sortie);
        assertFalse(sortie.secondaryMet());
        assertEquals(0, sortie.events().count(SimEvents.Type.OBJECTIVE_MET));
    }

    @Test
    void afterTheBossTheShipsAfloatGlideBackAndTheFrigateReturnsToItsStation() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        sortie.slamAllies(1);
        sortie.slamAllies(1);
        killBoss(sortie);
        sortie.step(0);
        assertEquals(Ally.State.GLIDING, sortie.ally(MBEKI).state());
        assertEquals(Ally.State.GLIDING, sortie.ally(RUYTER).state());
        assertEquals(Ally.State.WRECK, sortie.ally(HALVORSEN).state(), "the sunk stays sunk");
        steps(sortie, 3, 0);
        for (int k = MBEKI; k <= RUYTER; k++) {
            assertEquals(Ally.State.STATION, sortie.ally(k).state(), "unit " + k);
        }
        assertEquals(240, sortie.ally(RUYTER).x(), 1e-9);
        assertEquals(PlayField.HEIGHT - 480, sortie.ally(RUYTER).y(), 1e-9);
        assertEquals(350, sortie.ally(SAINT_LAURENT).x(), 1e-9);
        assertEquals(PlayField.HEIGHT - 380, sortie.ally(SAINT_LAURENT).y(), 1e-9);
    }

    @Test
    void theLevelEndCuesCountTheShipsAfloat() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        sortie.slamAllies(1);
        sortie.slamAllies(1);
        killBoss(sortie);
        List<Integer> cued = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(20) && !sortie.complete(); i++) {
            sortie.step(0);
            cued.addAll(radio(sortie));
        }
        assertTrue(sortie.complete());
        assertTrue(cued.contains(CUE_END_AFLOAT), "two of three afloat: " + cued);
        assertFalse(cued.contains(CUE_END_LOST));
        assertEquals(LevelResult.Escort.NONE, sortie.result().escort(), "a naval convoy is not an escort");
    }

    @Test
    void retryFromBossRestoresTheShipsAsTheFightBegan() {
        Sortie sortie = sortie();
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        sortie.slamAllies(1);
        sortie.slamAllies(1);
        sortie.slamAllies(2);
        assertTrue(sortie.afloatFailed());
        assertTrue(sortie.bossCheckpoint());

        sortie.retryFromBoss();
        sortie.step(0);
        assertFalse(sortie.afloatFailed());
        assertEquals(3, sortie.alliesAfloat());
        assertEquals(-1, sortie.lastAllyHit());
        for (int k = 0; k < 4; k++) {
            assertEquals(Ally.State.STATION, sortie.ally(k).state(), "at its station again, unit " + k);
            assertEquals(1, sortie.ally(k).hpShare(), "unhit, unit " + k);
        }
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        assertEquals(Ally.State.LANE, sortie.ally(HALVORSEN).state(), "and it glides out again");
    }

    @Test
    void theFrigatesFlakBurstsOverTheConvoyEveryTwoSecondsWhileOnItsStation() {
        Sortie sortie = sortie();
        List<double[]> bursts = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(4.5); i++) {
            sortie.step(0);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.ALLY_FLAK) {
                    assertEquals(RUYTER, events.value(e));
                    bursts.add(new double[] {i, events.x(e), events.y(e)});
                }
            }
        }
        assertEquals(2, bursts.size(), "at 2 s and 4 s");
        assertEquals(SimStep.ticks(2), bursts.get(1)[0] - bursts.get(0)[0]);
        for (double[] burst : bursts) {
            assertTrue(Math.abs(burst[1] - 240) <= 100, "over the convoy: " + burst[1]);
            assertTrue(burst[2] >= PlayField.HEIGHT - 480 + 80, "above the frigate: " + burst[2]);
        }
        untilHalt(sortie);
        steps(sortie, 3.5, 0);
        int away = 0;
        for (int i = 0; i < SimStep.ticks(5); i++) {
            sortie.step(0);
            away += sortie.events().count(SimEvents.Type.ALLY_FLAK);
        }
        assertEquals(0, away, "none while it is away from the arena");
    }

    @Test
    void invulnerabilityChangesNeitherTheShipsNorTheObjective() {
        Sortie plain = sortie(level(), TestSpecs.RULES);
        Sortie debug = sortie(level(), TestSpecs.RULES.withInvulnerableShip());
        for (Sortie sortie : List.of(plain, debug)) {
            untilHalt(sortie);
            steps(sortie, 3.5, 0);
            sortie.slamAllies(1);
            sortie.slamAllies(1);
            sortie.slamAllies(2);
            sortie.slamAllies(2);
            sortie.slamAllies(4);
            sortie.slamAllies(4);
            assertEquals(0, sortie.alliesAfloat());
            assertTrue(sortie.afloatFailed());
            assertFalse(sortie.primaryFailed(), "losing every hull never fails the mission");
        }
    }

    @Test
    void theShipsAreInTheStateHashAndTheLevelsWithoutAConvoyHashAsBefore() {
        Sortie a = sortie();
        Sortie b = sortie();
        untilHalt(a);
        untilHalt(b);
        steps(a, 1, 0);
        steps(b, 1, 0);
        assertEquals(a.stateHash(), b.stateHash(), "deterministic");
        a.slamAllies(1);
        assertNotEquals(a.stateHash(), b.stateHash(), "a hit is in the hash");

        LevelScript plain = level();
        LevelScript without = new LevelScript(
                plain.number(),
                plain.act(),
                plain.launchSeconds(),
                plain.sections(),
                plain.waves(),
                plain.groundObjects(),
                plain.groundUnits(),
                plain.secrets(),
                plain.radio(),
                new LevelScript.Secondary(0.8, 50),
                plain.cranes(),
                plain.debris(),
                plain.setPieces(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                true);
        Sortie none = sortie(without, TestSpecs.RULES.withInvulnerableShip());
        assertFalse(none.navalConvoy());
        assertEquals(0, none.allyCount());
        assertEquals(0, none.slamAllies(1));
        assertEquals("", TestSpecs.sortie(TestSpecs.level(10, List.of())).allyName(0));
    }

    @Test
    void driftjelliesSwapInTheHaltedArenaOnTheRealSteps() {
        var field = DriftjellyTest.field(3, DriftjellyTest.jelly(), 6, new WaveSpec.Field(240, 400, 120, 50, 0));
        Sortie sortie = sortie(
                level(LevelScript.Secondary.afloat(100, "CONVOY"), CARGO, List.of(field)),
                TestSpecs.RULES.withInvulnerableShip());
        untilHalt(sortie);
        double halted = sortie.levelSeconds();
        int swaps = 0;
        for (int i = 0; i < SimStep.ticks(12); i++) {
            sortie.step(0);
            for (int e = 0; e < sortie.enemyCount(); e++) {
                swaps += sortie.enemy(e).swap(1) == 0 ? 1 : 0;
            }
        }
        assertEquals(halted, sortie.levelSeconds());
        assertTrue(swaps > 0, "their timers keep going while the clock is halted");
    }

    @Test
    void theConvoyFightDoesNotAllocate() {
        long allocated = Allocations.least(NavalConvoyTest::sortie, sortie -> {
            untilHalt(sortie);
            steps(sortie, 3.5, 0);
            sortie.slamAllies(1);
            sortie.slamAllies(2);
            sortie.slamAllies(2);
            for (int i = 0; i < SimStep.ticks(20) && !sortie.complete(); i++) {
                sortie.step(Command.FIRE.bit());
            }
        });
        assertEquals(0, allocated);
    }

    @Test
    void aNavalConvoysUnitsTakeLanesOfItsArena() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LevelScript.Naval(convoy(CARGO).units(), 3, 90, 3, 120),
                "Saint-Laurent's lane 4 of 3");
        assertThrows(
                IllegalArgumentException.class,
                () -> new LevelScript.Naval(convoy(CARGO).units(), 3, 90, 5, 120),
                "five 120 px lanes leave the play field");
        assertFalse(LevelScript.Secondary.afloat(100, "CONVOY").none());
        assertTrue(CARGO.damageable());
        assertFalse(FRIGATE.damageable());
    }
}
