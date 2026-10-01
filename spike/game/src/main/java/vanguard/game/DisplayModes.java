package vanguard.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input.Keys;

/**
 * Switches between borderless full screen and a resizable window on Alt+Enter or F11.
 *
 * <p>Full screen uses the monitor's current video mode, so GLFW attaches the window to the
 * monitor without a mode switch (no flicker, the desktop layout stays intact). The GL context
 * survives the switch: textures, frame buffers and the audio thread are untouched, and
 * {@link vanguard.game.render.PixelScreen} letterboxes to whatever back buffer results.
 */
public final class DisplayModes {
    private static final String TAG = "display";

    private int windowWidth;
    private int windowHeight;
    private int toggles;

    /** @param windowWidth  logical width of the window to return to from full screen */
    public DisplayModes(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    /** Toggles when Alt+Enter or F11 was pressed since the previous frame. */
    public void poll() {
        boolean alt = Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT);
        if (Gdx.input.isKeyJustPressed(Keys.F11) || alt && Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            toggle();
        }
    }

    public void toggle() {
        Graphics graphics = Gdx.graphics;
        boolean toWindow = graphics.isFullscreen();
        boolean switched;
        if (toWindow) {
            switched = graphics.setWindowedMode(windowWidth, windowHeight);
        } else {
            windowWidth = graphics.getWidth();
            windowHeight = graphics.getHeight();
            switched = graphics.setFullscreenMode(graphics.getDisplayMode(graphics.getMonitor()));
        }
        toggles++;
        // The new back buffer size arrives with the next resize event, see resized().
        Gdx.app.log(TAG, "switching to " + (toWindow ? "window" : "full screen") + (switched ? "" : " refused"));
    }

    /** Logs the outcome of a switch; call from {@code ApplicationListener.resize}. */
    public void resized() {
        Graphics graphics = Gdx.graphics;
        Gdx.app.log(TAG, (graphics.isFullscreen() ? "full screen " : "window ")
                + graphics.getBackBufferWidth() + "x" + graphics.getBackBufferHeight());
    }

    public int toggles() {
        return toggles;
    }
}
