package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Coilwyrm's chained bursts rise in pitch down the taper (design/enemies/air/coilwyrm). */
class RipplePitchTest {
    @Test
    void theBurstsRiseAsTheMembersNarrowAndStopAtTheirMost() {
        assertEquals(0.94f, FlightSounds.ripplePitch(54 * 0.7), 1e-4, "the first segment");
        assertEquals(1.2f, FlightSounds.ripplePitch(27 * 0.7), 0.005, "the last segment");
        assertTrue(FlightSounds.ripplePitch(28) > FlightSounds.ripplePitch(37.8), "the tail in between");
        assertTrue(FlightSounds.ripplePitch(28) < FlightSounds.ripplePitch(18.9));
        assertEquals(1.2f, FlightSounds.ripplePitch(5), 1e-6, "capped");
    }
}
