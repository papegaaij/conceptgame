package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrigTest {
    @Test
    void matchesStrictMathClosely() {
        for (double x = -20; x < 20; x += 0.001) {
            assertEquals(StrictMath.sin(x), Trig.sin(x), 3e-8, "sin " + x);
            assertEquals(StrictMath.cos(x), Trig.cos(x), 3e-8, "cos " + x);
        }
    }
}
