package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartCSpecs.count;
import static vanguard.sim.PartCSpecs.find;
import static vanguard.sim.PartCSpecs.ravager;
import static vanguard.sim.PartCSpecs.walkers;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The Ravager's pounce (design/enemies/ground/ravager, M5 part C; user decision D4 = a): within
 * 200 px of the ship it leaps over 0.75 s, its middle passing over the ship's position at take-off
 * and landing as far beyond it on the play field (the overshoot, user decision 2026-10-07), the middle 0.3 s on
 * the air layer (its current layer, read by every hit, contact and targeting rule), touching the ship
 * and the wingman once per leap; it lands, runs on and may leap again its interval after landing.
 */
class RavagerPounceTest {
    /** Its leap in steps, and the air window of 0.3 s in the middle of it. */
    private static final int LEAP = SimStep.ticks(0.75);

    private static final int AIR = SimStep.ticks(0.3);

    /** One Ravager running across the screen {@code depth} px below the top edge, the ship still below it. */
    private static Sortie sortie(EnemySpec ravager, double depth, Rules rules, Loadout loadout) {
        LevelScript level = PartCSpecs.level(
                20,
                5,
                List.of(walkers(
                        0,
                        WaveSpec.Formation.PACK,
                        ravager,
                        1,
                        List.of(List.of(new WaveSpec.At(-40, depth), new WaveSpec.At(PlayField.WIDTH + 200, depth))))),
                List.of());
        return new Sortie(1, loadout, level, rules, TestSpecs.FULL_ARMOUR);
    }

    /** 150 px above the ship's start. */
    private static Sortie above(EnemySpec ravager, Rules rules) {
        return sortie(ravager, PlayField.HEIGHT - Ship.START_Y - 150, rules, TestSpecs.LOADOUT);
    }

    /** Steps until a pounce takes off; returns the steps taken. */
    private static int toTakeOff(Sortie sortie, int most) {
        return toTakeOff(sortie, most, Command.NONE);
    }

    /** Steps with {@code commands} until a pounce takes off; returns the steps taken. */
    private static int toTakeOff(Sortie sortie, int most, int commands) {
        for (int tick = 0; tick < most; tick++) {
            sortie.step(commands);
            if (count(sortie, SimEvents.Type.POUNCE) > 0) {
                return tick;
            }
        }
        throw new AssertionError("no pounce in " + most + " steps");
    }

    @Test
    void itOvershootsTheShipsPositionAtTakeOffAndIsOnAirForTheMiddleOfTheLeap() {
        Sortie sortie = above(ravager(0.3, 3.0), TestSpecs.RULES.withInvulnerableShip());
        toTakeOff(sortie, SimStep.ticks(3));
        Enemy ravager = find(sortie, "ravager");
        assertNotNull(ravager);
        Ship ship = sortie.ship();
        double shipX = ship.x();
        double shipY = ship.y();
        double fromX = ravager.x();
        double fromY = ravager.y();
        assertTrue(Math.hypot(fromX - shipX, fromY - shipY) <= 200);
        assertTrue(ravager.leaping());
        assertEquals(Layer.GROUND, ravager.layer(), "on the ground at take-off");
        assertEquals(1, ravager.leapScale(0));

        List<Layer> layers = new ArrayList<>();
        double apex = 0;
        double closest = Double.MAX_VALUE;
        double closestInAir = Double.MAX_VALUE;
        int landedAt = -1;
        for (int step = 1; step <= LEAP; step++) {
            sortie.step(Command.NONE);
            layers.add(ravager.layer());
            apex = Math.max(apex, ravager.leapScale(1));
            double distance = Math.hypot(ravager.x() - shipX, ravager.y() - shipY);
            closest = Math.min(closest, distance);
            if (ravager.layer() == Layer.AIR) {
                closestInAir = Math.min(closestInAir, distance);
            }
            if (count(sortie, SimEvents.Type.POUNCE_LANDED) > 0) {
                landedAt = step;
            }
        }

        assertEquals(LEAP, landedAt, "it lands after 0.75 s");
        assertFalse(ravager.leaping());
        double leg = Math.hypot(fromX - shipX, fromY - shipY);
        assertTrue(closest <= leg / LEAP * 2, "its middle passes over the ship's position at take-off: " + closest);
        assertEquals(closest, closestInAir, 1e-9, "on air there");
        // Beyond the ship as far again, kept on the play field (here: above the bottom edge).
        assertEquals(Math.clamp(2 * shipX - fromX, 20, PlayField.WIDTH - 20), ravager.x(), 1e-9, "beyond the ship");
        assertEquals(Math.clamp(2 * shipY - fromY, 14, PlayField.HEIGHT - 14), ravager.y(), 1e-9);
        assertTrue(PlayField.overlaps(ravager.x(), ravager.y(), ravager.hitbox()), "it lands on the play field");
        int first = layers.indexOf(Layer.AIR);
        int last = layers.lastIndexOf(Layer.AIR);
        assertEquals(AIR, last - first + 1, "0.3 s on air");
        assertEquals(AIR, layers.stream().filter(Layer.AIR::equals).count(), "in one piece");
        assertEquals(first, LEAP - 1 - last, 1, "in the middle of the leap");
        assertEquals(1.43, apex, 0.01, "drawn up to 1.43× at the apex");
    }

