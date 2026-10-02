package vanguard.desktop;

import com.badlogic.gdx.Input.Keys;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import vanguard.game.display.Bounds;
import vanguard.game.display.DisplaySettings;
import vanguard.game.display.WindowMode;
import vanguard.game.input.Action;
import vanguard.game.input.Binding;
import vanguard.game.input.BindingSlot;
import vanguard.game.input.Bindings;
import vanguard.game.input.ControlSettings;
import vanguard.game.input.GamepadControl;
import vanguard.game.settings.AudioSettings;
import vanguard.game.settings.GameplaySettings;
import vanguard.game.settings.Scaling;
import vanguard.game.settings.Settings;
import vanguard.game.settings.SettingsStore;
import vanguard.game.settings.VideoSettings;

/**
 * The settings file: a Java properties file, kept apart from the save slots (design/ui/options).
 * The display switcher writes the display keys, the Options screen the others; each keeps what the
 * other wrote. Missing or unreadable values fall back to their defaults one by one, so a damaged
 * file never stops the game from starting.
 */
final class SettingsFile implements SettingsStore {
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

    private static final String SCALING = "video.scaling";
    private static final String SCANLINES = "video.scanlines";
    private static final String MASTER = "audio.master";
    private static final String MUSIC = "audio.music";
    private static final String EFFECTS = "audio.effects";
    private static final String RADIO = "audio.radio";
    private static final String AUTO_FIRE = "controls.auto-fire";
    private static final String DEAD_ZONE = "controls.dead-zone";
    private static final String TEXT_SPEED = "gameplay.text-speed";
    private static final String SCREEN_SHAKE = "gameplay.screen-shake";
    private static final String FLASH_REDUCTION = "gameplay.flash-reduction";
    private static final String NO_KEY = "none";

    private final Path path;

    SettingsFile(Path path) {
        this.path = path;
    }

    Path path() {
        return path;
    }

    /** The display settings in the file, or the first-start defaults when there is no usable file. */
    DisplaySettings read() {
        Optional<Properties> loaded = load();
        if (loaded.isEmpty()) {
            return DisplaySettings.firstStart();
        }
        Properties properties = loaded.get();
        WindowMode mode = WINDOWED.equals(properties.getProperty(MODE)) ? WindowMode.WINDOWED : WindowMode.FULL_SCREEN;
        Optional<String> monitor =
                Optional.ofNullable(properties.getProperty(MONITOR)).filter(name -> !name.isBlank());
        return new DisplaySettings(mode, monitor, window(properties));
    }

    /** The Options settings in the file; each missing or invalid value is its default. */
    Settings readSettings() {
        Properties properties = load().orElseGet(Properties::new);
        Settings defaults = Settings.defaults();
        var video = new VideoSettings(
                "sharp-bilinear".equals(properties.getProperty(SCALING)) ? Scaling.SHARP_BILINEAR : Scaling.INTEGER,
                Boolean.parseBoolean(properties.getProperty(SCANLINES)));
        AudioSettings audio = defaults.audio();
        var readAudio = new AudioSettings(
                share(properties, MASTER, audio.master(), 0, 1),
                share(properties, MUSIC, audio.music(), 0, 1),
                share(properties, EFFECTS, audio.effects(), 0, 1),
                share(properties, RADIO, audio.radio(), 0, 1));
        var controls = new ControlSettings(
                Boolean.parseBoolean(properties.getProperty(AUTO_FIRE)),
                share(properties, DEAD_ZONE, ControlSettings.DEFAULT_DEAD_ZONE, 0.05, 0.5),
                bindings(properties));
        GameplaySettings gameplay = defaults.gameplay();
        var readGameplay = new GameplaySettings(
                (int) share(properties, TEXT_SPEED, gameplay.textSpeed(), 10, 90),
                share(properties, SCREEN_SHAKE, gameplay.screenShake(), 0, 1),
                Boolean.parseBoolean(properties.getProperty(FLASH_REDUCTION)));
        return new Settings(video, readAudio, controls, readGameplay);
    }

