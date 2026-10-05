package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.LoadoutSlot;

class LaunchOptionsTest {
    @Test
    void runsUntilQuitWithTheDefaultSettingsFileWithoutOptions() {
        assertEquals(
                new LaunchOptions(0, Optional.empty(), Difficulty.MEDIUM, 1, false, false, Optional.empty(), 1, false),
                LaunchOptions.parse());
    }

    @Test
    void parsesBenchAndSettings() {
        assertEquals(
                new LaunchOptions(
                        3,
                        Optional.of(Path.of("smoke.properties")),
                        Difficulty.MEDIUM,
                        1,
                        false,
                        true,
                        Optional.empty(),
                        1,
                        false),
                LaunchOptions.parse("--bench", "3", "--settings", "smoke.properties"));
    }

    @Test
    void parsesTheTestingOptions() {
        assertEquals(
                new LaunchOptions(0, Optional.empty(), Difficulty.HARD, 4, true, false, Optional.empty(), 1, false),
                LaunchOptions.parse("--difficulty", "hard", "--debug-speed", "4", "--invulnerable"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--difficulty", "nightmare"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--debug-speed", "0"));
    }

    @Test
    void aBenchRunFliesTheLevelUnlessItStartsAtTheTitle() {
        assertEquals(true, LaunchOptions.parse("--bench", "3").startLevel());
        assertEquals(
                false, LaunchOptions.parse("--bench", "3", "--start", "title").startLevel());
        assertEquals(true, LaunchOptions.parse("--start", "level").startLevel());
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--start", "hangar"));
    }

    @Test
    void aDebugFitStartsInTheLevelWithItsWeapons() {
        LaunchOptions options =
                LaunchOptions.parse("--loadout", "front=scatter-vulcan:3, left=bomb-rack,rear=side-splitter:2");

        assertEquals(true, options.startLevel());
        assertEquals(
                Map.of(
                        LoadoutSlot.FRONT, new Fitted("scatter-vulcan", 3),
                        LoadoutSlot.LEFT_WING, new Fitted("bomb-rack", 1),
                        LoadoutSlot.REAR, new Fitted("side-splitter", 2)),
                options.debugFit().orElseThrow().weapons());
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--loadout", "nose=pulse-cannon"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--loadout", "front=pulse-cannon:6"));
        assertThrows(
                IllegalArgumentException.class,
                () -> LaunchOptions.parse("--loadout", "front=pulse-cannon", "--start", "title"));
    }

    @Test
    void aDebugLevelStartsThere() {
        assertEquals(2, LaunchOptions.parse("--level", "2").level());
        assertEquals(true, LaunchOptions.parse("--level", "2").startLevel());
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--level", "0"));
    }

    @Test
    void theActEndOptionStartsInTheLevel() {
        LaunchOptions options = LaunchOptions.parse("--level", "6", "--act-end");

        assertEquals(true, options.actEnd());
        assertEquals(true, options.startLevel());
        assertEquals(false, LaunchOptions.parse("--level", "6").actEnd());
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--act-end", "--start", "title"));
    }

    @Test
    void rejectsUnknownOptionsAndMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--fullscreen"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--bench"));
    }
}
