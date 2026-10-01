package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigDirectoryTest {
    @TempDir
    Path home;

    @Test
    void linuxUsesXdgConfigHome() {
        Path configHome = home.resolve("xdg");

        assertEquals(
                configHome.resolve("terran-vanguard"),
                ConfigDirectory.of("Linux", Map.of("XDG_CONFIG_HOME", configHome.toString()), home));
    }

    @Test
    void linuxFallsBackToDotConfigWhenXdgConfigHomeIsUnsetOrRelative() {
        Path expected = home.resolve(".config").resolve("terran-vanguard");

        assertEquals(expected, ConfigDirectory.of("Linux", Map.of(), home));
        assertEquals(expected, ConfigDirectory.of("Linux", Map.of("XDG_CONFIG_HOME", "relative"), home));
    }

    @Test
    void windowsUsesAppData() {
        Path appData = home.resolve("Roaming");

        assertEquals(
                appData.resolve("Terran Vanguard"),
                ConfigDirectory.of("Windows 11", Map.of("APPDATA", appData.toString()), home));
    }

    @Test
    void macOsUsesApplicationSupport() {
        assertEquals(
                home.resolve("Library").resolve("Application Support").resolve("Terran Vanguard"),
                ConfigDirectory.of("Mac OS X", Map.of(), home));
    }
}
