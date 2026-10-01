package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LaunchOptionsTest {
    @Test
    void runsUntilQuitWithTheDefaultSettingsFileWithoutOptions() {
        assertEquals(new LaunchOptions(0, Optional.empty()), LaunchOptions.parse());
    }

    @Test
    void parsesBenchAndSettings() {
        assertEquals(
                new LaunchOptions(3, Optional.of(Path.of("smoke.properties"))),
                LaunchOptions.parse("--bench", "3", "--settings", "smoke.properties"));
    }

    @Test
    void rejectsUnknownOptionsAndMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--fullscreen"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse("--bench"));
    }
}
