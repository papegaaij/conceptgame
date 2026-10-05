package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.sim.Command;
import vanguard.sim.Loadout;
import vanguard.sim.Pickup;
import vanguard.sim.PickupType;
import vanguard.sim.Sortie;
import vanguard.sim.Tow;

/**
 * Level 07's lifeboat tow after the cut (design/campaign/act-1-first-contact/level-07-brood-carrier,
 * Secrets; user decision 2026-10-05): the pod comes loose and falls away from the boat on its own,
 * the crate falls out of it down in the ship's part of the screen and is an ordinary hidden crate
 * from there, so a ship that stays low collects it.
 */
class LifeboatTowTest {
    private static final int MAX_STEPS = 60 * 60;
    /** The ship's height while it stays low, px above the bottom edge. */
    private static final double LOW = 60;

    private final Content content = ContentLoader.fromClasspath();

    @Test
    void theLoosePodFallsAwayAndALowShipCollectsTheCrate() {
        Loadout loadout = Level07Test.planLoadout(content, Difficulty.MEDIUM);
        Sortie sortie = new Sortie(
                7,
                loadout,
                SimSpecs.level(content, Level07Test.LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, Level07Test.LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                loadout.plating().maxArmour());
        Tow tow = sortie.tow(0);
        double cutPodY = -1;
        double cutBoatY = -1;
        double lastPodY = Double.MAX_VALUE;
        double crateX = -1;
        double crateY = -1;
        int crateTicks = -1;
        boolean collected = false;
        int steps = 0;
        while (steps++ < MAX_STEPS && !collected) {
            boolean crate = false;
            double targetX;
            if (tow.holding()) {
                targetX = tow.present() ? tow.cableX() : tow.spec().x();
            } else {
                Pickup found = crate(sortie);
                crate = found != null;
                targetX = crate ? found.renderX() : tow.podX();
            }
            sortie.step(steer(sortie, targetX, tow.holding() ? Command.FIRE.bit() : 0));
            if (cutPodY < 0 && !tow.holding()) {
                cutPodY = tow.podY();
                cutBoatY = tow.y();
                assertTrue(tow.crateDue() || crate(sortie) != null, "the crate is in the pod or out of it");
            }
            if (cutPodY >= 0 && tow.present() && tow.podY() > -tow.spec().pod().height()) {
                assertTrue(tow.podY() < lastPodY, "the loose pod falls");
                lastPodY = tow.podY();
            }
            Pickup found = crate(sortie);
            if (found != null && crateY < 0) {
                crateX = found.renderX();
                crateY = found.renderY(1);
                crateTicks = found.ticksLeft();
                assertTrue(crateY <= 160 + 1e-9, "the crate falls out low: " + crateY);
                assertFalse(tow.crateDue());
            }
            if (crateY >= 0 && found == null) {
                collected = sortie.result().credits().secrets() > 0;
                assertTrue(collected, "the crate was collected, not lost");
            }
            assertTrue(Math.abs(sortie.ship().y() - LOW) < 40 || sortie.levelSeconds() < 5, "the ship stays low");
        }
        System.out.printf(
                "Level 07 lifeboat: cut with the pod at y %.0f (boat %.0f), crate out at (%.0f, %.0f) with %d ticks,"
                        + " collected at t %.1f%n",
                cutPodY, cutBoatY, crateX, crateY, crateTicks, sortie.levelSeconds());
        assertTrue(cutPodY > 160, "cut while the pod is still high above the ship");
        assertTrue(collected, "a ship that stays low collects the crate");
        assertEquals(1, sortie.result().secretsFound());
    }

    private static Pickup crate(Sortie sortie) {
        for (int i = 0; i < sortie.pickupCount(); i++) {
            if (sortie.pickup(i).type() == PickupType.HIDDEN_CRATE) {
                return sortie.pickup(i);
            }
        }
        return null;
    }

    /** Steers the ship to {@code targetX} at the low height, with {@code fire}. */
    private static int steer(Sortie sortie, double targetX, int fire) {
        int commands = fire;
        if (sortie.ship().x() < targetX - 3) {
            commands |= Command.RIGHT.bit();
        } else if (sortie.ship().x() > targetX + 3) {
            commands |= Command.LEFT.bit();
        }
        if (sortie.ship().y() < LOW - 3) {
            commands |= Command.UP.bit();
        } else if (sortie.ship().y() > LOW + 3) {
            commands |= Command.DOWN.bit();
        }
        return commands;
    }
}
