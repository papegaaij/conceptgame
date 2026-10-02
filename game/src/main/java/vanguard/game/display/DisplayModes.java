package vanguard.game.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Switches between borderless full screen and a resizable window on Alt+Enter or F11 or from the
 * Video tab, and remembers the mode, the monitor and the window bounds for the settings file.
 *
 * <p>Full screen uses the monitor's current video mode, so GLFW attaches the window to the
 * monitor without a mode switch (no flicker, the desktop layout stays intact). The GL context
 * survives the switch: textures, frame buffers and the audio thread are untouched, and
 * {@link vanguard.game.render.PixelScreen} letterboxes to whatever back buffer results.
 */
public final class DisplayModes {
    private static final String TAG = "display";

    private final Consumer<DisplaySettings> store;
    private Optional<Bounds> window;

    /**
     * @param initial the settings the game was started with
     * @param store saves the settings after every switch and when the game ends
     */
    public DisplayModes(DisplaySettings initial, Consumer<DisplaySettings> store) {
        this.store = store;
        this.window = initial.window();
    }

    /** Toggles when Alt+Enter or F11 was pressed since the previous frame. */
    public void poll() {
        boolean alt = Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT);
        if (Gdx.input.isKeyJustPressed(Keys.F11) || alt && Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            toggle();
        }
    }

    /** Whether the game is in full screen now. */
    public boolean fullScreen() {
        return Gdx.graphics.isFullscreen();
    }

    /** Switches to the other mode and stores the settings. */
    public void toggle() {
        Monitor monitor = Gdx.graphics.getMonitor();
        WindowMode mode;
        if (Gdx.graphics.isFullscreen()) {
            Bounds bounds = window.filter(Monitors.workArea(monitor)::containsCentreOf)
                    .orElseGet(() -> Monitors.newWindow(monitor));
            Gdx.graphics.setWindowedMode(bounds.width(), bounds.height());
            lwjglWindow().setPosition(bounds.x(), bounds.y());
            window = Optional.of(bounds);
            mode = WindowMode.WINDOWED;
        } else {
            window = Optional.of(currentWindowBounds());
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode(monitor));
            mode = WindowMode.FULL_SCREEN;
        }
        // The new back buffer size arrives with the next resize event, see resized().
        Gdx.app.log(TAG, "switching to " + mode + " on " + monitor.name);
        store.accept(new DisplaySettings(mode, Optional.of(monitor.name), window));
    }

    /** Logs the outcome of a switch; call from {@code ApplicationListener.resize}. */
    public void resized() {
        Gdx.app.log(
                TAG,
                (Gdx.graphics.isFullscreen() ? "full screen " : "window ")
                        + Gdx.graphics.getBackBufferWidth()
                        + "x"
                        + Gdx.graphics.getBackBufferHeight());
    }

    /** Saves the current mode, monitor and window bounds; call when the game ends. */
    public void storeCurrent() {
        boolean fullScreen = Gdx.graphics.isFullscreen();
        if (!fullScreen) {
            window = Optional.of(currentWindowBounds());
        }
        store.accept(new DisplaySettings(
                fullScreen ? WindowMode.FULL_SCREEN : WindowMode.WINDOWED,
                Optional.of(Gdx.graphics.getMonitor().name),
                window));
    }

    private static Bounds currentWindowBounds() {
        Lwjgl3Window lwjglWindow = lwjglWindow();
        return new Bounds(
                lwjglWindow.getPositionX(),
                lwjglWindow.getPositionY(),
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
    }

    private static Lwjgl3Window lwjglWindow() {
        return ((Lwjgl3Graphics) Gdx.graphics).getWindow();
    }
}
