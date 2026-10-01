package vanguard.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import vanguard.game.GameOptions;
import vanguard.game.SpikeGame;

/** Starts the spike in a 1920x1080 window (the 960x540 internal screen at 2x). */
public final class DesktopLauncher {
    /** Raised from libGDX's default of 16 so 32 effects plus the music stream can play at once. */
    private static final int AUDIO_SOURCES = 64;
    /** Music stream buffers: 8 x 4096 bytes = 186 ms of 44.1 kHz stereo. */
    private static final int AUDIO_BUFFER_BYTES = 4096;
    private static final int AUDIO_BUFFER_COUNT = 8;

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        GameOptions options = GameOptions.parse(args);
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Terran Vanguard spike");
        config.setWindowedMode(1920, 1080);
        config.useVsync(options.vsync());
        config.setForegroundFPS(0); // uncapped; vsync paces the frames when it is on
        config.setAudioConfig(AUDIO_SOURCES, AUDIO_BUFFER_BYTES, AUDIO_BUFFER_COUNT);
        new Lwjgl3Application(new SpikeGame(options), config);
    }
}
