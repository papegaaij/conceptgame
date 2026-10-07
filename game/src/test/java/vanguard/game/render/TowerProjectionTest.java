package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.BackdropData;

/**
 * The perspective towers' projection (design/art-direction, Perspective towers are scenery): a
 * roof h camera units high is drawn at k = 6 ÷ (6 − h) round the play field's (240, 243), 297 px
 * below its top edge.
 */
class TowerProjectionTest {
    private static final double K_135 = 6 / 4.65;

    @Test
    void theProjectionCentreLies297PxBelowTheTopEdge() {
        assertEquals(240, BackdropData.Tower.CENTRE_X);
        assertEquals(540 - 297, BackdropData.Tower.CENTRE_Y);
    }

    /**
     * The walls drawn are the ones facing the projection centre (the camera sees those): a tower left
     * of the centre shows its east wall, right of it its west wall, above it its south wall, below it
     * its north wall; one over the centre shows none. (The east and west walls were swapped before
     * 2026-10-07: a tower left of the centre drew its hidden west wall under its roof and left a gap
     * where its east wall belongs.)
     */
    @Test
    void theWallsFacingTheCentreAreTheOnesDrawn() {
        assertEquals(TowerProjection.EAST, sides(60, 243), "left of the centre");
        assertEquals(TowerProjection.WEST, sides(420, 243), "right of the centre");
        assertEquals(TowerProjection.SOUTH, sides(240, 480), "above the centre");
        assertEquals(TowerProjection.NORTH, sides(240, 40), "below the centre");
        assertEquals(TowerProjection.EAST | TowerProjection.SOUTH, sides(60, 480), "up and left: two walls");
        assertEquals(0, sides(240, 243), "over the centre: only the roof");
    }

    /** The visible sides of a 60 x 50 tower 1.35 units high whose footprint is centred at (x, y). */
    private static int sides(double x, double y) {
        double k = K_135;
        float fx0 = (float) (x - 30);
        float fy0 = (float) (y - 25);
        float rx0 = TowerProjection.roofLeft(x, (int) Math.round(60 * k), k);
        float ry0 = TowerProjection.roofBottom(y, (int) Math.round(50 * k), k);
        return TowerProjection.visibleSides(
                fx0, fy0, fx0 + 60, fy0 + 50, rx0, ry0, rx0 + Math.round(60 * k), ry0 + Math.round(50 * k));
    }

    @Test
    void aRoofOverTheCentreDoesNotLean() {
        // A 60 x 60 footprint at k = 1.29 has a 77 x 77 roof, centred over it.
        assertEquals(240 - 38, TowerProjection.roofLeft(240, 77, K_135));
        assertEquals(243 - 38, TowerProjection.roofBottom(243, 77, K_135));
    }

    @Test
    void aRoofLeansAwayFromTheCentre() {
        // At the field's side edges the parallax B concept's tallest roofs (h = 1.35) lean 70 px outward:
        // 240 x 0.29 = 69.7, less half the roof's 77 px.
        assertEquals(-108, TowerProjection.roofLeft(0, 77, K_135));
        assertEquals(511, TowerProjection.roofLeft(480, 77, K_135));
        // The bottom edge is 243 px below the centre (70.5 px of lean), the top edge 297 px above it (86.2 px).
        assertEquals(-109, TowerProjection.roofBottom(0, 77, K_135));
        assertEquals(588, TowerProjection.roofBottom(540, 77, K_135));
    }

    @Test
    void aLowerRoofLeansLess() {
        double k = BackdropData.Tower.scaleAt(0.5); // 1.09
        assertEquals(12.0 / 11, k, 1e-12);
        // A 20 px footprint has a 22 px roof: 21.8 px of lean at the left edge, 9.1 px at x = 340.
        assertEquals(-33, TowerProjection.roofLeft(0, 22, k));
        assertEquals(338, TowerProjection.roofLeft(340, 22, k));
    }

    @Test
    void aRoofMovesAtKTimesTheScroll() {
        // While the ground scrolls 100 px, the roof moves 129 px: its own speed is (k - 1) x the scroll.
        int before = TowerProjection.roofBottom(400, 77, K_135);
        int after = TowerProjection.roofBottom(300, 77, K_135);
        assertEquals(Math.round(100 * K_135), before - after);
        assertEquals(29, (before - after) - 100);
        // At a scroll of 140 px/s the tallest roof (k = 1.33): 47 px/s, inside the 120 px/s budget.
        assertEquals(140 / 3.0, new BackdropData.Tower(1.5, "wall", Optional.empty()).ownSpeed(140), 1e-9);
    }

    @Test
    void windowRowsFollowTheTruePerspectiveUpAWall() {
        assertEquals(0, TowerProjection.rise(0, 1.35), 1e-12);
        assertEquals(1, TowerProjection.rise(1, 1.35), 1e-12);
        // Half way up is drawn a little below half way to the roof's edge: rows grow towards the top.
        double half = TowerProjection.rise(0.5, 1.35);
        assertTrue(half < 0.5 && half > 0.4, "half way up: " + half);
        // s(0.675) = 6 / 5.325
        assertEquals((6 / 5.325 - 1) / (K_135 - 1), half, 1e-12);
    }

    @Test
    void aWallIsCutIntoBandsOfAtMostACell() {
        // Even a wall that hardly leans gets enough bands that each widens by at most a tenth.
        assertEquals(4, TowerProjection.bands(0, 4 / 3.0));
        assertEquals(3, TowerProjection.bands(10, K_135));
        assertEquals(2, TowerProjection.bands(10, BackdropData.Tower.scaleAt(0.9)));
        assertEquals(7, TowerProjection.bands(100, K_135));
        assertEquals(TowerProjection.MAX_BANDS, TowerProjection.bands(400, K_135));
    }
}
