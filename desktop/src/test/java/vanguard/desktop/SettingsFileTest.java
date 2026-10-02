package vanguard.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import vanguard.game.settings.VideoSettings;

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

    @Test
    void theOptionsStartAtTheirDefaultsWithoutAFile() {
        assertEquals(Settings.defaults(), new SettingsFile(directory.resolve("settings.properties")).readSettings());
    }

    @Test
    void readsBackEveryOptionItWrote() {
        var file = new SettingsFile(directory.resolve("settings.properties"));
        Bindings bindings = Bindings.defaults()
                .withKey(new Bindings.Assignment(Action.SPECIAL, BindingSlot.PRIMARY), Keys.Z)
                .withKey(new Bindings.Assignment(Action.DASH, BindingSlot.ALTERNATIVE), Keys.SHIFT_RIGHT)
                .withButton(Action.FIRE, GamepadControl.Y);
        var settings = new Settings(
                new VideoSettings(Scaling.SHARP_BILINEAR, true),
                new AudioSettings(0.8, 0.55, 0.3, 0.05),
                new ControlSettings(true, 0.35, bindings),
                new GameplaySettings(60, 0.4, true));

        file.save(settings);

        assertEquals(settings, file.readSettings());
    }

    @Test
    void anUnboundSlotStaysUnbound() {
        var file = new SettingsFile(directory.resolve("settings.properties"));
        var settings = Settings.defaults();

        file.save(settings);

        assertEquals(
                Binding.NO_KEY,
                file.readSettings().controls().bindings().get(Action.DASH).primaryKey());
    }

    @Test
    void invalidOptionsFallBackOneByOne() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, """
                audio.music=1.5
                audio.radio=0.25
                controls.dead-zone=loose
                controls.fire.primary=NoSuchKey
                controls.fire.gamepad=A,LEFT_STICK_UP
                controls.special.alternative=Q
                gameplay.text-speed=1000
                video.scaling=blurry
                """);

        Settings settings = new SettingsFile(path).readSettings();

        assertEquals(1, settings.audio().music());
        assertEquals(0.25, settings.audio().radio());
        assertEquals(ControlSettings.DEFAULT_DEAD_ZONE, settings.controls().deadZone());
        Bindings defaults = Bindings.defaults();
        assertEquals(defaults.get(Action.FIRE), settings.controls().bindings().get(Action.FIRE));
        assertEquals(Keys.Q, settings.controls().bindings().get(Action.SPECIAL).alternativeKey());
        assertEquals(GameplaySettings.DEFAULT_TEXT_SPEED, settings.gameplay().textSpeed());
        assertEquals(Scaling.INTEGER, settings.video().scaling());
    }

    @Test
    void theDisplayAndTheOptionsKeepEachOthersKeys() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "controls.auto-fire=true\nwindow.x=5\n");
        var file = new SettingsFile(path);

        file.write(new DisplaySettings(WindowMode.FULL_SCREEN, Optional.of("DP-1"), Optional.empty()));
        assertTrue(file.readSettings().controls().autoFire());
        assertFalse(Files.readString(path).contains("window."), "stale window bounds are dropped");

        file.save(Settings.defaults().withAudio(new AudioSettings(0.5, 1, 1, 1)));

        assertEquals(Optional.of("DP-1"), file.read().monitor());
        assertEquals(0.5, file.readSettings().audio().master());
    }
}
