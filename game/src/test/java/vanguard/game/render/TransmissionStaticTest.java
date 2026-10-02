package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransmissionStaticTest {
    private static final float NEVER = Float.POSITIVE_INFINITY;

    @Test
    void theStaticFadesOutAfterOpeningAndInBeforeClosing() {
        float half = TransmissionStatic.SECONDS / 2;

        assertEquals(1, TransmissionStatic.strength(0, NEVER));
        assertEquals(0.5f, TransmissionStatic.strength(half, NEVER), 1e-6f);
        assertEquals(0, TransmissionStatic.strength(TransmissionStatic.SECONDS, NEVER));
        assertEquals(0.5f, TransmissionStatic.strength(10, half), 1e-6f);
        assertEquals(1, TransmissionStatic.strength(10, 0));
    }
}
