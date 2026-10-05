package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.DifficultyData;

/** Retry from boss follows the difficulty data's {@code boss_checkpoint} (design/systems/difficulty). */
class BossCheckpointTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void easyAndMediumOfferItOnceReachedAndHardNever() {
        DifficultyData data = CONTENT.difficulty();

        assertTrue(LevelScreen.checkpointOffered(data, Difficulty.EASY, true));
        assertTrue(LevelScreen.checkpointOffered(data, Difficulty.MEDIUM, true));
        assertFalse(LevelScreen.checkpointOffered(data, Difficulty.HARD, true));
        assertFalse(LevelScreen.checkpointOffered(data, Difficulty.MEDIUM, false), "not reached yet");
    }

    @Test
    void everyLevelThemeHasAMusicFile() {
        assertEquals("coalition-rising", LevelScreen.theme(5));
        assertEquals("afterburner", LevelScreen.theme(4));
        for (var level : CONTENT.levels().values()) {
            LevelScreen.theme(level.music().track());
        }
    }
}
