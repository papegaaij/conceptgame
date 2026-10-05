package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.sim.PlayField;
import vanguard.sim.Sortie;

class ThreatArrowsTest {
    private static final int W = PlayField.WIDTH;
    private static final int H = PlayField.HEIGHT;

    private final ThreatArrows arrows = new ThreatArrows();

    /** A threat at (x, y) that moved by (dx, dy) in the last step. */
    private void threat(double x, double y, double dx, double dy) {
        arrows.track(x - dx, y - dy, x, y, x, y);
    }

    @Test
    void eachArrowPointsOutOfItsEdgeAtItsThreat() {
        threat(-30, 200, 2, 0);
        threat(W + 30, 300, -2, 0);
        threat(120, H + 30, 0, -2);
        threat(360, -30, 0, 2);

        assertEquals(4, arrows.count());
        assertArrow(0, ThreatArrows.LEFT, ThreatArrows.INSET, 200, 180);
        assertArrow(1, ThreatArrows.RIGHT, W - ThreatArrows.INSET, 300, 0);
        assertArrow(2, ThreatArrows.TOP, 120, H - ThreatArrows.INSET, 90);
        assertArrow(3, ThreatArrows.BOTTOM, 360, ThreatArrows.INSET, 270);
    }

    private void assertArrow(int arrow, int edge, float x, float y, float rotation) {
        assertEquals(edge, arrows.edgeOf(arrow));
        assertEquals(x, arrows.x(arrow));
        assertEquals(y, arrows.y(arrow));
        assertEquals(rotation, arrows.rotation(arrow));
    }

    @Test
    void aCornerThreatTakesTheEdgeItIsFarthestBeyond() {
        threat(-60, H + 20, 1, -1);

        assertEquals(ThreatArrows.LEFT, arrows.edgeOf(0));
        assertEquals(H - 2 * ThreatArrows.INSET, arrows.y(0), "kept off the corner");
    }

    @Test
    void enemiesOnTheScreenOutOfRangeOrLeavingGetNone() {
        threat(240, 270, 0, -2);
        threat(-ThreatArrows.RANGE - 10, 200, 2, 0);
        threat(-30, 200, -2, 0);
        threat(240, -30, 0, -2);

        assertEquals(0, arrows.count());
    }

    @Test
    void aThreatWaitingOffTheScreenKeepsItsArrow() {
        threat(W + 40, 400, 0, 0);

        assertEquals(1, arrows.count());
    }

    @Test
    void anArrowGrowsAndBrightensAsItsThreatClosesIn() {
        threat(-200, 100, 2, 0);
        threat(-10, 400, 2, 0);

        assertTrue(arrows.size(1) > arrows.size(0));
        assertTrue(arrows.opacity(1) > arrows.opacity(0));
        assertTrue(arrows.size(1) <= ThreatArrows.LARGE);
        assertTrue(arrows.size(0) >= ThreatArrows.SMALL);
    }

    @Test
    void threatsCloseTogetherShareAnArrowAndAtMostEightShow() {
        threat(100, H + 40, 0, -2);
        threat(110, H + 20, 0, -2);
        assertEquals(1, arrows.count());
        assertEquals(110, arrows.x(0), "at the nearer threat");

        for (int i = 0; i < 12; i++) {
            threat(-30, 40 + 40 * i, 2, 0);
        }
        assertEquals(ThreatArrows.MAX, arrows.count());
    }

    @Test
    void everyArrowStaysInsideThePlayField() {
        threat(-30, -400, 0, 2);
        threat(W + 100, H + 30, -2, -2);
        for (int i = 0; i < arrows.count(); i++) {
            float half = ThreatArrows.LARGE / 2 + 2;
            assertTrue(arrows.x(i) - half >= 0 && arrows.x(i) + half <= W, "x " + arrows.x(i));
            assertTrue(arrows.y(i) - half >= 0 && arrows.y(i) + half <= H, "y " + arrows.y(i));
        }
    }

    /**
     * Level 06 flown without input: a Coilwyrm entering from the rear (after its loop or its rear
     * entry) shows rear arrows for its body still below the screen, for seconds in all.
     */
    @Test
    void inFarsideTheArrowsFollowTheCoilwyrmsEnteringFromTheRear() {
        Content content = ContentLoader.fromClasspath();
        String key = content.levelKey(6).orElseThrow();
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(content, Difficulty.MEDIUM),
                SimSpecs.level(content, key, Difficulty.MEDIUM),
                SimSpecs.rules(content, key, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
        int rear = 0;
        for (int t = 0; t < 60 * 240 && !sortie.complete(); t++) {
            sortie.step(0);
            arrows.plan(sortie, 1);
            for (int i = 0; i < arrows.count(); i++) {
                rear += arrows.edgeOf(i) == ThreatArrows.BOTTOM ? 1 : 0;
            }
        }
        assertTrue(rear > 3 * 60, "rear arrows for " + rear + " steps");
    }
}
