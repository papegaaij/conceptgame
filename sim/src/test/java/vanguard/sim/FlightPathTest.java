package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlightPathTest {
    @Test
    void aStraightPathHasItsLengthAndInterpolates() {
        FlightPath path = FlightPath.through(0, 100, 0, 0);

        assertEquals(100, path.length(), 1e-9);
        int segment = path.segmentAt(25, 0);
        assertEquals(0, path.x(segment, 25), 1e-9);
        assertEquals(75, path.y(segment, 25), 1e-9);
    }

    @Test
    void aCurveRunsFromTheFirstToTheLastControlPoint() {
        FlightPath path = FlightPath.through(0, 0, 100, 100, 200, 0);

        assertEquals(0, path.y(path.segmentAt(0, 0), 0), 1e-9);
        int end = path.segmentAt(path.length(), 0);
        assertEquals(200, path.x(end, path.length()), 1e-9);
        assertTrue(path.length() > 2 * Math.hypot(100, 100) - 1e-9, "the curve is at least as long as the corners");
    }

    @Test
    void passesThroughTheControlPoints() {
        FlightPath path = FlightPath.through(0, 0, 100, 100, 200, 0);
        double peak = 0;
        for (double d = 0; d <= path.length(); d += 0.5) {
            peak = Math.max(peak, path.y(path.segmentAt(d, 0), d));
        }

        assertEquals(100, peak, 0.5);
    }

    @Test
    void needsTwoPoints() {
        assertThrows(IllegalArgumentException.class, () -> FlightPath.through(1, 2));
    }
}
