package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A tow's pod after the cut (part G, Level 07's lifeboat; user decision 2026-10-05): it comes loose
 * and falls away from the boat on its own, and the crate falls out of it at {@link Tow#RELEASE_Y},
 * or at once when the pod is cut lower; a boss checkpoint brings both back as they were.
 */
class TowTest {
    /** Level 07's lifeboat: from x 130 at t 22, drifting (12, -45) px/s, the pod 70 px behind the boat. */
    private static final LevelScript.TowSpec LIFEBOAT = new LevelScript.TowSpec(
            22, 130, 12, -45, new Hitbox(72, 36), new Hitbox(32, 32), 0, 70, new Hitbox(16, 40), 3, 75, "lifeboat tow");

    private static int tick(double seconds) {
        return SimStep.ticks(seconds);
    }

    private static Tow cutAt(double seconds) {
        Tow tow = new Tow(LIFEBOAT);
        tow.update(tick(seconds));
        assertFalse(tow.countHit(tick(seconds)));
        assertFalse(tow.countHit(tick(seconds)));
        assertTrue(tow.countHit(tick(seconds)), "the third hit cuts the cable");
        return tow;
    }

    @Test
    void theLoosePodFallsAwayFromTheBoatAndDropsTheCrateLow() {
        Tow tow = cutAt(24);
        assertFalse(tow.holding());
        assertTrue(tow.crateDue());
        assertTrue(tow.podY() > Tow.RELEASE_Y, "cut high up");
        assertFalse(tow.releaseCrate(), "not up there");
        double boatY = tow.y();
        int t = tick(24);
        int released = -1;
        double lastY = tow.podY();
        double lastX = tow.podX();
        while (tow.present() && t < tick(60)) {
            tow.update(++t);
            assertTrue(tow.podY() < lastY, "it falls");
            assertTrue(tow.podX() > lastX, "it slips aside towards the middle (and drifts on)");
            lastY = tow.podY();
            lastX = tow.podX();
            if (tow.releaseCrate()) {
                assertEquals(-1, released, "the crate falls out once");
                released = t;
                assertTrue(tow.podY() <= Tow.RELEASE_Y && tow.podY() > Tow.RELEASE_Y - 10);
            }
        }
        assertTrue(released > 0, "the crate fell out");
        double seconds = (released - tick(24)) * SimStep.SECONDS;
        assertTrue(seconds < 3, "within " + seconds + " s of the cut");
        assertTrue(tow.y() > boatY - 45 * 36, "the boat drifts on at its own pace");
        assertFalse(tow.crateDue());
        assertFalse(tow.releaseCrate());
    }

    @Test
    void aPodCutDownAtTheShipDropsTheCrateAtOnce() {
        // the boat at the bottom edge after about 12.8 s, the pod 70 px above it
        Tow tow = cutAt(22 + 11);
        assertTrue(tow.podY() <= Tow.RELEASE_Y, "cut low: " + tow.podY());
        assertTrue(tow.releaseCrate());
    }

    @Test
    void theHoldingPodHangsOnTheCable() {
        Tow tow = new Tow(LIFEBOAT);
        tow.update(tick(26));
        assertEquals(tow.x(), tow.podX(), 1e-9);
        assertEquals(tow.y() + 70, tow.podY(), 1e-9);
        assertFalse(tow.crateDue());
        assertFalse(tow.releaseCrate());
        assertEquals(0, tow.looseSeconds(1), 1e-9);
    }

    @Test
    void aCheckpointRestoresTheLoosePodAndItsCrate() {
        Tow tow = cutAt(24);
        tow.update(tick(25));
        Tow restored = new Tow(LIFEBOAT);
        restored.restore(tow.hitsLeft(), tow.cutTick(), tow.crateDue(), tick(25));
        assertEquals(tow.podX(), restored.podX(), 1e-9);
        assertEquals(tow.podY(), restored.podY(), 1e-9);
        assertTrue(restored.crateDue());
        StateHash a = new StateHash();
        StateHash b = new StateHash();
        tow.addTo(a);
        restored.addTo(b);
        assertEquals(a.value(), b.value());
    }
}
