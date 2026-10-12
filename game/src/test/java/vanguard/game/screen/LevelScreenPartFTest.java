package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * M5 part F's wiring in the level screen (Level 12): Rook's first-latch line counts for his barks'
 * spacing (the shake-off's event prompt is tested with its box, PartFHudTest), and the debrief's row reads {@code SONDES n / 3}.
 */
class LevelScreenPartFTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(12).orElseThrow();

    @Test
    void rooksFirstLatchLineCountsForHisBarksSpacing() {
        assertTrue(LevelScreen.scriptedForBarks(LevelScript.CueTrigger.FIRST_LATCH));
    }

    @Test
    void theDebriefsSondesRowCountsTheSondesCollected() {
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM),
                60);
        DebriefScreen.Hulls count = LevelScreen.debriefCount(sortie);
        assertEquals(DebriefScreen.SONDES, count.label());
        assertEquals(0, count.afloat());
        assertEquals(3, count.of());
        DebriefScreen.Row met = DebriefScreen.hullsRow(new DebriefScreen.Hulls(DebriefScreen.SONDES, 3, 3), true, 120);
        assertEquals("SONDES", met.label());
        assertEquals("3 / 3", met.middle());
        assertEquals("+ 120 CR", met.right(1));
        DebriefScreen.Row missed =
                DebriefScreen.hullsRow(new DebriefScreen.Hulls(DebriefScreen.SONDES, 2, 3), false, 0);
        assertEquals("2 / 3", missed.middle());
        assertEquals("", missed.right(1), "nothing paid");
        assertEquals("HULLS AFLOAT", new DebriefScreen.Hulls(3, 3).label(), "Level 11's row as before");
    }
}
