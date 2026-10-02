package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HullTest {
    private final Hull hull = TestSpecs.HULL;
    private final Hitbox thorn = new Hitbox(6, 6);

    @Test
    void aBulletCrossingAWingHits() {
        assertTrue(hull.overlaps(100, 100, thorn, 100 - 16, 100 - 3), "left wing, near the tip");
        assertTrue(hull.overlaps(100, 100, thorn, 100 + 14, 100 - 6), "right trailing edge");
    }

    @Test
    void aBulletOnTheNoseOrTailHits() {
        assertTrue(hull.overlaps(100, 100, thorn, 100, 100 + 21), "nose tip");
        assertTrue(hull.overlaps(100, 100, thorn, 100, 100 - 22), "engines");
    }

    @Test
    void aBulletBesideTheNoseInFrontOfTheWingsMisses() {
        // Between the nose and the wingtips the sprite is empty space.
        assertFalse(hull.overlaps(100, 100, thorn, 100 + 9, 100 + 12));
    }

    @Test
    void aBulletPastTheWingtipsMisses() {
        assertFalse(hull.overlaps(100, 100, thorn, 100 + 22, 100));
    }
}
