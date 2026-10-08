package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartCSpecs.count;
import static vanguard.sim.PartCSpecs.find;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.Enemy.AmbushPhase;

/**
 * The Wraith (design/enemies/air/wraith, user decision D6 = a of M5 part D): cloaked on {@code
 * high-air} it swoops down its lane past the ship and off the bottom edge; after the bottom edge's
 * warning it comes back up to its hold point, decloaks with a 0.4 s flash (on {@code air} from its
 * start, the {@code DECLOAK} event), holds 2.5 s (hard 3.0 s) with two bursts up the screen and leaves
 * decloaked up the nearer side lane.
 */
class WraithTest {
    private static final int FLASH = SimStep.ticks(0.4);
    private static final int HOLD = SimStep.ticks(2.5);

    /** One step of a lone Wraith's way. */
    private record Seen(
            int step,
            AmbushPhase phase,
            Layer layer,
            double x,
            double y,
            boolean decloakEvent,
            WaveSpec.Entry entry,
            boolean cloaked,
            double decloak,
            int warnings) {}

    /** A lone Wraith at t=1, followed until it is gone, flying {@code commands}. */
    private static List<Seen> follow(Sortie sortie, int commands) {
        List<Seen> seen = new ArrayList<>();
        Enemy wraith = null;
        for (int step = 0; step < SimStep.ticks(20); step++) {
            sortie.step(commands);
            if (wraith == null) {
                wraith = find(sortie, "wraith");
            } else if (find(sortie, "wraith") == null) {
                break;
            }
            if (wraith != null) {
                seen.add(new Seen(
                        step,
                        wraith.ambushPhase(),
                        wraith.layer(),
                        wraith.x(),
                        wraith.y(),
                        count(sortie, SimEvents.Type.DECLOAK) > 0,
                        wraith.entry(),
                        wraith.cloaked(),
                        wraith.decloak(1),
                        sortie.edgeWarnings()));
            }
        }
        return seen;
    }

    private static Sortie lone(EnemySpec wraith) {
        return PartDSpecs.sortie(PartDSpecs.level(30, List.of(PartDSpecs.ambush(1, wraith, 1))));
    }

    private static int first(List<Seen> seen, AmbushPhase phase) {
        for (Seen s : seen) {
            if (s.phase() == phase) {
                return s.step();
            }
        }
        throw new AssertionError("never " + phase);
    }

    private static long steps(List<Seen> seen, AmbushPhase phase) {
        return seen.stream().filter(s -> s.phase() == phase).count();
    }

    @Test
    void itIsCloakedOnHighAirUntilItsFlashAndOnAirFromItsStart() {
        List<Seen> seen = follow(lone(PartDSpecs.wraith()), Command.NONE);

        AmbushPhase last = AmbushPhase.SWOOP;
        for (Seen s : seen) {
            assertTrue(s.phase().compareTo(last) >= 0, "the phases in order: " + last + " then " + s.phase());
            last = s.phase();
            boolean decloaked = s.phase().compareTo(AmbushPhase.DECLOAK) >= 0;
            assertEquals(decloaked ? Layer.AIR : Layer.HIGH_AIR, s.layer(), s.phase() + " at " + s.step());
            assertEquals(!decloaked, s.cloaked());
        }
        assertEquals(AmbushPhase.EXIT, last);
        assertEquals(FLASH, steps(seen, AmbushPhase.DECLOAK), "a 0.4 s flash");
        assertEquals(HOLD, steps(seen, AmbushPhase.HOLD), "a 2.5 s hold");
        List<Integer> events =
                seen.stream().filter(Seen::decloakEvent).map(Seen::step).toList();
        assertEquals(List.of(first(seen, AmbushPhase.DECLOAK)), events, "one DECLOAK, at the flash's start");
    }

