package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BackdropDataTest {
    private static final LevelData LEVEL_01 = ContentLoader.fromClasspath().level(Level01Test.LEVEL);

    private static final BackdropData.PlacedPiece FLIGHT = new BackdropData.PlacedPiece(
            "stormhawk-far",
            2,
            390,
            Optional.empty(),
            Optional.of(List.of(
                    new BackdropData.Waypoint(1, 0, 0),
                    new BackdropData.Waypoint(3, 0, 100),
                    new BackdropData.Waypoint(5, 100, 100))));

    @Test
    void aPieceRestsAtItsFirstWaypointUntilThePathStarts() {
        assertEquals(0, FLIGHT.offsetY(0));
        assertEquals(0, FLIGHT.heading(0));
    }

    @Test
    void aPieceMovesBetweenWaypointsAndHeadsAlongThePath() {
        assertEquals(50, FLIGHT.offsetY(2));
        assertEquals(50, FLIGHT.offsetX(4));
        assertEquals(90, FLIGHT.heading(4));
    }

    @Test
    void aPieceStaysAtItsLastWaypoint() {
        assertEquals(100, FLIGHT.offsetX(9));
        assertEquals(100, FLIGHT.offsetY(9));
        assertEquals(90, FLIGHT.heading(9));
    }

    @Test
    void theScrollFollowsTheSectionSpeeds() {
        assertEquals(130 * 75.5, LEVEL_01.scrollAt(75.5), 1e-9);
        assertEquals(130 * 200, LEVEL_01.scrollAt(200), 1e-9);
    }

    @Test
    void aSectionsTileSetEntersAtTheTopEdgeWhenTheSectionStarts() {
        double seam = LEVEL_01.seam(BackdropLayer.FAR, 2);
        double layerScroll = LEVEL_01.scrollAt(60) * 0.45;

        assertEquals(540, seam - layerScroll, 1e-9);
    }

    @Test
    void aPlacedPiecePassesTheMiddleOfTheScreenAtItsTime() {
        BackdropData.PlacedPiece hull = LEVEL_01.backdrop().placed().stream()
                .filter(p -> p.piece().equals("cruiser-hull"))
                .findFirst()
                .orElseThrow();

        assertEquals(270, LEVEL_01.pieceCentre(hull) - LEVEL_01.scrollAt(hull.t()), 1e-9);
    }
}
