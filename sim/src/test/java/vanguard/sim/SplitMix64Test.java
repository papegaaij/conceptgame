package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SplitMix64Test {
    @Test
    void producesTheReferenceSequence() {
        // First outputs of the reference implementation (Vigna, splitmix64.c) for seed 1234567.
        var random = new SplitMix64(1234567);
        assertEquals(6457827717110365317L, random.nextLong());
        assertEquals(3203168211198807973L, random.nextLong());
        assertEquals(Long.parseUnsignedLong("9817491932198370423"), random.nextLong());
    }

    @Test
    void derivedValuesStayInTheirRanges() {
        var random = new SplitMix64(42);
        for (int i = 0; i < 10_000; i++) {
            double unit = random.nextDouble();
            assertTrue(unit >= 0 && unit < 1, "nextDouble " + unit);
            double ranged = random.range(-3, 5);
            assertTrue(ranged >= -3 && ranged < 5, "range " + ranged);
            int bounded = random.nextInt(7);
            assertTrue(bounded >= 0 && bounded < 7, "nextInt " + bounded);
        }
    }
}