    @Test
    void theFlashFadesItInAndItHoldsLowBehindTheShip() {
        List<Seen> seen = follow(lone(PartDSpecs.wraith()), Command.NONE);

        double previous = 0;
        for (Seen s : seen) {
            switch (s.phase()) {
                case SWOOP, GAP, RISE -> assertEquals(0, s.decloak());
                case DECLOAK -> {
                    assertTrue(s.decloak() > previous && s.decloak() <= 1, "rising over the flash: " + s.decloak());
                    previous = s.decloak();
                }
                default -> assertEquals(1, s.decloak());
            }
            if (s.phase().compareTo(AmbushPhase.DECLOAK) <= 0) {
                assertEquals(PlayField.WIDTH / 2.0, s.x(), 1e-9, "a lone one in the middle lane");
            }
        }
        Seen hold = seen.get(first(seen, AmbushPhase.HOLD) - seen.getFirst().step());
        assertTrue(
                hold.y() >= PlayField.HEIGHT - 520 && hold.y() <= PlayField.HEIGHT - 470,
                "20-70 px above the bottom edge: " + hold.y());
        assertTrue(hold.y() < Ship.START_Y, "behind the ship");
        assertTrue(
                seen.stream().filter(s -> s.phase() == AmbushPhase.SWOOP).anyMatch(s -> s.y() < 0),
                "its swoop leaves the bottom edge");
        assertTrue(
                seen.stream().filter(s -> s.phase() == AmbushPhase.GAP).allMatch(s -> s.y() < 0),
                "below the bottom edge in its gap");
    }

    @Test
    void theBottomEdgeIsWarnedThreeSecondsAheadOfItsReEntryNotOfItsEntry() {
        List<Seen> seen = follow(lone(PartDSpecs.wraith()), Command.NONE);
        int rise = first(seen, AmbushPhase.RISE);
        int bottom = WarningEdge.BOTTOM.bit();

        for (Seen s : seen) {
            boolean warned = (s.warnings() & bottom) != 0;
            if (s.step() < rise - SimStep.ticks(3) - 1 || s.step() > rise) {
                assertFalse(warned, "no warning at " + s.step() + " (re-entry at " + rise + ")");
            } else if (s.step() >= rise - SimStep.ticks(3) + 1 && s.step() < rise) {
                assertTrue(warned, "warned at " + s.step() + " (re-entry at " + rise + ")");
            }
            // A rear wave for Rook's Trail from its re-entry on.
            assertEquals(s.step() >= rise ? WaveSpec.Entry.REAR : WaveSpec.Entry.FRONT, s.entry(), "at " + s.step());
        }
        assertTrue(
                seen.stream().filter(s -> s.phase() == AmbushPhase.SWOOP).anyMatch(s -> (s.warnings() & bottom) != 0),
                "the warning starts while it still swoops");
    }

    @Test
    void itLeavesDecloakedUpTheNearerSideLaneClearOfTheShuttleBand() {
        List<Seen> seen = follow(lone(PartDSpecs.wraith()), Command.NONE);
        Seen hold = seen.get(first(seen, AmbushPhase.HOLD) - seen.getFirst().step());

        List<Seen> exit =
                seen.stream().filter(s -> s.phase() == AmbushPhase.EXIT).toList();
        assertFalse(exit.isEmpty());
        for (Seen s : exit) {
            assertEquals(Layer.AIR, s.layer(), "decloaked on its way out");
            assertTrue(s.x() >= 25, "on the play field: " + s.x());
            if (s.y() > hold.y() + 120) {
                // A tie goes left: its centre 40 px from the left edge, its 50 px hit box clear of x = 120.
                assertEquals(40, s.x(), 1e-6, "up the side lane at " + s.y());
            }
        }
        assertTrue(
                exit.getLast().y() > PlayField.HEIGHT,
                "it leaves through the top edge: " + exit.getLast().y());
        double climb = exit.getLast().y() - exit.get(exit.size() - 2).y();
        assertEquals(120 * SimStep.SECONDS, climb, 1e-6, "at its straight speed");
    }

