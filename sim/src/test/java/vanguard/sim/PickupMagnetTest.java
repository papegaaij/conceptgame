package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.level;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The Pickup magnet (design/player/systems, Utility modules): pickups within its reach of the ship
 * fly at it at its pull speed and are collected; those outside drift on, and nothing but pickups is
 * pulled.
 */
class PickupMagnetTest {
    /** The magnet at L1 (design/player/systems/data.yaml). */
    private static final Magnet L1 = new Magnet(72, 240);

    private static Sortie sortie(LevelScript level, Optional<Magnet> magnet) {
        Loadout loadout = magnet.map(TestSpecs.LOADOUT::withMagnet).orElse(TestSpecs.LOADOUT);
        return new Sortie(1, loadout, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static Sortie emptyLevel(Optional<Magnet> magnet) {
        Sortie sortie = sortie(level(20, List.of()), magnet);
        sortie.step(Command.NONE);
        return sortie;
    }

    @Test
    void aPickupWithinItsReachFliesToTheShipAndIsCollected() {
        Sortie sortie = emptyLevel(Optional.of(L1));
        Ship ship = sortie.ship();
        sortie.drop(PickupType.SMALL_SALVAGE, ship.x() + 60, ship.y());

        sortie.step(Command.NONE);
        Pickup pickup = sortie.pickup(0);
        assertEquals(ship.x() + 60 - 240 * SimStep.SECONDS, pickup.renderX(), 1e-9, "4 px at the ship, sideways");
        assertEquals(ship.y(), pickup.y(), 1e-9, "no drift while pulled");
        assertEquals(ship.x() + 60, pickup.renderX(0), 1e-9, "drawn from where it was");

        int steps = 1;
        while (sortie.pickupCount() > 0 && steps < SimStep.PER_SECOND) {
            sortie.step(Command.NONE);
            steps++;
        }
        assertEquals(0, sortie.pickupCount());
        assertEquals(6, steps, "24 px to the 36 px collection radius at 4 px per step");
        assertEquals(10, sortie.credits());
    }

    @Test
    void withoutAMagnetTheSamePickupDriftsPast() {
        Sortie sortie = emptyLevel(Optional.empty());
        Ship ship = sortie.ship();
        sortie.drop(PickupType.SMALL_SALVAGE, ship.x() + 60, ship.y());

        run(sortie, SimStep.PER_SECOND);

        assertEquals(1, sortie.pickupCount());
        assertEquals(ship.x() + 60, sortie.pickup(0).renderX(), 1e-9);
        assertEquals(0, sortie.credits());
    }

    @Test
    void aPickupOutsideItsReachDriftsOn() {
        Sortie sortie = emptyLevel(Optional.of(L1));
        Ship ship = sortie.ship();
        double x = ship.x() + 73;
        sortie.drop(PickupType.SHIELD_CELL, x, ship.y());

        sortie.step(Command.NONE);
        assertEquals(x, sortie.pickup(0).renderX(), 1e-9);
        assertEquals(ship.y() - 40 * SimStep.SECONDS, sortie.pickup(0).y(), 1e-9, "the normal drift");

        run(sortie, SimStep.PER_SECOND);
        assertEquals(1, sortie.pickupCount());
        assertEquals(x, sortie.pickup(0).renderX(), 1e-9);
    }

    @Test
    void aSecretsHiddenCrateIsAPickupTooAndIsPulledFromEverySide() {
        Sortie sortie = emptyLevel(Optional.of(L1));
        Ship ship = sortie.ship();
        sortie.drop(PickupType.HIDDEN_CRATE, ship.x() - 70, ship.y());
        sortie.drop(PickupType.OVERDRIVE, ship.x(), ship.y() + 70);

        int collected = 0;
        for (int i = 0; i < SimStep.PER_SECOND / 2; i++) {
            sortie.step(Command.NONE);
            collected += sortie.events().count(SimEvents.Type.PICKUP_COLLECTED);
        }

        assertEquals(2, collected);
        assertEquals(0, sortie.pickupCount());
    }

    @Test
    void groundObjectsInItsReachAreNotPulled() {
        // A secret's beacon and a container scroll past the ship's column a little to each side.
        var beacon = new LevelScript.GroundObjectSpec(
                0, Ship.START_X - 30, new Hitbox(12, 12), 0, 0, Optional.empty(), 3, 80, "beacon cache", false);
        var container = new LevelScript.GroundObjectSpec(
                0, Ship.START_X + 30, new Hitbox(32, 24), 3, 5, Optional.of(PickupType.SMALL_SALVAGE), 0, 0, "", false);
        var level = level(20, List.of(), List.of(beacon, container), List.of());
        Sortie pulling = sortie(level, Optional.of(L1));
        Sortie plain = sortie(level, Optional.empty());

        double closest = Double.MAX_VALUE;
        for (int i = 0; i < 8 * SimStep.PER_SECOND; i++) {
            pulling.step(Command.NONE);
            plain.step(Command.NONE);
            assertEquals(plain.groundObjectCount(), pulling.groundObjectCount());
            for (int g = 0; g < plain.groundObjectCount(); g++) {
                GroundObject object = pulling.groundObject(g);
                assertEquals(plain.groundObject(g).x(), object.x(), 0, "step " + i);
                assertEquals(plain.groundObject(g).y(), object.y(), 0, "step " + i);
                closest = Math.min(
                        closest,
                        Math.hypot(
                                object.x() - pulling.ship().x(),
                                object.y() - pulling.ship().y()));
            }
        }

        assertTrue(closest < L1.radius(), "they came within the magnet's reach: " + closest);
        assertEquals(plain.stateHash(), pulling.stateHash(), "with no pickup about, the magnet changes nothing");
    }

    @Test
    void steppingWithAMagnetDoesNotAllocate() {
        long allocated = Allocations.least(
                () -> {
                    var sortie = sortie(SortieTest.mixedLevel(), Optional.of(L1));
                    for (int i = 0; i < 600; i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                    return sortie;
                },
                sortie -> {
                    for (int i = 600; i < 600 + 3600; i++) {
                        if (i % 30 == 0) {
                            Ship ship = sortie.ship();
                            sortie.drop(PickupType.SMALL_SALVAGE, ship.x() + 50, ship.y() + 30);
                        }
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "3600 steps allocated " + allocated + " bytes");
    }

    private static void run(Sortie sortie, int steps) {
        for (int i = 0; i < steps; i++) {
            sortie.step(Command.NONE);
        }
    }
}
