package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.game.render.LevelRenderer;
import vanguard.sim.SetPiece;
import vanguard.sim.Sortie;

/**
 * M5 part H's wiring in the level screen (Level 14): the debrief's row reads {@code ROOTS n / 8} (easy
 * {@code n / 4}), paid when met; the Siege Spire's death is its own (its looks, not the Kraken's arena
 * death).
 */
class LevelScreenPartHTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(14).orElseThrow();

    @Test
    void theDebriefsRootsRowCountsTheGrowthsDestroyed() {
        for (Difficulty difficulty : Difficulty.values()) {
            DebriefScreen.Hulls count = LevelScreen.debriefCount(sortie(difficulty));
            assertEquals("ROOTS", count.label());
            assertFalse(count.percent());
            assertEquals(0, count.afloat(), "none destroyed at the start");
            assertEquals(difficulty == Difficulty.EASY ? 4 : 8, count.of(), difficulty.toString());
        }
        DebriefScreen.Row met = DebriefScreen.hullsRow(new DebriefScreen.Hulls("ROOTS", 8, 8), true, 101);
        assertEquals("ROOTS", met.label());
        assertEquals("8 / 8", met.middle());
        assertEquals("+ 101 CR", met.right(1));
        DebriefScreen.Row missed = DebriefScreen.hullsRow(new DebriefScreen.Hulls("ROOTS", 7, 8), false, 0);
        assertEquals("7 / 8", missed.middle());
        assertEquals("", missed.right(1), "nothing paid");
        // Level 02's docks (a groups secondary without a label) keep their pips and no count row.
        String docks = CONTENT.levelKey(2).orElseThrow();
        Sortie level02 = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, docks, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, docks, Difficulty.MEDIUM),
                60);
        assertFalse(LevelScreen.debriefCount(level02).present());
    }

    @Test
    void theSpireDiesByItsOwnLooks() {
        Sortie sortie = sortie(Difficulty.MEDIUM);
        boolean found = false;
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            if (piece.boss().isPresent()) {
                assertTrue(LevelRenderer.spire(piece), "the Siege Spire");
                assertTrue(LevelRenderer.flashesAtDeath(piece), "an act boss's flash");
                assertEquals(180, LevelRenderer.chainTicks(piece), "its 3 s death chain");
                found = true;
            }
        }
        assertTrue(found);
        Sortie level11 = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, CONTENT.levelKey(11).orElseThrow(), Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, CONTENT.levelKey(11).orElseThrow(), Difficulty.MEDIUM),
                60);
        for (int k = 0; k < level11.setPieceCount(); k++) {
            assertFalse(LevelRenderer.spire(level11.setPiece(k)), "the Kraken keeps its own");
        }
    }

    private static Sortie sortie(Difficulty difficulty) {
        return new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, difficulty),
                SimSpecs.level(CONTENT, KEY, difficulty),
                SimSpecs.rules(CONTENT, KEY, difficulty),
                60);
    }
}
