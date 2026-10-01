package vanguard.game.input;

import com.badlogic.gdx.Input.Keys;

/**
 * The state of every {@link Action}, sampled once per frame from the devices through the
 * {@link Bindings}: whether it is held and whether it was pressed since the previous frame. One
 * instance serves all screens, so a press that switches screens is not seen again by the next one.
 */
public final class ActionInput {
    private static final Action[] ACTIONS = Action.values();

    private final Bindings bindings;
    private final boolean[] held = new boolean[ACTIONS.length];
    private final boolean[] pressed = new boolean[ACTIONS.length];

    public ActionInput(Bindings bindings) {
        this.bindings = bindings;
    }

    /** Samples the devices; call once per frame before the screens read the actions. */
    public void update(DeviceState devices) {
        // Alt+Enter toggles the display mode (design/ui/options), so it must not also confirm.
        boolean alt = devices.keyPressed(Keys.ALT_LEFT) || devices.keyPressed(Keys.ALT_RIGHT);
        for (Action action : ACTIONS) {
            Binding binding = bindings.get(action);
            boolean now = key(devices, binding.primaryKey(), alt) || key(devices, binding.alternativeKey(), alt);
            for (GamepadControl control : binding.gamepad()) {
                now |= devices.gamepadPressed(control);
            }
            int i = action.ordinal();
            pressed[i] = now && !held[i];
            held[i] = now;
        }
    }

    private static boolean key(DeviceState devices, int keyCode, boolean alt) {
        return keyCode != Binding.NO_KEY && !(alt && keyCode == Keys.ENTER) && devices.keyPressed(keyCode);
    }

    public boolean held(Action action) {
        return held[action.ordinal()];
    }

    /** Whether the action went down in the last {@link #update}. */
    public boolean pressed(Action action) {
        return pressed[action.ordinal()];
    }
}
