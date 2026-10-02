package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnemyLooksTest {
    private static final double STEP = 2 * Math.PI / 16;

    @Test
    void theNearestHeadingIsShownClockwiseFromDown() {
        assertEquals(0, EnemyLooks.heading(0, 16), "down");
        assertEquals(4, EnemyLooks.heading(Math.PI / 2, 16), "left");
        assertEquals(8, EnemyLooks.heading(-Math.PI, 16), "up");
        assertEquals(12, EnemyLooks.heading(-Math.PI / 2, 16), "right");
        assertEquals(1, EnemyLooks.heading(0.6 * STEP, 16));
        assertEquals(0, EnemyLooks.heading(-0.4 * STEP, 16));
        assertEquals(15, EnemyLooks.heading(-0.6 * STEP, 16));
    }

    @Test
    void aFixedUnitHasOneHeading() {
        assertEquals(0, EnemyLooks.heading(2.0, 1));
    }
}
