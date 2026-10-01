package vanguard.game.display;

import java.util.Optional;

/**
 * The display part of the settings file.
 *
 * @param mode the mode to start in
 * @param monitor the name of the monitor the game was last on; empty for the primary monitor
 * @param window where the window was last; empty until the game has run in a window
 */
public record DisplaySettings(WindowMode mode, Optional<String> monitor, Optional<Bounds> window) {
    /** The first start opens in full screen on the primary monitor. */
    public static DisplaySettings firstStart() {
        return new DisplaySettings(WindowMode.FULL_SCREEN, Optional.empty(), Optional.empty());
    }
}
