package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitboxTest {
    private final Hitbox ship = new Hitbox(9, 9);
    private final Hitbox skitter = new Hitbox(16, 16);

    @Test
    void overlapsWhenTheBoxesIntersect() {
        assertTrue(ship.overlaps(100, 100, skitter, 112, 100));
        assertTrue(ship.overlaps(100, 100, skitter, 100, 88));
    }

    @Test
    void touchingEdgesDoNotOverlap() {
        assertFalse(ship.overlaps(100, 100, skitter, 112.5, 100));
        assertFalse(ship.overlaps(100, 100, skitter, 110, 112.5));
    }
}
