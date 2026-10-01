package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LayerTest {
    @Test
    void standardShotsHitAirLowAirAndGroundButNotHighAir() {
        assertTrue(Layer.AIR.hitByStandardShots());
        assertTrue(Layer.LOW_AIR.hitByStandardShots());
        assertTrue(Layer.GROUND.hitByStandardShots());
        assertFalse(Layer.HIGH_AIR.hitByStandardShots());
    }

    @Test
    void onlyThePlayersPlaneCollides() {
        assertTrue(Layer.AIR.collidesWithPlayer());
        assertFalse(Layer.LOW_AIR.collidesWithPlayer());
        assertFalse(Layer.GROUND.collidesWithPlayer());
        assertFalse(Layer.HIGH_AIR.collidesWithPlayer());
    }
}
