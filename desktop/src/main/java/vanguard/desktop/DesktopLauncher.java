package vanguard.desktop;

import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import vanguard.game.TerranVanguard;
import vanguard.game.display.Bounds;
import vanguard.game.display.DisplayModes;
import vanguard.game.display.DisplaySettings;
import vanguard.game.display.Monitors;
import vanguard.game.display.WindowMode;

/**
 * Starts the game in the display mode, on the monitor and at the window position of the settings
 * file: on the first start in borderless full screen. Alt+Enter or F11 switch modes at any time.
 */
public final class DesktopLauncher {
    /** Raised from libGDX's default of 16 so many effects plus the music stream can play at once. */
    private static final int AUDIO_SOURCES = 64;
    /** Stream buffers: 8 x 4096 bytes = 186 ms of 44.1 kHz stereo (bytes, not samples as libGDX's Javadoc says). */
    private static final int AUDIO_BUFFER_BYTES = 4096;

    private static final int AUDIO_BUFFER_COUNT = 8;

    private DesktopLauncher() {}

    public static void main(String[] args) {
        LaunchOptions options = LaunchOptions.parse(args);
        var settingsFile = new SettingsFile(
                options.settingsFile().orElseGet(() -> ConfigDirectory.current().resolve(SettingsFile.FILE_NAME)));
        DisplaySettings settings = settingsFile.read();
        System.out.println("settings: " + settingsFile.path());
        var displayModes = new DisplayModes(settings, settingsFile::write);
        new Lwjgl3Application(new TerranVanguard(displayModes, options.benchSeconds()), configuration(settings));
    }

    private static Lwjgl3ApplicationConfiguration configuration(DisplaySettings settings) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Terran Vanguard");
        Monitor[] monitors = Lwjgl3ApplicationConfiguration.getMonitors();
        Monitor monitor =
                Monitors.find(monitors, settings.monitor(), Lwjgl3ApplicationConfiguration.getPrimaryMonitor());
        Bounds window = settings.window()
                .filter(bounds -> Monitors.isOnAny(monitors, bounds))
                .orElseGet(() -> Monitors.newWindow(monitor));
        config.setWindowedMode(window.width(), window.height());
        config.setWindowPosition(window.x(), window.y());
        if (settings.mode() == WindowMode.FULL_SCREEN) {
            // The monitor's current mode: GLFW attaches the window without a video mode switch.
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode(monitor));
        }
        config.useVsync(true);
        config.setAudioConfig(AUDIO_SOURCES, AUDIO_BUFFER_BYTES, AUDIO_BUFFER_COUNT);
        return config;
    }
}
