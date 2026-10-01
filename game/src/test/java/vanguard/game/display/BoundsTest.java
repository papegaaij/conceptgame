package vanguard.game.display;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoundsTest {
    private static final Bounds SECOND_MONITOR = new Bounds(3840, 0, 3840, 2160);

    @Test
    void containsAWindowWhoseCentreIsInside() {
        assertTrue(SECOND_MONITOR.containsCentreOf(new Bounds(3000, 100, 1920, 1080)));
    }

    @Test
    void doesNotContainAWindowWhoseCentreIsOutside() {
        assertFalse(SECOND_MONITOR.containsCentreOf(new Bounds(2000, 100, 1920, 1080)));
        assertFalse(SECOND_MONITOR.containsCentreOf(new Bounds(4000, 2000, 1920, 1080)));
    }
}
