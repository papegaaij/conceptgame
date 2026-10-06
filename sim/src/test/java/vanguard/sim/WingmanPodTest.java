package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Rook's eject pod (design/player/wingmen, Shot down): it drifts to the nearer side edge, unless the
 * player's ship lies on that way; then to the other edge, so it never crosses the ship.
 */
class WingmanPodTest {
    @Test
    void thePodDriftsToTheNearerEdgeWhenTheWayIsClear() {
        assertEquals(-1, Wingman.podSide(200, 68, 264, 96), "left half, the ship to his right");
        assertEquals(1, Wingman.podSide(300, 68, 236, 96), "right half, the ship to his left");
    }

    @Test
    void thePodTakesTheOtherEdgeWhenTheShipLiesOnTheWay() {
        // The round-28 capture: Rook left of the ship (Wing, 28 px below) but in the right half.
        assertEquals(-1, Wingman.podSide(266, 68, 330, 96));
        assertEquals(1, Wingman.podSide(214, 86, 150, 96), "Wide on the right, in the left half");
    }

    @Test
    void aShipWellAboveOrBelowThePodsLineIsNotInTheWay() {
        assertEquals(1, Wingman.podSide(280, 6, 320, 96), "Trail: 90 px below the ship");
        assertEquals(1, Wingman.podSide(280, 96 - Wingman.POD_CLEARANCE, 320, 96), "just clear");
        assertEquals(-1, Wingman.podSide(280, 96 - Wingman.POD_CLEARANCE + 1, 320, 96), "just in the way");
    }

    @Test
    void anEjectionSendsThePodAwayFromTheShip() {
        Wingman rook = new Wingman(WingmanTest.rook(WingmanSpec.Side.LEFT));
        double shipX = 330;
        double shipY = 96;
        rook.reset(10, shipX, shipY);
        double ejectX = rook.x();
        assertTrue(ejectX > PlayField.WIDTH / 2.0 && ejectX < shipX, "on the ship's left, in the right half");
        SimEvents events = new SimEvents(8);

        assertTrue(rook.hit(20, shipX, shipY, events));
        assertEquals(SimEvents.Type.WINGMAN_EJECTED, events.type(events.size() - 1));
        assertEquals(-1, events.value(events.size() - 1), "the event names the side");
        for (int i = 0; i < 60; i++) {
            rook.fly(shipX, shipY, 0, null, null, null, -1);
            double podX = rook.podX(0);
            assertTrue(podX < ejectX, "it drifts left, away from the ship");
            assertTrue(Math.abs(podX - shipX) >= 24 + 8 || Math.abs(rook.podY() - shipY) >= 24 + 8);
        }
    }
}
