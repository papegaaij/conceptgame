package vanguard.game.input;

/**
 * The control part of the settings file (design/ui/options, Controls tab).
 *
 * @param autoFire fire continuously without holding the fire button; off means hold to fire
 */
public record ControlSettings(boolean autoFire) {
    /** Hold to fire, the default. */
    public static ControlSettings defaults() {
        return new ControlSettings(false);
    }
}
