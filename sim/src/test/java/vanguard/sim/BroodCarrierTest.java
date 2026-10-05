package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.CARRIER_ARRIVES;
import static vanguard.sim.TestSpecs.CARRIER_CORE;
import static vanguard.sim.TestSpecs.CARRIER_TURRET;
import static vanguard.sim.TestSpecs.SACS;
import static vanguard.sim.TestSpecs.SAC_A_LEFT;
import static vanguard.sim.TestSpecs.SAC_A_RIGHT;
import static vanguard.sim.TestSpecs.SAC_B_LEFT;
import static vanguard.sim.TestSpecs.SAC_B_RIGHT;
import static vanguard.sim.TestSpecs.STATION_X;
import static vanguard.sim.TestSpecs.STATION_Y;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The boss engine of M4 part G (design/enemies/bosses/brood-carrier) on a small test carrier
 * ({@link TestSpecs#carrier}): the high-air arrival that engages at once, the timed phase and the
 * timeout, the invulnerable descent and turn into another pose, the windows that open the sacs
 * and release units, the fan of a fire-only turret, a spiral and a ring together, the surviving
 * sacs paying at the kill, the checkpoint, determinism and no allocation.
 */
class BroodCarrierTest {
    /** The ship's place for the boss alone, bottom centre. */
    private static final double SHIP_X = 240;

    private static final double SHIP_Y = 60;

    /** What the boss sends to the level, recorded. */
    private static final class Recorder implements SetPiece.BossActions {
        final List<String> launches = new ArrayList<>();
        final List<Integer> phases = new ArrayList<>();
        final List<Integer> timeouts = new ArrayList<>();
        int fans;
        int fanBullets;
        int rings;
        int bullets;

        @Override
        public void aimed(double x, double y, EnemyGun gun) {
            fans++;
            fanBullets += gun.fan();
        }

        @Override
        public void ring(double x, double y, int count, double speed, double damage) {
            rings++;
        }

        @Override
        public void bullet(double x, double y, double angle, double speed, double damage) {
            bullets++;
        }

        @Override
        public void release(BossSpec.Stream stream, boolean left) {}

        @Override
        public void phase(int phase) {
            phases.add(phase);
        }

        @Override
        public void timeout(int phase) {
            timeouts.add(phase);
        }

        @Override
        public void launch(BossSpec.Spawn spawn, double x, double y, double angle) {
            launches.add(spawn.name());
        }

        void clear() {
            launches.clear();
            fans = fanBullets = rings = bullets = 0;
        }
    }

    /** The carrier alone on the level clock, stepped as the sortie steps it. */
    private static final class Rig {
        final SetPiece piece;
        final Recorder actions = new Recorder();
        int tick;

        Rig(LevelScript.SetPieceSpec spec) {
            piece = new SetPiece(spec);
        }

        void step() {
            piece.update(tick++);
            piece.act(SHIP_X, SHIP_Y, actions, true);
        }

        /** Steps until {@code seconds} after the carrier arrived. */
        void until(double seconds) {
            while (piece.bossTicks() < SimStep.ticks(seconds)) {
                step();
            }
        }
    }

    private static Rig rig() {
        return new Rig(TestSpecs.carrier(true, 3));
    }

    @Test
    void itArrivesOnHighAirEngagedAndStopsOverTheShip() {
        Rig rig = rig();
        SetPiece carrier = rig.piece;
        assertEquals(SetPiece.Motion.NONE, carrier.motion());
        rig.until(0);
        assertTrue(carrier.present());
        assertEquals(Layer.HIGH_AIR, carrier.layer());
        assertFalse(carrier.onPlane(), "out of reach of contact on high air");
        assertTrue(carrier.engaged(), "the phases run from the arrival");
        assertEquals(SetPiece.Motion.PASS, carrier.motion());
        assertEquals(1, carrier.altitude(1), 1e-9);
        assertEquals(BossSpec.HIGH_AIR_SCALE, carrier.scale(), 1e-9);
        assertEquals(
                -60 * BossSpec.HIGH_AIR_SCALE, carrier.partOffsetY(SAC_A_LEFT), 1e-9, "offsets at the high-air scale");
        assertTrue(carrier.renderY(1) - 140 * BossSpec.HIGH_AIR_SCALE > PlayField.HEIGHT, "it enters from above");
        assertTrue(carrier.partShielded(SAC_A_LEFT), "a shut sac takes no damage");
        assertTrue(carrier.partShielded(CARRIER_CORE));
        assertTrue(carrier.partShielded(CARRIER_TURRET));

        rig.until(14);
        assertEquals(SetPiece.Motion.HOLD, carrier.motion());
        assertEquals(270, carrier.renderY(1), 1e-9);
        assertEquals(240, carrier.renderX(1), 1e-9);
        assertEquals(0, carrier.phase());
    }

    @Test
    void theTimedPhaseEndsInAnInvulnerableDescentAndTurnIntoBroadside() {
        Rig rig = rig();
        SetPiece carrier = rig.piece;
        rig.until(25 - 2 * SimStep.SECONDS);
        assertEquals(0, carrier.phase());
        rig.until(25);
        assertEquals(1, carrier.phase());
        assertEquals(List.of(1), rig.actions.phases);
        assertEquals(List.of(0), rig.actions.timeouts, "phase one ends on its timer");
        assertTrue(carrier.endedOnTimeout(0));

        rig.step();
        assertEquals(SetPiece.Motion.DESCEND, carrier.motion());
        for (int p = 0; p < carrier.partCount(); p++) {
            assertTrue(carrier.partShielded(p), "invulnerable while it moves: " + p);
        }
        assertEquals(Layer.HIGH_AIR, carrier.layer());
        double altitude = carrier.altitude(1);
        rig.until(26);
        assertTrue(carrier.altitude(1) < altitude, "it sinks");
        assertEquals(0, carrier.pose());

        rig.until(27 + 0.5);
        assertEquals(SetPiece.Motion.TURN, carrier.motion());
        assertEquals(Layer.AIR, carrier.layer());
        assertTrue(carrier.onPlane());
        assertEquals(1, carrier.scale(), 1e-9);
        assertEquals(STATION_X, carrier.renderX(1), 1e-9);
        assertEquals(STATION_Y, carrier.renderY(1), 1e-9);
        assertEquals(0.25, carrier.turnShare(1), 0.02);
        assertEquals(0, carrier.pose(), "the pose switches half-way through the turn");
        assertEquals(1, carrier.turnToPose());
        rig.until(28.5);
        assertEquals(1, carrier.pose());
        assertEquals("broadside", carrier.poseName());
        assertEquals(new Hitbox(300, 100), carrier.body());
        assertEquals(140, carrier.partOffsetX(CARRIER_TURRET), 1e-9);

        rig.until(29 + SimStep.SECONDS);
        assertEquals(SetPiece.Motion.HOLD, carrier.motion());
        assertEquals(0, carrier.turnShare(1), 1e-9);
        assertTrue(carrier.partShielded(CARRIER_TURRET), "the turret is fire-only");
        assertTrue(carrier.partShielded(CARRIER_CORE), "the core waits for phase 3");
        assertEquals(0, carrier.phaseSeconds(), 0.05, "the timeout's clock starts after the move");
    }

    @Test
    void thePairsOpenInTurnAndReleaseTheirSpawnsAlternately() {
        Rig rig = rig();
        SetPiece carrier = rig.piece;
        rig.until(14);
        rig.actions.clear();
        // In the hold, the windows come every 2.5 s, pair A and pair B in turn.
        List<Integer> opened = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(10); i++) {
            boolean before = carrier.partOpen(SAC_A_LEFT) || carrier.partOpen(SAC_B_LEFT);
            int launches = rig.actions.launches.size();
            rig.step();
            if (rig.actions.launches.size() > launches) {
                opened.add(carrier.partOpen(SAC_A_LEFT) && !before ? 0 : 1);
                assertTrue(carrier.partOpen(SAC_A_LEFT) == carrier.partOpen(SAC_A_RIGHT), "a pair opens together");
                assertFalse(carrier.partShielded(carrier.partOpen(SAC_A_LEFT) ? SAC_A_LEFT : SAC_B_LEFT));
                assertTrue(carrier.partShielded(carrier.partOpen(SAC_A_LEFT) ? SAC_B_LEFT : SAC_A_LEFT));
            }
        }
        assertEquals(4, opened.size());
        assertTrue(opened.get(0) != opened.get(1) && opened.get(1) != opened.get(2), "the pairs take turns: " + opened);
        long skitters =
                rig.actions.launches.stream().filter("pass-skitters"::equals).count();
        long needlers =
                rig.actions.launches.stream().filter("pass-needlers"::equals).count();
        assertEquals(2 * 4 + 2 * 2, skitters + needlers, "4 Skitters or 2 Needlers per opening");
        assertEquals(2.0 * needlers, skitters, 1e-9);
    }

    @Test
    void aDestroyedSacHalvesItsPairsSpawnsAndAGonePairIsSkipped() {
        Rig rig = rig();
        SetPiece carrier = rig.piece;
        rig.until(13);
        carrier.damagePart(SAC_A_LEFT, 1000);
        rig.actions.clear();
        int aOpenings = 0;
        int aUnits = 0;
        for (int i = 0; i < SimStep.ticks(5); i++) {
            boolean wasOpen = carrier.partOpen(SAC_A_RIGHT);
            int launches = rig.actions.launches.size();
            rig.step();
            if (!wasOpen && carrier.partOpen(SAC_A_RIGHT)) {
                aOpenings++;
                aUnits += rig.actions.launches.size() - launches;
                assertFalse(carrier.partOpen(SAC_A_LEFT), "a destroyed sac stays shut");
            }
        }
        assertEquals(1, aOpenings, "the pairs take turns every 2.5 s");
        // With two pairs and two spawns in turn, pair A always releases the Skitters: half of 4.
        assertEquals(2, aUnits, "half of 4 Skitters, rounded up");

        carrier.damagePart(SAC_A_RIGHT, 1000);
        rig.actions.clear();
        int bOpenings = 0;
        for (int i = 0; i < SimStep.ticks(5); i++) {
            boolean wasOpen = carrier.partOpen(SAC_B_LEFT);
            rig.step();
            bOpenings += !wasOpen && carrier.partOpen(SAC_B_LEFT) ? 1 : 0;
            assertFalse(carrier.partOpen(SAC_A_RIGHT));
        }
        assertEquals(2, bOpenings, "only pair B is left, so it opens at every window");
    }

    @Test
    void boltsPassUnderTheHighAirCarrierAndHomingHitsOnlyOpenSacs() {
        LevelScript level = TestSpecs.carrierLevel(TestSpecs.carrier(false, 3));
        Sortie bolts = new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        flyInPhaseOne(bolts);
        for (int p : SACS) {
            assertEquals(20, bolts.setPiece(0).partHp(p), 1e-9, "a bolt does not reach high air");
        }
        assertEquals(1, bolts.setPiece(0).barShare(), 1e-9);

        Loadout missiles = TestSpecs.loadout(
                new Armament.Mount(Armament.Slot.FRONT, TestSpecs.PULSE, TestSpecs.PULSE_L2),
                new Armament.Mount(Armament.Slot.LEFT_WING, TestSpecs.HOMING, TestSpecs.HOMING));
        Sortie homing = new Sortie(1, missiles, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        flyInPhaseOne(homing);
        SetPiece carrier = homing.setPiece(0);
        double sacs = 0;
        for (int p : SACS) {
            sacs += carrier.partHp(p);
        }
        assertTrue(sacs < 80, "missiles reach the open sacs: " + sacs);
        assertEquals(30, carrier.partHp(CARRIER_CORE), 1e-9, "the shut core takes nothing");
        assertEquals(1, carrier.partHp(CARRIER_TURRET), 1e-9, "the turret takes nothing");
    }

    /** Fires from the start until 20 s after the carrier arrived (its hold in phase 1). */
    private static void flyInPhaseOne(Sortie sortie) {
        while (sortie.setPiece(0).bossSeconds() < 20) {
            sortie.step(Command.FIRE.bit());
        }
        assertEquals(0, sortie.setPiece(0).phase());
    }

    @Test
    void phaseTwoEndsOnTheSacsNotOnATimeoutAndLeavesPhaseThreeNothingToSpawn() {
        Rig rig = rig();
        SetPiece carrier = rig.piece;
        rig.until(32);
        assertEquals(1, carrier.phase());
        assertTrue(carrier.partWindowed(SAC_A_LEFT));
        for (int p : SACS) {
            carrier.damagePart(p, 1000);
        }
        rig.step();
        assertEquals(2, carrier.phase());
        assertFalse(carrier.endedOnTimeout(1));
        assertEquals(List.of(0), rig.actions.timeouts);
        rig.actions.clear();
        rig.until(32 + 13);
        assertTrue(rig.actions.launches.isEmpty(), "no surviving pair, no timeout spawns");
        assertTrue(rig.actions.rings > 0 && rig.actions.bullets > 0);
    }

    @Test
    void phaseTwoTimesOutAfterSeventySecondsAndTheSurvivorsSpawnInPhaseThree() {
        Rig rig = rig();
        SetPiece carrier = rig.piece;
        rig.until(29 + SimStep.SECONDS);
        assertEquals(1, carrier.phase());
        rig.actions.clear();
        // Between volleys one pair opens for 2 s; each window releases one Skitter per open sac.
        int windows = 0;
        int overlaps = 0;
        for (int i = 0; i < SimStep.ticks(24); i++) {
            boolean wasOpen = carrier.partOpen(SAC_A_LEFT) || carrier.partOpen(SAC_B_LEFT);
            int fans = rig.actions.fans;
            rig.step();
            boolean open = carrier.partOpen(SAC_A_LEFT) || carrier.partOpen(SAC_B_LEFT);
            windows += open && !wasOpen ? 1 : 0;
            overlaps += rig.actions.fans > fans && open ? 1 : 0;
        }
        assertEquals(10, rig.actions.fans, "a fan every 2.4 s");
        assertEquals(50, rig.actions.fanBullets, "5-way fans");
        assertEquals(0, overlaps, "the windows open between the volleys");
        assertEquals(10, windows);
        assertEquals(20, rig.actions.launches.size(), "one Skitter per open sac");

        carrier.damagePart(SAC_B_LEFT, 1000);
        rig.until(29 + 70 - 0.1);
        assertEquals(1, carrier.phase());
        rig.until(29 + 70 + 2 * SimStep.SECONDS);
        assertEquals(2, carrier.phase());
        assertTrue(carrier.endedOnTimeout(1));
        assertEquals(List.of(0, 1), rig.actions.timeouts);

        rig.actions.clear();
        int opened = 0;
        for (int i = 0; i < SimStep.ticks(12.5); i++) {
            boolean wasOpen = carrier.partOpen(SAC_A_LEFT);
            rig.step();
            if (!wasOpen && carrier.partOpen(SAC_A_LEFT)) {
                opened++;
                assertTrue(carrier.partOpen(SAC_B_RIGHT), "every surviving pair opens together");
                assertFalse(carrier.partShielded(SAC_A_LEFT), "open sacs take damage");
            }
        }
        assertEquals(2, opened, "every 6 s after the iris's second");
        assertEquals(2 * (2 + 1), rig.actions.launches.size(), "2 Skitters per pair, 1 from the halved pair");
    }

    @Test
    void theCoreFiresItsSpiralAndItsRingTogether() {
        for (int arms : new int[] {3, 4}) {
            Rig rig = new Rig(TestSpecs.carrier(false, arms));
            rig.until(32);
            for (int p : SACS) {
                rig.piece.damagePart(p, 1000);
            }
            rig.step();
            assertEquals(2, rig.piece.phase());
            assertFalse(rig.piece.partShielded(CARRIER_CORE));
            rig.actions.clear();
            rig.until(32 + 1 + 8);
            assertEquals(2, rig.actions.rings, "a ring every 4 s");
            // 8 s at a bullet per arm every 0.375 s, the first at the end of the iris's second.
            int volleys = (int) Math.floor((8 - SimStep.SECONDS) / 0.375) + 1;
            assertEquals(
                    volleys * arms, rig.actions.bullets, 2 * arms, "the spiral beside the ring, " + arms + " arms");
        }
    }

    @Test
    void theTurretFiresOutOfTheBarAndTheSurvivingSacsPayAtTheKill() {
        Sortie sortie = TestSpecs.sortie(TestSpecs.carrierLevel(TestSpecs.carrier(false, 3)));
        SetPiece carrier = sortie.setPiece(0);
        while (carrier.bossSeconds() < 40) {
            sortie.step(0);
        }
        assertEquals(1, carrier.phase());
        assertTrue(sortie.bulletCount() > 0, "the turret's fans");
        sortie.destroyPart(0, SAC_A_LEFT);
        assertEquals((20 * 3 + 30) / 110.0, carrier.barShare(), 1e-9, "the turret is not in the bar");
        assertEquals(25, sortie.credits());

        sortie.destroyPart(0, CARRIER_CORE);
        assertTrue(carrier.destroyed());
        SimEvents events = sortie.events();
        int parts = 0;
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == SimEvents.Type.BOSS_DESTROYED) {
                assertEquals(150, events.value(i), "the credit shower: every sac and the core");
            }
            parts += events.type(i) == SimEvents.Type.PART_DESTROYED ? 1 : 0;
        }
        assertEquals(5, parts, "the sac before, the core and the three survivors; the turret pays nothing");
        assertEquals(150, sortie.credits(), "the surviving sacs burst and pay");
        assertEquals(0, carrier.barShare(), 1e-9);
    }

    @Test
    void retryFromTheBossBringsItBackNoseDownOnHighAir() {
        Sortie sortie = TestSpecs.sortie(TestSpecs.carrierLevel(TestSpecs.carrier(false, 3)));
        SetPiece carrier = sortie.setPiece(0);
        while (carrier.bossSeconds() < 35) {
            sortie.step(0);
        }
        assertEquals(1, carrier.pose());
        assertEquals(Layer.AIR, carrier.layer());
        sortie.retryFromBoss();
        sortie.step(0);
        assertEquals(CARRIER_ARRIVES, sortie.levelSeconds(), 1e-9);
        assertEquals(0, carrier.pose());
        assertEquals(0, carrier.phase());
        assertEquals(Layer.HIGH_AIR, carrier.layer());
        assertEquals(SetPiece.Motion.PASS, carrier.motion());
        assertFalse(carrier.endedOnTimeout(0));
    }

    @Test
    void theSameCommandsGiveTheSameStateAndTheFightDoesNotAllocate() {
        long[] hashes = new long[2];
        for (int run = 0; run < 2; run++) {
            Sortie sortie = carrierSortie();
            fight(sortie);
            hashes[run] = sortie.stateHash();
        }
        assertEquals(hashes[0], hashes[1]);

        long allocated = Allocations.least(BroodCarrierTest::carrierSortie, BroodCarrierTest::fight);
        assertEquals(0, allocated, "the fight allocated " + allocated + " bytes");
    }

    private static Sortie carrierSortie() {
        Loadout missiles = TestSpecs.loadout(
                new Armament.Mount(Armament.Slot.FRONT, TestSpecs.PULSE, TestSpecs.PULSE_L2),
                new Armament.Mount(Armament.Slot.LEFT_WING, TestSpecs.HOMING, TestSpecs.HOMING));
        return new Sortie(
                1,
                missiles,
                TestSpecs.carrierLevel(TestSpecs.carrier(true, 3)),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
    }

    /** Every phase and move, the windows, the timeout's spawns and a retry, the guns firing. */
    private static void fight(Sortie sortie) {
        for (int i = 0; i < 60 * 130; i++) {
            if (i == 60 * 120) {
                sortie.retryFromBoss();
            }
            sortie.step(Command.FIRE.bit() | (i / 90 % 2 == 0 ? Command.LEFT.bit() : Command.RIGHT.bit()));
        }
    }

    @Test
    void theWindowsCountAsTheSpawnKinds() {
        BossSpec boss = TestSpecs.carrier(true, 3).boss().orElseThrow();
        assertEquals(List.of(TestSpecs.SKITTER, TestSpecs.NEEDLER), boss.spawnKinds());
        assertTrue(TestSpecs.carrier(false, 3).boss().orElseThrow().spawnKinds().isEmpty());
        assertEquals(List.of(SAC_A_LEFT, SAC_A_RIGHT, SAC_B_LEFT, SAC_B_RIGHT), SACS);
    }
}
