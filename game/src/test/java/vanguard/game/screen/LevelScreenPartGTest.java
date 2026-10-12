package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.sim.Sortie;

/**
 * M5 part G's wiring in the level screen (Level 13): the debrief's row reads {@code RELAY n %}, the
 * relay's integrity left rounded down so a relay under half never reads 50 % (the secondary's
 * threshold); with the integrity secondary met it pays, without it nothing.
 */
class LevelScreenPartGTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(13).orElseThrow();

    @Test
    void theDebriefsRelayRowShowsTheIntegrityLeft() {
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM),
                60);
        DebriefScreen.Hulls count = LevelScreen.debriefCount(sortie);
        assertEquals(DebriefScreen.RELAY, count.label());
        assertTrue(count.percent());
        assertEquals(100, count.afloat(), "untouched at the start");
        DebriefScreen.Row met = DebriefScreen.hullsRow(DebriefScreen.Hulls.share(DebriefScreen.RELAY, 53), true, 152);
        assertEquals("RELAY", met.label());
        assertEquals("53 %", met.middle());
        assertEquals("+ 152 CR", met.right(1));
        DebriefScreen.Row missed = DebriefScreen.hullsRow(DebriefScreen.Hulls.share(DebriefScreen.RELAY, 49), false, 0);
        assertEquals("49 %", missed.middle());
        assertEquals("", missed.right(1), "nothing paid");
        assertEquals(
                "3 / 3",
                DebriefScreen.hullsRow(new DebriefScreen.Hulls(3, 3), true, 0).middle(),
                "counts as before");
    }

    @Test
    void aShareUnderHalfNeverReadsFiftyPercent() {
        assertEquals(50, LevelScreen.percent(0.5));
        assertEquals(49, LevelScreen.percent(0.4999));
        assertEquals(100, LevelScreen.percent(1));
        assertEquals(0, LevelScreen.percent(0));
        assertEquals(29, LevelScreen.percent(70 / 240.0), "70 of 240");
    }
}
