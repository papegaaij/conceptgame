package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part F, Level 12's sondes and pickups (design/campaign Level 12, user decisions F2 = a and F7 =
 * b): the sondes drift down on the play plane, unpushed, and are collected within the radius of the
 * ship's centre; the {@code collect: sondes} secondary is met at the last and failed at one lost; the
 * level's drop factor, a placed pickup and the drifting canister on {@code low-air}.
 */
class SondeTest {
    private static final LevelScript.Secondary COLLECT = LevelScript.Secondary.sondes(75, "SONDES");

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static LevelScript sondes(double... xs) {
        List<LevelScript.Sonde> at = new java.util.ArrayList<>();
        for (int i = 0; i < xs.length; i++) {
            at.add(new LevelScript.Sonde(1 + i, xs[i]));
        }
        return TestSpecs.level(30, List.of()).withSondes(new LevelScript.Sondes(60, 36, at), COLLECT);
    }

    @Test
    void sondesDriftDownUnpushedAndTheShipCollectsThemWithinTheRadius() {
        LevelScript level = sondes(Ship.START_X, Ship.START_X)
                .withWeather(WindTest.storm(List.of(), List.of(new LevelScript.Drift(0, 30, 40, true)), List.of()));
        Sortie sortie = sortie(level);
        Sondes sondes = sortie.sondes();
        int collected = 0;
        boolean met = false;
        for (int i = 0; i < SimStep.ticks(20); i++) {
            // Holding the ship against the wind, in the sondes' lane.
            double dx = sortie.ship().x() - Ship.START_X;
            sortie.step(dx > 1 ? Command.of(Command.LEFT) : Command.NONE);
            if (sondes.state(0) == Sondes.DRIFTING) {
                assertEquals(Ship.START_X, sondes.x(0), "its x held, the wind does not push it");
            }
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.SONDE_COLLECTED) {
                    collected++;
                    assertEquals(collected, events.value(e));
                }
            }
            met |= sortie.events().count(SimEvents.Type.OBJECTIVE_MET) > 0;
        }
        assertEquals(2, collected);
        assertEquals(2, sondes.collected());
        assertTrue(met && sortie.secondaryMet());
        assertTrue(sortie.secondaryByEscapes());
        assertEquals(2, sortie.escapesDestroyed());
        assertEquals(2, sortie.escapesTotal());
    }

    @Test
    void aSondeLeavingTheBottomEdgeFailsTheObjective() {
        Sortie sortie = sortie(sondes(Ship.START_X, 40));
        boolean failed = false;
        for (int i = 0; i < SimStep.ticks(20); i++) {
            sortie.step(Command.NONE);
            failed |= sortie.events().count(SimEvents.Type.OBJECTIVE_FAILED) > 0;
        }
        assertTrue(failed);
        assertTrue(sortie.secondaryFailed());
        assertFalse(sortie.secondaryMet());
        assertEquals(Sondes.LOST, sortie.sondes().state(1));
        assertEquals(1, sortie.sondes().collected());
    }

    @Test
    void aRetryBringsTheSondesBack() {
        Sortie sortie = sortie(sondes(Ship.START_X));
        for (int i = 0; i < SimStep.ticks(12); i++) {
            sortie.step(Command.NONE);
        }
        assertEquals(1, sortie.sondes().collected());
        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(Command.NONE);
        assertEquals(0, sortie.sondes().collected());
        assertEquals(Sondes.WAITING, sortie.sondes().state(0));
    }

    @Test
    void theDropFactorDividesTheDropCounter() {
        assertEquals(1, shieldCells(1.5), "every 4th kill / 1.5 = every 3rd");
        assertEquals(0, shieldCells(1));
    }

    /** Destroys three Needlers; returns the shield cells dropped. */
    private static int shieldCells(double factor) {
        WaveSpec line = TestSpecs.wave(
                0, WaveSpec.Formation.LINE_ABREAST, TestSpecs.NEEDLER, 3, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Sortie sortie = sortie(TestSpecs.level(30, List.of(line)).withPickups(factor, List.of()));
        sortie.step(Command.NONE);
        assertEquals(3, sortie.enemyCount());
        for (int k = 0; k < 3; k++) {
            sortie.destroyEnemy(0);
        }
        int cells = 0;
        for (int i = 0; i < sortie.pickupCount(); i++) {
            cells += sortie.pickup(i).type() == PickupType.SHIELD_CELL ? 1 : 0;
        }
        return cells;
    }

    @Test
    void aPlacedPickupEntersAtTheTopEdgeAtItsTime() {
        LevelScript level = TestSpecs.level(30, List.of())
                .withPickups(1, List.of(new LevelScript.PlacedPickup(1, 100, PickupType.ARMOUR_PATCH)));
        Sortie sortie = sortie(level);
        for (int i = 0; i < SimStep.ticks(1) - 1; i++) {
            sortie.step(Command.NONE);
            assertEquals(0, sortie.pickupCount());
        }
        sortie.step(Command.NONE);
        assertEquals(1, sortie.pickupCount());
        assertEquals(PickupType.ARMOUR_PATCH, sortie.pickup(0).type());
        assertEquals(100, sortie.pickup(0).x());
        assertTrue(sortie.pickup(0).y() > PlayField.HEIGHT - 2);
    }

    @Test
    void theDriftingCanisterDriftsInScreenSpaceAndTakesItsHits() {
        LevelScript.GroundObjectSpec canister = new LevelScript.GroundObjectSpec(
                        0, Ship.START_X, new Hitbox(24, 24), 0, 0, Optional.empty(), 3, 130, "canister", false)
                .drifting(-10, -40);
        LevelScript level = TestSpecs.level(30, List.of(), List.of(canister), List.of())
                .withWeather(WindTest.storm(List.of(), List.of(new LevelScript.Drift(0, 30, 40, true)), List.of()));
        Sortie sortie = sortie(level);
        sortie.step(Command.NONE);
        GroundObject object = sortie.groundObject(0);
        double x = object.x();
        double y = object.y();
        sortie.step(Command.NONE);
        assertEquals(-10 * SimStep.SECONDS, object.x() - x, 1e-9, "its own drift, no wind");
        assertEquals(-40 * SimStep.SECONDS, object.y() - y, 1e-9, "not the scroll's 130 px/s");
        boolean found = false;
        for (int i = 0; i < SimStep.ticks(10) && !found; i++) {
            double dx = object.x() - sortie.ship().x();
            int steer = dx < -2
                    ? Command.of(Command.LEFT, Command.FIRE)
                    : dx > 2 ? Command.of(Command.RIGHT, Command.FIRE) : Command.of(Command.FIRE);
            sortie.step(steer);
            found = sortie.events().count(SimEvents.Type.SECRET_FOUND) > 0;
        }
        assertTrue(found, "three hits broke it open");
    }

    @Test
    void sondesDoNotAllocate() {
        LevelScript level = sondes(Ship.START_X, 100, 300);
        long allocated = Allocations.least(() -> sortie(level), sortie -> {
            for (int i = 0; i < 900; i++) {
                sortie.step(Command.NONE);
            }
        });
        assertEquals(0, allocated);
    }
}
