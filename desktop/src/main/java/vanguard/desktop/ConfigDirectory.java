package vanguard.desktop;

import java.nio.file.Path;
import java.util.Map;

/** The per-user directory for the settings file, following each platform's convention. */
final class ConfigDirectory {
    private ConfigDirectory() {}

    /** The directory on the platform the game runs on. */
    static Path current() {
        return of(System.getProperty("os.name"), System.getenv(), Path.of(System.getProperty("user.home")));
    }

    /**
     * Linux and other Unix systems: {@code $XDG_CONFIG_HOME/terran-vanguard} (default
     * {@code ~/.config}); Windows: {@code %APPDATA%\Terran Vanguard}; macOS:
     * {@code ~/Library/Application Support/Terran Vanguard}.
     */
    static Path of(String osName, Map<String, String> environment, Path home) {
        if (osName.startsWith("Windows")) {
            String appData = environment.get("APPDATA");
            Path base =
                    appData != null ? Path.of(appData) : home.resolve("AppData").resolve("Roaming");
            return base.resolve("Terran Vanguard");
        }
        if (osName.startsWith("Mac")) {
            return home.resolve("Library").resolve("Application Support").resolve("Terran Vanguard");
        }
        // The XDG spec says to ignore relative paths.
        String configHome = environment.get("XDG_CONFIG_HOME");
        Path base =
                configHome != null && Path.of(configHome).isAbsolute() ? Path.of(configHome) : home.resolve(".config");
        return base.resolve("terran-vanguard");
    }
}