    @Test
    void itLeapsAgainOnlyItsIntervalAfterLanding() {
        // A short interval; the ship follows it to the right, so it is still in range when it is over.
        Sortie sortie = above(ravager(0.3, 0.5), TestSpecs.RULES.withInvulnerableShip());
        int first = toTakeOff(sortie, SimStep.ticks(3));
        int second = first + 1 + toTakeOff(sortie, SimStep.ticks(3), Command.RIGHT.bit());

        assertEquals(first + LEAP + SimStep.ticks(0.5), second, 1);
    }

    @Test
    void aShipThatHoldsStillIsTouchedOnce() {
        // D4's numbers: 0.3 s of the 0.75 s leap on air, its middle over the ship's position.
        Sortie sortie = above(ravager(0.3, 3.0), TestSpecs.RULES);
        toTakeOff(sortie, SimStep.ticks(3));

        assertEquals(1, contacts(sortie, Command.NONE), "one contact");
    }

    @Test
    void aShipThatMovesAwayIsNotTouched() {
        Sortie sortie = above(ravager(0.3, 3.0), TestSpecs.RULES);
        toTakeOff(sortie, SimStep.ticks(3));

        assertEquals(0, contacts(sortie, Command.RIGHT.bit()), "it leaps at where the ship was");
    }

    /** The ship's contacts over the leap and 0.5 s after it, flying {@code commands}. */
    private static int contacts(Sortie sortie, int commands) {
        Defences defences = sortie.ship().defences();
        int hits = 0;
        for (int step = 0; step < LEAP + 30; step++) {
            double before = defences.shield() + defences.armour();
            sortie.step(commands);
            hits += defences.shield() + defences.armour() < before ? 1 : 0;
        }
        return hits;
    }

    @Test
    void itTouchesTheShipOncePerLeap() {
        // Air for the whole leap: it lands on the ship while still on air and stays on it a while.
        Sortie sortie = above(ravager(0.75, 3.0), TestSpecs.RULES);
        toTakeOff(sortie, SimStep.ticks(3));
        Defences defences = sortie.ship().defences();
        int hits = 0;
        double lost = 0;
        for (int step = 0; step < LEAP + 30; step++) {
            double before = defences.shield() + defences.armour();
            sortie.step(Command.NONE);
            double after = defences.shield() + defences.armour();
            if (after < before) {
                hits++;
                lost += before - after;
            }
        }

        assertEquals(1, hits, "one contact");
        assertEquals(15, lost, 1e-9, "`medium` contact");
    }

    @Test
    void itTouchesTheWingmanOncePerLeap() {
        // Rook stays in his wing slot (no flank threat, no look-ahead), 64 px left of and 28 px below
        // the ship. A Ravager running in 80 px below the ship takes off 200 px from it on the line
        // through Rook, air for the whole leap.
        WingmanSpec.Ai still = new WingmanSpec.Ai(
                new WingmanSpec.Offset(64, 28),
                new WingmanSpec.Offset(120, 10),
                new WingmanSpec.Offset(40, 90),
                0.6,
                1.0,
                0,
                0.25,
                0.1,
                0,
                0,
                48,
                1,
                Math.toRadians(15),
                360,
                1.0);
        WingmanSpec rook = new WingmanSpec(WingmanSpec.Side.LEFT, 80, WingmanTest.CRAFT, still, WingmanTest.GUN);
        Sortie sortie = sortie(
                ravager(0.75, 3.0),
                PlayField.HEIGHT - Ship.START_Y + 80,
                TestSpecs.RULES,
                TestSpecs.LOADOUT.withWingman(rook));
        toTakeOff(sortie, SimStep.ticks(3));
        Wingman wingman = sortie.wingman().orElseThrow();
        double start = wingman.armour();
        int hits = 0;
        for (int step = 0; step < LEAP + 30; step++) {
            double before = wingman.armour();
            sortie.step(Command.NONE);
            hits += wingman.armour() < before ? 1 : 0;
        }

        assertEquals(1, hits, "one contact");
        assertEquals(15, start - wingman.armour(), 1e-9);
    }

    @Test
    void aPackStepsDeterministicallyWithoutAllocating() {
        List<WaveSpec> waves = List.of(walkers(
                0,
                WaveSpec.Formation.PACK,
                ravager(0.3, 2.0),
                4,
                List.of(
                        List.of(new WaveSpec.At(-40, 300), new WaveSpec.At(520, 380)),
                        List.of(new WaveSpec.At(-40, 340), new WaveSpec.At(520, 420)),
                        List.of(new WaveSpec.At(520, 320), new WaveSpec.At(-40, 400)),
                        List.of(new WaveSpec.At(520, 360), new WaveSpec.At(-40, 440)))));
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = new Sortie(
                            1,
                            TestSpecs.LOADOUT.withWingman(WingmanTest.rook(WingmanSpec.Side.LEFT)),
                            PartCSpecs.level(20, 30, waves, List.of()),
                            TestSpecs.RULES,
                            TestSpecs.FULL_ARMOUR);
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(8); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "8 s allocated " + allocated + " bytes");
        assertEquals(flown.getFirst().stateHash(), flown.getLast().stateHash());
    }
}
