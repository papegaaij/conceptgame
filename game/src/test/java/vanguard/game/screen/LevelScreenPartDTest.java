package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.game.audio.LevelMusic;
import vanguard.game.audio.Sfx;
import vanguard.sim.LevelScript;

/**
 * M5 part D's wiring in the level screen: Level 10's ambience change from its music block, the
 * {@code ally-lost} cue naming the unit lost last, and Rook's flock line counting for his barks.
 */
class LevelScreenPartDTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void levelTenCrossfadesToTheOceanAtSectionFourOverFourSeconds() {
        var music = CONTENT.level(CONTENT.levelKey(10).orElseThrow()).music();
        assertEquals(
                List.of(new LevelMusic.AmbienceChange(4, Sfx.AMBIENCE_OCEAN, 4)), LevelScreen.ambienceChanges(music));
        var nine = CONTENT.level(CONTENT.levelKey(9).orElseThrow()).music();
        assertEquals(List.of(), LevelScreen.ambienceChanges(nine), "Level 09 keeps its one ambience");
    }

    @Test
    void rooksFlockLineAndFirstKillCountForHisBarksSpacing() {
        assertTrue(LevelScreen.scriptedForBarks(LevelScript.CueTrigger.ESCORT_FIRST_KILL));
        assertTrue(LevelScreen.scriptedForBarks(LevelScript.CueTrigger.FIRST_LOOP_BACK));
        assertFalse(LevelScreen.scriptedForBarks(LevelScript.CueTrigger.FIRST_DECLOAK));
        assertFalse(LevelScreen.scriptedForBarks(LevelScript.CueTrigger.TIME));
    }
}
