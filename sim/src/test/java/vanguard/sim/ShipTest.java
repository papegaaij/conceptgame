package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipTest {
    private static final int LEFT = Command.LEFT.bit();
    private static final int RIGHT = Command.RIGHT.bit();
    private static final int UP = Command.UP.bit();
    private static final int PRECISION = Command.PRECISION.bit();

    private final Ship ship = new Ship(
            TestSpecs.SHIP,
            new Defences(TestSpecs.LOADOUT.shield(), TestSpecs.LOADOUT.plating(), TestSpecs.SHIP.mercySeconds()));

    @Test
    void reachesFullSpeedWithinAFewSteps() {
        fly(RIGHT, 4);
        assertTrue(ship.vx() < 270, "not yet at full speed after 4 steps");

        fly(RIGHT, 1);

        assertEquals(270, ship.vx(), 1e-9, "full speed after 5 steps (0.08 s rounded up)");
    }

    @Test
    void stopsWithinFourSteps() {
        fly(RIGHT, 10);

        fly(Command.NONE, 3);
        assertTrue(ship.vx() > 0, "still moving after 3 steps");
        fly(Command.NONE, 1);

        assertEquals(0, ship.vx(), "stopped after 4 steps (0.06 s rounded up)");
    }

    @Test
    void precisionModeHalvesTheSpeed() {
        fly(RIGHT | PRECISION, 20);

        assertEquals(135, ship.vx(), 1e-9);
    }

    @Test
    void diagonalsAreNotFaster() {
        fly(RIGHT | UP, 20);

        assertEquals(270, Math.hypot(ship.vx(), ship.vy()), 1e-9);
    }

    @Test
    void keepsTheWholeHullTwelvePixelsInsideThePlayField() {
        double limit = 48 / 2.0 + 12;
        fly(LEFT, 120);

        assertEquals(limit, ship.x());
        assertEquals(0, ship.vx(), "pressing against the edge does not keep the speed");

        fly(RIGHT | UP, 240);

        assertEquals(PlayField.WIDTH - limit, ship.x());
        assertEquals(PlayField.HEIGHT - limit, ship.y());
    }

    @Test
    void banksOneFrameEveryThreeStepsAndLevelsOutWhenReleased() {
        fly(LEFT, 3);
        assertEquals(0, ship.bank());
        fly(LEFT, 1);
        assertEquals(-1, ship.bank());
        fly(LEFT, 3);
        assertEquals(-2, ship.bank(), "hard left after about 6 steps of steering");

        fly(Command.NONE, 6);

        assertEquals(0, ship.bank());
    }

    @Test
    void banksHalfWayInPrecisionMode() {
        fly(RIGHT | PRECISION, 30);

        assertEquals(1, ship.bank());
    }

    private void fly(int commands, int steps) {
        for (int i = 0; i < steps; i++) {
            ship.rememberPosition();
            ship.fly(commands);
        }
    }
}
