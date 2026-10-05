package vanguard.game.input;

import com.badlogic.gdx.Input.Keys;

/**
 * The state of every {@link Action}, sampled once per frame from the devices through the
 * {@link Bindings}: whether it is held and whether it was pressed since the previous frame. One
 * instance serves all screens, so a press that switches screens is not seen again by the next one.
 * It also notices the interruptions that pause a level (design/ui/pause): the window losing the
 * focus and a gamepad disconnecting.
 */
public final class ActionInput {
    private static final Action[] ACTIONS = Action.values();

    private Bindings bindings;
    private final boolean[] held = new boolean[ACTIONS.length];
    private final boolean[] pressed = new boolean[ACTIONS.length];
    private boolean sampled;
    private boolean focused;
    private int gamepads;
    private boolean interrupted;
    private boolean interruption;

    public ActionInput(Bindings bindings) {
        this.bindings = bindings;
    }

    /** What triggers each action, for prompts that name the keys. */
    public Bindings bindings() {
        return bindings;
    }

    /** Uses changed bindings from the next {@link #update} on. */
    public void rebind(Bindings changed) {
        bindings = changed;
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
        boolean nowFocused = devices.focused();
        int nowGamepads = devices.gamepads();
        interrupted = interruption || sampled && (focused && !nowFocused || nowGamepads < gamepads);
        interruption = false;
        sampled = true;
        focused = nowFocused;
        gamepads = nowGamepads;
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

    /**
     * Reports an interruption in the next {@link #update}: the window was minimised (libGDX's
     * {@code pause}), which normally also loses the focus, but a level must pause even where it does not.
     */
    public void interrupt() {
        interruption = true;
    }

    /**
     * Whether the window lost the focus, was minimised ({@link #interrupt}) or a gamepad disconnected
     * in the last {@link #update}.
     */
    public boolean interrupted() {
        return interrupted;
    }
}