    @Test
    void standardShotsPassItAndItTouchesNothingWhileCloaked() {
        // The ship fires up its own lane, the lone Wraith's, as it swoops down through the ship.
        Sortie sortie = PartDSpecs.sortie(PartDSpecs.level(30, List.of(PartDSpecs.ambush(1, PartDSpecs.wraith(), 1))));
        Sortie touchable = new Sortie(
                1,
                TestSpecs.LOADOUT,
                PartDSpecs.level(30, List.of(PartDSpecs.ambush(1, PartDSpecs.wraith(), 1))),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
        for (Sortie s : List.of(sortie, touchable)) {
            Defences defences = s.ship().defences();
            double before = defences.shield() + defences.armour();
            Enemy wraith = null;
            boolean passed = false;
            for (int step = 0; step < SimStep.ticks(8); step++) {
                s.step(Command.FIRE.bit());
                wraith = wraith == null ? find(s, "wraith") : wraith;
                if (wraith == null || wraith.ambushPhase().compareTo(AmbushPhase.RISE) >= 0) {
                    continue;
                }
                passed |= Math.abs(wraith.y() - s.ship().y()) < 20;
                assertEquals(16, wraith.hp(), "no bolt hits it cloaked");
                assertEquals(before, defences.shield() + defences.armour(), "it passes the ship without contact");
            }
            assertNotNull(wraith);
            assertTrue(passed, "it flew past the ship");
        }
    }

    @Test
    void homingHitsItWhileCloaked() {
        Loadout homing = TestSpecs.loadout(new Armament.Mount(Armament.Slot.FRONT, TestSpecs.HOMING, TestSpecs.HOMING));
        Sortie sortie = new Sortie(
                1,
                homing,
                PartDSpecs.level(30, List.of(PartDSpecs.ambush(1, PartDSpecs.wraith(), 1))),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        boolean hitCloaked = false;
        for (int step = 0; step < SimStep.ticks(8) && !hitCloaked; step++) {
            sortie.step(Command.FIRE.bit());
            Enemy wraith = find(sortie, "wraith");
            hitCloaked = wraith != null && wraith.cloaked() && wraith.hp() < 16;
        }

        assertTrue(hitCloaked, "a homing missile hits it on high-air");
    }

    /** The steps of the shots it fired, from its stop (the flash's start). */
    private static List<Integer> shots(EnemySpec spec) {
        Sortie sortie = lone(spec);
        List<Integer> shots = new ArrayList<>();
        int stop = -1;
        for (int step = 0; step < SimStep.ticks(20); step++) {
            sortie.step(step < SimStep.ticks(1) ? Command.UP.bit() : Command.NONE);
            Enemy wraith = find(sortie, "wraith");
            if (count(sortie, SimEvents.Type.DECLOAK) > 0) {
                stop = step;
            }
            if (count(sortie, SimEvents.Type.ENEMY_FIRED) > 0) {
                assertNotNull(wraith);
                assertTrue(stop >= 0, "no shot before its decloak");
                assertTrue(wraith.ambushPhase() == AmbushPhase.HOLD, "only in its hold: " + wraith.ambushPhase());
                shots.add(step - stop);
            }
        }
        return shots;
    }

    @Test
    void itFiresTwoFiveShotBurstsUpTheScreenInItsHold() {
        List<Integer> shots = shots(PartDSpecs.wraith());
        int first = SimStep.ticks(0.9);
        int gap = SimStep.ticks(0.12);
        int interval = SimStep.ticks(1.2);
        List<Integer> expected = new ArrayList<>();
        for (int burst = 0; burst < 2; burst++) {
            for (int shot = 0; shot < 5; shot++) {
                expected.add(first + burst * interval + shot * gap);
            }
        }

        assertEquals(expected, shots, "0.5 s into the hold, then 1.2 s later; 0.12 s apart");
    }

    @Test
    void itsBurstsGoStraightUpAsAFixedFanWhereverTheShipIs() {
        // The ship climbs above it, or flies off to the left or the right, before its hold.
        for (int commands : new int[] {Command.UP.bit(), Command.LEFT.bit(), Command.RIGHT.bit()}) {
            Sortie sortie = lone(PartDSpecs.wraith());
            List<Double> angles = new ArrayList<>();
            for (int step = 0; step < SimStep.ticks(20); step++) {
                sortie.step(step < SimStep.ticks(1) ? commands : Command.NONE);
                Enemy wraith = find(sortie, "wraith");
                if (count(sortie, SimEvents.Type.ENEMY_FIRED) > 0) {
                    EnemyBullet shot = nearest(sortie, wraith);
                    angles.add(Math.toDegrees(StrictMath.atan2(shot.vy(), shot.vx())));
                    assertEquals(220, Math.hypot(shot.vx(), shot.vy()), 1e-3);
                }
            }

            assertEquals(10, angles.size(), "two 5-shot bursts");
            for (int k = 0; k < 10; k++) {
                assertEquals(
                        110 - 10 * (k % 5), angles.get(k), 0.01, "left to right across the 40° fan, centred on up");
            }
        }
    }

    /** The enemy bullet nearest {@code enemy}: the shot it just fired. */
    private static EnemyBullet nearest(Sortie sortie, Enemy enemy) {
        EnemyBullet nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            double distance = Math.hypot(bullet.x() - enemy.x(), bullet.y() - enemy.y());
            if (distance < best) {
                best = distance;
                nearest = bullet;
            }
        }
        assertNotNull(nearest, "a shot in the air");
        return nearest;
    }

    @Test
    void theNoFireDistanceDoesNotSilenceItsBursts() {
        // The ship starts above its lane and stays there: the Wraith holds right under it.
        Sortie sortie = lone(PartDSpecs.wraith());
        int shots = 0;
        double closest = Double.POSITIVE_INFINITY;
        for (int step = 0; step < SimStep.ticks(20); step++) {
            sortie.step(Command.DOWN.bit());
            Enemy wraith = find(sortie, "wraith");
            if (count(sortie, SimEvents.Type.ENEMY_FIRED) > 0) {
                shots++;
                closest = Math.min(
                        closest,
                        Math.hypot(
                                wraith.x() - sortie.ship().x(),
                                wraith.y() - sortie.ship().y()));
            }
        }

        assertEquals(10, shots);
        assertTrue(closest < 72, "fired inside the no-fire distance: " + closest);
    }

    @Test
    void hardHoldsThreeSecondsAndFiresSevenShotBursts() {
        Sortie sortie = lone(PartDSpecs.wraith(3.0, 7));
        List<Seen> seen = follow(sortie, Command.NONE);

        assertEquals(SimStep.ticks(3.0), steps(seen, AmbushPhase.HOLD));
        assertEquals(14, shots(PartDSpecs.wraith(3.0, 7)).size(), "two 7-shot bursts");
    }

    @Test
    void anAmbushOfThreeHoldsInItsLanesAndLeavesByTheNearerSides() {
        Sortie sortie = PartDSpecs.sortie(PartDSpecs.level(30, List.of(PartDSpecs.ambush(1, PartDSpecs.wraith(), 3))));
        List<Double> lanes = new ArrayList<>();
        List<Double> exits = new ArrayList<>();
        for (int step = 0; step < SimStep.ticks(20); step++) {
            sortie.step(Command.NONE);
            for (Enemy wraith : PartDSpecs.all(sortie, "wraith")) {
                if (wraith.ambushPhase() == AmbushPhase.HOLD && !lanes.contains(wraith.x())) {
                    lanes.add(wraith.x());
                }
            }
            for (Enemy wraith : PartDSpecs.all(sortie, "wraith")) {
                if (wraith.ambushPhase() == AmbushPhase.EXIT && wraith.y() > PlayField.HEIGHT - 200) {
                    double x = Math.round(wraith.x());
                    if (!exits.contains(x)) {
                        exits.add(x);
                    }
                }
            }
        }

        assertEquals(List.of(120.0, 240.0, 360.0), lanes.stream().sorted().toList(), "x = (i + 1) × 480 ÷ 4");
        assertEquals(
                List.of(40.0, 440.0), exits.stream().sorted().toList(), "out by the nearer side lanes, a tie left");
    }
}
