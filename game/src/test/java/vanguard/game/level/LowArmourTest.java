package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LowArmourTest {
    @Test
    void theWarningsStartAtThirtyAndFifteenPercent() {
        assertEquals(LowArmour.NONE, LowArmour.of(60, 60));
        assertEquals(LowArmour.NONE, LowArmour.of(18.5, 60));
        assertEquals(LowArmour.LOW, LowArmour.of(18, 60));
        assertEquals(LowArmour.LOW, LowArmour.of(9.5, 60));
        assertEquals(LowArmour.CRITICAL, LowArmour.of(9, 60));
        assertEquals(LowArmour.CRITICAL, LowArmour.of(0, 60));
    }

    @Test
    void theStagesFollowTheShareOfTheMaxArmour() {
        assertEquals(LowArmour.NONE, LowArmour.of(30, 80));
        assertEquals(LowArmour.LOW, LowArmour.of(24, 80));
        assertEquals(LowArmour.CRITICAL, LowArmour.of(12, 80));
    }
}
