package vanguard.game.input;

/**
 * The control part of the settings file (design/ui/options, Controls tab).
 *
 * @param autoFire fire continuously without holding the fire button; off means hold to fire
 * @param deadZone the share of a stick's travel that counts as centred, 0..1
 * @param bindings the keys and gamepad controls of every action
 */
public record ControlSettings(boolean autoFire, double deadZone, Bindings bindings) {
    public static final double DEFAULT_DEAD_ZONE = 0.2;

    /** Hold to fire, a 20 % dead zone and the default bindings (design/ui/controls). */
    public static ControlSettings defaults() {
        return new ControlSettings(false, DEFAULT_DEAD_ZONE, Bindings.defaults());
    }

    public ControlSettings withAutoFire(boolean on) {
        return new ControlSettings(on, deadZone, bindings);
    }

    public ControlSettings withDeadZone(double share) {
        return new ControlSettings(autoFire, share, bindings);
    }

    public ControlSettings withBindings(Bindings changed) {
        return new ControlSettings(autoFire, deadZone, changed);
    }
}
