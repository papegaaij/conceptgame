package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vanguard.game.display.Bounds;
import vanguard.game.display.DisplaySettings;
import vanguard.game.display.WindowMode;

class SettingsFileTest {
    @TempDir
    Path directory;

    @Test
    void startsInFullScreenOnThePrimaryMonitorWithoutAFile() {
        assertEquals(DisplaySettings.firstStart(), new SettingsFile(directory.resolve("settings.properties")).read());
    }

    @Test
    void readsBackWhatItWrote() {
        var file = new SettingsFile(directory.resolve("new").resolve("settings.properties"));
        var settings = new DisplaySettings(
                WindowMode.WINDOWED, Optional.of("DP-2"), Optional.of(new Bounds(4320, 286, 2880, 1620)));

        file.write(settings);

        assertEquals(settings, file.read());
    }

    @Test
    void writesAReadablePropertiesFile() throws IOException {
        Path path = directory.resolve("settings.properties");
        new SettingsFile(path)
                .write(new DisplaySettings(WindowMode.FULL_SCREEN, Optional.of("HDMI-0"), Optional.empty()));

        String text = Files.readString(path);

        assertTrue(text.contains("display.mode=full-screen"), text);
        assertTrue(text.contains("display.monitor=HDMI-0"), text);
        assertFalse(text.contains("window."), text);
    }

    @Test
    void ignoresInvalidValues() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "display.mode=sideways\nwindow.x=1\nwindow.y=2\nwindow.width=wide\nwindow.height=3\n");

        assertEquals(DisplaySettings.firstStart(), new SettingsFile(path).read());
    }

    @Test
    void ignoresAWindowWithoutArea() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "display.mode=window\nwindow.x=1\nwindow.y=2\nwindow.width=0\nwindow.height=540\n");

        assertEquals(
                new DisplaySettings(WindowMode.WINDOWED, Optional.empty(), Optional.empty()),
                new SettingsFile(path).read());
    }
}