    /** A number in {@code [min, max]}, or the default when it is missing, unreadable or outside. */
    private static double share(Properties properties, String key, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, ""));
            return value >= min && value <= max ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * The remappable actions' bindings; a slot that is missing or unreadable keeps its default, and so
     * do Pause's fixed primary key and a slot naming a fixed system key (design/ui/controls).
     */
    private static Bindings bindings(Properties properties) {
        Bindings bindings = Bindings.defaults();
        for (Action action : Action.REMAPPABLE) {
            Binding binding = bindings.get(action);
            int primary = action.remappable(BindingSlot.PRIMARY)
                    ? key(properties.getProperty(prefix(action) + "primary"), binding.primaryKey())
                    : binding.primaryKey();
            int alternative = key(properties.getProperty(prefix(action) + "alternative"), binding.alternativeKey());
            Set<GamepadControl> gamepad = action.gamepadRemappable()
                    ? gamepad(properties.getProperty(prefix(action) + "gamepad"), binding.gamepad())
                    : binding.gamepad();
            bindings = bindings.with(action, new Binding(primary, alternative, gamepad));
        }
        return bindings;
    }

    private static String prefix(Action action) {
        return "controls." + action.name().toLowerCase(Locale.ROOT).replace('_', '-') + ".";
    }

    private static int key(String name, int fallback) {
        if (name == null) {
            return fallback;
        }
        if (name.equals(NO_KEY)) {
            return Binding.NO_KEY;
        }
        int key = Keys.valueOf(name);
        return key > 0 && !Bindings.systemKey(key) ? key : fallback;
    }

    private static String keyName(int key) {
        return key == Binding.NO_KEY ? NO_KEY : Keys.toString(key);
    }

    private static Set<GamepadControl> gamepad(String names, Set<GamepadControl> fallback) {
        if (names == null || names.isBlank()) {
            return fallback;
        }
        Set<GamepadControl> controls = EnumSet.noneOf(GamepadControl.class);
        for (String name : names.split(",")) {
            try {
                GamepadControl control = GamepadControl.valueOf(name.strip());
                if (!GamepadControl.BUTTONS.contains(control)) {
                    return fallback;
                }
                controls.add(control);
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
        return controls;
    }

    /** The file's properties; empty when there is no file or it cannot be read. */
    private Optional<Properties> load() {
        Properties properties = new Properties();
        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(in);
            return Optional.of(properties);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "ignoring unreadable settings file " + path, e);
            return Optional.empty();
        }
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

    /** Writes the display settings, keeping the file's other settings. */
    void write(DisplaySettings settings) {
        update(properties -> {
            properties.remove(MONITOR);
            properties.remove(WINDOW_X);
            properties.remove(WINDOW_Y);
            properties.remove(WINDOW_WIDTH);
            properties.remove(WINDOW_HEIGHT);
            properties.setProperty(MODE, settings.mode() == WindowMode.WINDOWED ? WINDOWED : FULL_SCREEN);
            settings.monitor().ifPresent(name -> properties.setProperty(MONITOR, name));
            settings.window().ifPresent(window -> {
                properties.setProperty(WINDOW_X, Integer.toString(window.x()));
                properties.setProperty(WINDOW_Y, Integer.toString(window.y()));
                properties.setProperty(WINDOW_WIDTH, Integer.toString(window.width()));
                properties.setProperty(WINDOW_HEIGHT, Integer.toString(window.height()));
            });
        });
    }

    /** Writes the Options settings, keeping the file's display settings. */
    @Override
    public void save(Settings settings) {
        update(properties -> {
            VideoSettings video = settings.video();
            properties.setProperty(SCALING, video.scaling() == Scaling.SHARP_BILINEAR ? "sharp-bilinear" : "integer");
            properties.setProperty(SCANLINES, Boolean.toString(video.scanlines()));
            AudioSettings audio = settings.audio();
            properties.setProperty(MASTER, Double.toString(audio.master()));
            properties.setProperty(MUSIC, Double.toString(audio.music()));
            properties.setProperty(EFFECTS, Double.toString(audio.effects()));
            properties.setProperty(RADIO, Double.toString(audio.radio()));
            ControlSettings controls = settings.controls();
            properties.setProperty(AUTO_FIRE, Boolean.toString(controls.autoFire()));
            properties.setProperty(DEAD_ZONE, Double.toString(controls.deadZone()));
            for (Action action : Action.REMAPPABLE) {
                Binding binding = controls.bindings().get(action);
                properties.setProperty(prefix(action) + "primary", keyName(binding.primaryKey()));
                properties.setProperty(prefix(action) + "alternative", keyName(binding.alternativeKey()));
                if (action.gamepadRemappable()) {
                    properties.setProperty(
                            prefix(action) + "gamepad",
                            Arrays.stream(GamepadControl.values())
                                    .filter(binding.gamepad()::contains)
                                    .map(GamepadControl::name)
                                    .collect(Collectors.joining(",")));
                }
            }
            GameplaySettings gameplay = settings.gameplay();
            properties.setProperty(TEXT_SPEED, Integer.toString(gameplay.textSpeed()));
            properties.setProperty(SCREEN_SHAKE, Double.toString(gameplay.screenShake()));
            properties.setProperty(FLASH_REDUCTION, Boolean.toString(gameplay.flashReduction()));
        });
    }

    /**
     * Changes the file's properties and writes them; a failure is logged, since losing settings must
     * not crash the game.
     */
    private void update(Consumer<Properties> change) {
        Properties properties = load().orElseGet(Properties::new);
        change.accept(properties);
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
