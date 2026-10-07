package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.LevelData;
import vanguard.content.LevelData.Music.FullOn;

/** M5 part C: the full mix plays while the sortie's state asks for it, as the level's {@code full_on} names it. */
class RunTimeMusicTest {
    private static LevelData.Music music(Optional<List<FullOn>> fullOn) {
        return new LevelData.Music(
                7,
                1,
                Optional.empty(),
                99,
                Optional.empty(),
                "earth-megacity",
                "mission-complete",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                fullOn);
    }

    @Test
    void holdsAndTheCollapsePlayTheFullMixOnlyWhenTheLevelNamesThem() {
        LevelData.Music level09 = music(Optional.of(List.of(FullOn.HOLD, FullOn.COLLAPSE)));
        assertFalse(LevelScreen.runTimeFull(level09, false, false));
        assertTrue(LevelScreen.runTimeFull(level09, true, false));
        assertTrue(LevelScreen.runTimeFull(level09, false, true));

        LevelData.Music holdsOnly = music(Optional.of(List.of(FullOn.HOLD)));
        assertTrue(LevelScreen.runTimeFull(holdsOnly, true, false));
        assertFalse(LevelScreen.runTimeFull(holdsOnly, false, true));

        LevelData.Music none = music(Optional.empty());
        assertFalse(LevelScreen.runTimeFull(none, true, true));
    }
}
