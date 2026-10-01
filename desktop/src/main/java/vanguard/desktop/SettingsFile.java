package vanguard.desktop;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;
import vanguard.game.display.Bounds;
import vanguard.game.display.DisplaySettings;
import vanguard.game.display.WindowMode;

/**
 * The settings file: a Java properties file, kept apart from the save slots (design/ui/options).
 * Missing or unreadable values fall back to the first-start defaults, so a damaged file never
 * stops the game from starting.
 */
final class SettingsFile {
    static final String FILE_NAME = "settings.properties";

    private static final System.Logger LOG = System.getLogger(SettingsFile.class.getName());
    private static final String MODE = "display.mode";
    private static final String MONITOR = "display.monitor";
    private static final String WINDOW_X = "window.x";
    private static final String WINDOW_Y = "window.y";
    private static final String WINDOW_WIDTH = "window.width";
    private static final String WINDOW_HEIGHT = "window.height";
    private static final String FULL_SCREEN = "full-screen";
    private static final String WINDOWED = "window";

    private final Path path;

    SettingsFile(Path path) {
        this.path = path;
    }

    Path path() {
        return path;
    }

    /** The settings in the file, or the first-start defaults when there is no usable file. */
    DisplaySettings read() {
        Properties properties = new Properties();
        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (NoSuchFileException e) {
            return DisplaySettings.firstStart();
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "ignoring unreadable settings file " + path, e);
            return DisplaySettings.firstStart();
        }
        WindowMode mode = WINDOWED.equals(properties.getProperty(MODE)) ? WindowMode.WINDOWED : WindowMode.FULL_SCREEN;
        Optional<String> monitor =
                Optional.ofNullable(properties.getProperty(MONITOR)).filter(name -> !name.isBlank());
        return new DisplaySettings(mode, monitor, window(properties));
    }

    private static Optional<Bounds> window(Properties properties) {
        try {
            var bounds = new Bounds(
                    Integer.parseInt(properties.getProperty(WINDOW_X, "")),
                    Integer.parseInt(properties.getProperty(WINDOW_Y, "")),
                    Integer.parseInt(properties.getProperty(WINDOW_WIDTH, "")),
                    Integer.parseInt(properties.getProperty(WINDOW_HEIGHT, "")));
            return bounds.width() > 0 && bounds.height() > 0 ? Optional.of(bounds) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Writes the settings; a failure is logged, since losing them must not crash the game. */
    void write(DisplaySettings settings) {
        Properties properties = new Properties();
        properties.setProperty(MODE, settings.mode() == WindowMode.WINDOWED ? WINDOWED : FULL_SCREEN);
        settings.monitor().ifPresent(name -> properties.setProperty(MONITOR, name));
        settings.window().ifPresent(window -> {
            properties.setProperty(WINDOW_X, Integer.toString(window.x()));
            properties.setProperty(WINDOW_Y, Integer.toString(window.y()));
            properties.setProperty(WINDOW_WIDTH, Integer.toString(window.width()));
            properties.setProperty(WINDOW_HEIGHT, Integer.toString(window.height()));
        });
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            // Write next to the file and move it into place, so a crash never leaves half a file.
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer out = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(out, "Terran Vanguard settings");
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "could not write settings file " + path, e);
        }
    }
}
