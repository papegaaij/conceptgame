package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefencesTest {
    private final Defences defences = new Defences(TestSpecs.LOADOUT.shield(), TestSpecs.LOADOUT.plating(), 0.25);
    private final SimEvents events = new SimEvents(16);

    @Test
    void startsWithTheStarterShieldAndPlating() {
        assertEquals(20, defences.shield());
        assertEquals(60, defences.armour());
    }

    @Test
    void shotsHitTheShieldFirst() {
        defences.takeShot(6, events, 0, 0);

        assertEquals(14, defences.shield());
        assertEquals(60, defences.armour());
        assertEquals(1, events.size());
        assertEquals(SimEvents.Type.SHIELD_HIT, events.type(0));
    }

    @Test
    void overflowGoesToArmourAndBreaksTheShield() {
        defences.takeShot(18, events, 0, 0);
        defences.takeShot(6, events, 0, 0);

        assertEquals(0, defences.shield());
        assertEquals(56, defences.armour());
        assertTrue(defences.broken());
        assertEquals(SimEvents.Type.SHIELD_BROKEN, events.type(2));
        assertEquals(SimEvents.Type.ARMOUR_HIT, events.type(3));
    }

    @Test
    void collisionsSplitTheDamageBetweenShieldAndArmour() {
        defences.takeCollision(6, events, 0, 0);

        assertEquals(17, defences.shield());
        assertEquals(57, defences.armour());
    }

    @Test
    void armourDamageGivesMercyInvulnerability() {
        defences.takeCollision(6, events, 0, 0);
        step(14);
        defences.takeCollision(6, events, 0, 0);
        assertEquals(57, defences.armour(), "ignored during the 0.25 s mercy time");

        step(1);
        defences.takeCollision(6, events, 0, 0);

        assertEquals(54, defences.armour());
    }

    @Test
    void shieldHitsGiveNoMercy() {
        defences.takeShot(4, events, 0, 0);
        defences.takeShot(4, events, 0, 0);

        assertEquals(12, defences.shield());
    }

    @Test
    void regeneratesTwoPointsPerSecondAfterTwoSecondsWithoutHits() {
        defences.takeShot(10, events, 0, 0);
        step(120);
        assertEquals(10, defences.shield(), "nothing during the 2.0 s delay");

        step(60);

        assertEquals(12, defences.shield(), 1e-9);
    }

    @Test
    void everyHitRestartsTheDelay() {
        defences.takeShot(4, events, 0, 0);
        step(100);
        defences.takeShot(4, events, 0, 0);
        step(100);

        assertEquals(12, defences.shield());
    }

    @Test
    void aBrokenShieldStaysDownOneSecondLonger() {
        defences.takeShot(20, events, 0, 0);
        step(180);
        assertEquals(0, defences.shield(), "nothing during break time plus delay");
        assertTrue(defences.broken());

        step(1);

        assertTrue(defences.shield() > 0);
        assertFalse(defences.broken());
    }

    @Test
    void reportsDestructionWhenArmourReachesZero() {
        assertFalse(defences.takeShot(79, events, 0, 0));
        step(15);

        assertTrue(defences.takeShot(1, events, 0, 0));
        assertEquals(0, defences.armour());
    }

    @Test
    void restoreRefillsTheShieldAndSetsTheArmour() {
        defences.takeShot(50, events, 0, 0);

        defences.restore(45);

        assertEquals(20, defences.shield());
        assertEquals(45, defences.armour());
        assertEquals(0, defences.armourLost());
        assertEquals(0, defences.mercyTicks());
        assertThrows(IllegalArgumentException.class, () -> defences.restore(0));
        assertThrows(IllegalArgumentException.class, () -> defences.restore(61));
    }

    @Test
    void aShieldCellAndAnArmourPatchRefillUpToTheMaximum() {
        defences.takeShot(30, events, 0, 0);

        defences.restoreShield(5);
        defences.repair(10);

        assertEquals(5, defences.shield());
        assertEquals(60, defences.armour(), "10 lost, 10 repaired");
        defences.repair(10);
        assertEquals(60, defences.armour());
        assertEquals(10, defences.armourLost(), "repairs do not undo the damage taken");
    }

    private void step(int steps) {
        for (int i = 0; i < steps; i++) {
            defences.step();
        }
    }
}
