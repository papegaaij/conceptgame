package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class BackdropDataTest {
    private static final LevelData LEVEL_01 = ContentLoader.fromClasspath().level(Level01Test.LEVEL);
    private static final String LEVEL_01_FILE = "campaign/" + Level01Test.LEVEL + "/data.yaml";
    /** A tower piece and its placement, added to Level 01's backdrop by the schema tests. */
    private static final String PLATFORM = "    platform: {layer: ground, size: [140, 140], mid_size: true}\n";

    private static final String CRANE = "    - {piece: crossbeam, t: 62.08, x: 240}\n";

    private static final BackdropData.PlacedPiece FLIGHT = new BackdropData.PlacedPiece(
            "stormhawk-far",
            2,
            390,
            Optional.empty(),
            Optional.of(List.of(
                    new BackdropData.Waypoint(1, 0, 0),
                    new BackdropData.Waypoint(3, 0, 100),
                    new BackdropData.Waypoint(5, 100, 100))),
            Optional.empty(),
            Optional.empty());

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

    @Test
    void aTowerIsAGroundPieceWithAHeightAWallAndAShade() {
        LevelData level = withTower("tower: {height: 1.35, wall: wall-glass}", "t: 75, x: 400");
        BackdropData.Piece piece = level.backdrop().pieces().get("tower-a");
        BackdropData.Tower tower = piece.tower().orElseThrow();

        assertEquals(1.35, tower.height());
        assertEquals("wall-glass", tower.wall());
        assertEquals(0.5, tower.shadeOrDefault());
        assertEquals(6 / 4.65, tower.scale(), 1e-12);
        // The roof's image is the footprint at the roof's scale: 80 × 1.29 by 60 × 1.29.
        assertEquals(103, piece.imageWidth());
        assertEquals(77, piece.imageHeight());
    }

    @Test
    void theTallestTowerIsDrawnAThirdLarger() {
        assertEquals(4 / 3.0, tower(1.5).scale(), 1e-12);
        assertEquals(1, BackdropData.Tower.scaleAt(0));
        assertRejected(() -> tower(1.6), "tower.height must be > 0 and <= 1.5");
        assertRejected(() -> tower(0), "tower.height must be > 0 and <= 1.5");
    }

    @Test
    void aTowersRoofSlidesOverTheGroundAtKMinusOneTimesTheScroll() {
        // h = 1.35: k = 1.29, so at Level 01's 130 px/s the roof moves 37 px/s on its own.
        assertEquals(130 * (6 / 4.65 - 1), tower(1.35).ownSpeed(130), 1e-9);
        // The tallest roof at the fastest planned scroll (160 px/s) stays well inside 120 px/s.
        assertEquals(160 / 3.0, tower(1.5).ownSpeed(160), 1e-9);
    }

    @Test
    void aRoofSlidingFasterThanTheMotionBudgetIsRejected() {
        assertLoadProblem(
                text -> towerText(text, "tower: {height: 1.5, wall: wall-glass}", "t: 75, x: 400")
                        .replace("scroll_speed: 130", "scroll_speed: 400"),
                "backdrop.placed[11]: t=",
                "the roof of 'tower-a' slides 133 px/s over the ground, more than 120");
    }

    @Test
    void aMirroredTowerIsRejected() {
        assertLoadProblem(
                text -> towerText(text, "tower: {height: 1, wall: wall-glass}", "t: 75, x: 400, mirror: true"),
                "backdrop.placed[11].mirror: a tower is not mirrored");
    }

    @Test
    void aWallTextureIsAnImageOfItsOwn() {
        assertLoadProblem(
                text -> towerText(text, "tower: {height: 1, wall: platform}", "t: 75, x: 400"),
                "backdrop.pieces.tower-a.tower.wall: 'platform' is a set piece or tile set");
        assertLoadProblem(
                text -> towerText(text, "tower: {height: 1, wall: Glass_Wall}", "t: 75, x: 400"),
                "tower.wall: an image id in kebab-case");
    }

    @Test
    void onlyAGroundPieceIsATower() {
        assertLoadProblem(
                text -> towerText(text, "tower: {height: 1, wall: wall-glass}", "t: 75, x: 400")
                        .replace("tower-a: {layer: ground", "tower-a: {layer: far"),
                "tower: only a ground piece is a tower, this one is on far");
    }

    @Test
    void aRepeatPlacesThePieceAgainAndShiftsItsPathWithIt() {
        BackdropData.PlacedPiece stream = new BackdropData.PlacedPiece(
                FLIGHT.piece(),
                FLIGHT.t(),
                FLIGHT.x(),
                FLIGHT.mirror(),
                FLIGHT.path(),
                FLIGHT.overhead(),
                Optional.of(new BackdropData.Repeat(3, 4)));

        BackdropData.PlacedPiece third = stream.copy(2);

        assertEquals(3, stream.count());
        assertEquals(10, third.t());
        assertEquals(
                List.of(9.0, 11.0, 13.0),
                third.path().orElseThrow().stream()
                        .map(BackdropData.Waypoint::t)
                        .toList());
        assertEquals(50, third.offsetY(10));
        assertTrue(third.repeat().isEmpty());
    }

    @Test
    void aRepeatedTowerIsPlacedCountTimes() {
        LevelData level = withTower(
                "tower: {height: 1, wall: wall-glass, shade: 0.4}", "t: 75, x: 400, repeat: {count: 3, every: 2}");

        List<Double> times = level.backdrop().placements().stream()
                .filter(p -> p.piece().equals("tower-a"))
                .map(BackdropData.PlacedPiece::t)
                .toList();

        assertEquals(List.of(75.0, 77.0, 79.0), times);
        assertEquals(
                LEVEL_01.backdrop().placed().size() + 1,
                level.backdrop().placed().size());
    }

    @Test
    void aRepeatThatLeavesTheLevelIsNeverOnScreen() {
        assertLoadProblem(
                text -> text.replace(
                        "{piece: moon, t: 21.2, x: 44}",
                        "{piece: moon, t: 21.2, x: 44, repeat: {count: 2, every: 1000}}"),
                "backdrop.placed[5] (repeat 2): 'moon' is never on screen");
        assertRejected(() -> new BackdropData.Repeat(1, 2), "repeat.count must be >= 2, was 1");
    }

    private static BackdropData.Tower tower(double height) {
        return new BackdropData.Tower(height, "wall-glass", Optional.empty());
    }

    /** Level 01 with a tower piece {@code tower-a} (80 × 60 px) and one placement of it. */
    private static LevelData withTower(String block, String placement) {
        return ContentLoader.load(edited(text -> towerText(text, block, placement)))
                .level(Level01Test.LEVEL);
    }

    private static String towerText(String text, String block, String placement) {
        assertTrue(text.contains(PLATFORM) && text.contains(CRANE), "Level 01's backdrop as the test expects");
        return text.replace(PLATFORM, PLATFORM + "    tower-a: {layer: ground, size: [80, 60], " + block + "}\n")
                .replace(CRANE, CRANE + "    - {piece: tower-a, " + placement + "}\n");
    }

    private static List<DataFile> edited(UnaryOperator<String> edit) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(LEVEL_01_FILE) ? new DataFile(f.path(), edit.apply(f.text())) : f)
                .toList();
    }

    /** Loading Level 01, edited, reports a problem that contains every part. */
    private static void assertLoadProblem(UnaryOperator<String> edit, String... parts) {
        var e = assertThrows(ContentException.class, () -> ContentLoader.load(edited(edit)));
        String found = e.problems().stream()
                .filter(problem -> List.of(parts).stream().allMatch(problem::contains))
                .findFirst()
                .orElse(null);
        assertTrue(found != null, String.join(" ... ", parts) + " in " + e.getMessage());
    }

    private static void assertRejected(Runnable create, String message) {
        var e = assertThrows(IllegalArgumentException.class, create::run);
        assertTrue(e.getMessage().contains(message), "'" + message + "' in " + e.getMessage());
    }
}
