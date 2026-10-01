package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class StateHashTest {
    @Test
    void startsAtTheFnvOffsetBasis() {
        assertEquals(0xCBF29CE484222325L, new StateHash().value());
    }

    @Test
    void dependsOnTheOrderOfTheValues() {
        assertNotEquals(
                new StateHash().add(1L).add(2L).value(),
                new StateHash().add(2L).add(1L).value());
    }

    @Test
    void hashesDoublesByTheirBitPattern() {
        assertNotEquals(
                new StateHash().add(0.0).value(), new StateHash().add(-0.0).value());
        assertEquals(
                new StateHash().add(Double.doubleToLongBits(1.5)).value(),
                new StateHash().add(1.5).value());
    }
}
