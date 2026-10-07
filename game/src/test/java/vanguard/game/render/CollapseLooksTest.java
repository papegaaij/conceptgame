package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;

/**
 * Level 09's collapse as drawn (M5 part C, D5 = a; round 31's look c): the tower leans to the right
 * over the 1.5 s warning (4°, eased in), drops straight down in 1.5 s (gone at the impact, the lean
 * relaxing to 0.65 of its end), and the heavy dust peak comes in from 0.35 s before the impact, is
 * full 0.3 s after it and ramps out over 6 s.
 */
class CollapseLooksTest {
    private static final double WARNING = 1.5;
    private static final double DROP = 1.5;

    @Test
    void theTowerLeansToTheRightOverTheWarningEasedIn() {
        assertEquals(0, CollapseLooks.lean(0, WARNING, DROP));
        assertEquals(Math.toRadians(4), CollapseLooks.lean(WARNING, WARNING, DROP), 1e-12, "4° at the warning's end");
        assertEquals(
                Math.toRadians(0.3), CollapseLooks.lean(0.3, WARNING, DROP), Math.toRadians(0.01), "slow at first");
        assertEquals(Math.toRadians(1.8), CollapseLooks.lean(0.9, WARNING, DROP), Math.toRadians(0.05));
        for (double t = 0.05; t < WARNING; t += 0.05) {
            assertTrue(CollapseLooks.lean(t, WARNING, DROP) > CollapseLooks.lean(t - 0.05, WARNING, DROP), "one way");
        }
        assertEquals(
                0.65 * Math.toRadians(4),
                CollapseLooks.lean(WARNING + DROP, WARNING, DROP),
                1e-12,
                "relaxing through the drop");
    }

    @Test
    void itDropsInOneGoFasterAtTheEnd() {
        assertEquals(1, CollapseLooks.share(WARNING, WARNING, DROP), "standing through the lean");
        assertEquals(1 - (0.25 * 0.5 + 0.75 * 0.25), CollapseLooks.share(WARNING + 0.75, WARNING, DROP), 1e-12);
        assertEquals(0, CollapseLooks.share(WARNING + DROP, WARNING, DROP), 1e-12, "on the ground at the impact");
        double first = 1 - CollapseLooks.share(WARNING + 0.5, WARNING, DROP);
        double last = CollapseLooks.share(WARNING + 1.0, WARNING, DROP); // the last third's fall: to 0
        assertTrue(last > 2 * first, "a slow start, a fast end: the first third sinks " + first + ", the last " + last);
    }

    /** The lean bends the tower from its foot: 28 px at the roof of a 400 px tower leaning 4°. */
    @Test
    void theLeanBendsFromTheFoot() {
        assertEquals(400 * Math.sin(Math.toRadians(4)), TowerProjection.leanOffset(1, 400, Math.toRadians(4)), 1e-9);
        assertEquals(27.9, TowerProjection.leanOffset(1, 400, Math.toRadians(4)), 0.05);
        assertEquals(
                TowerProjection.leanOffset(1, 400, Math.toRadians(4)) / 4,
                TowerProjection.leanOffset(0.5, 400, Math.toRadians(4)),
                1e-9,
                "half way up a quarter of it");
        assertEquals(0, TowerProjection.leanOffset(0, 400, Math.toRadians(4)));
    }

    /** The cast shadow at half the kit's length: 217 px for the standing arcology, right and down. */
    @Test
    void theShadowIsHalfTheKitsLength() {
        double length = Math.hypot(CollapseLooks.SHADOW_X, CollapseLooks.SHADOW_Y) * 400;
        assertEquals(217, length, 1);
        assertTrue(CollapseLooks.SHADOW_X > 0 && CollapseLooks.SHADOW_Y < 0, "right and down");
    }

    /**
     * Level 09's heavy dust, from the real seconds since the warning started (the sortie's {@code
     * collapseSeconds()}, which run on after its end): in from the base dust's start, full just after
     * the impact, gone 6 s later.
     */
    @Test
    void level09sDustPeaksAtTheImpactAndIsGoneItsSecondsLater() {
        Content content = ContentLoader.fromClasspath();
        LevelData.Collapse collapse =
                content.level(content.levelKey(9).orElseThrow()).collapse().orElseThrow();
        double impact = collapse.impact();
        assertEquals(3.0, impact, 1e-9, "the 1.5 s lean and the 1.5 s drop");
        assertEquals(6, collapse.dust().seconds());

        assertEquals(0, CollapseLooks.dustAt(impact - 0.36, collapse), "none before the base dust");
        assertTrue(CollapseLooks.dustAt(impact, collapse) > 0.5);
        assertEquals(1, CollapseLooks.dustAt(impact + 0.3, collapse), 1e-6, "full just after the impact");
        assertEquals(0.5, CollapseLooks.dustAt(impact + 3.3, collapse), 1e-6, "half way out");
        assertTrue(CollapseLooks.dustAt(impact + 6.2, collapse) > 0);
        assertEquals(0, CollapseLooks.dustAt(impact + 6.3, collapse), 1e-9, "gone 6 s later");
        assertEquals(0, CollapseLooks.dustAt(impact + 60, collapse));
    }
}
