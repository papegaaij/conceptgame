package vanguard.game.input;

import com.badlogic.gdx.Input.Keys;
import java.util.EnumMap;
import java.util.Map;

/**
 * The binding of every {@link Action}. Immutable: a remapping (the Options screen, M3) makes a
 * changed copy with {@link #with}.
 */
public final class Bindings {
    private final Map<Action, Binding> bindings;

    private Bindings(Map<Action, Binding> bindings) {
        this.bindings = new EnumMap<>(bindings);
        if (this.bindings.size() != Action.values().length) {
            throw new IllegalArgumentException("every action needs a binding");
        }
    }

    /**
     * The defaults of design/ui/controls. Dash's primary input is a double-tap on a direction, which
     * comes with the evasive thrusters module; until then only V and the left bumper are bound.
     */
    public static Bindings defaults() {
        Map<Action, Binding> map = new EnumMap<>(Action.class);
        map.put(Action.MOVE_UP, Binding.of(Keys.UP, Keys.W, GamepadControl.LEFT_STICK_UP, GamepadControl.DPAD_UP));
        map.put(
                Action.MOVE_DOWN,
                Binding.of(Keys.DOWN, Keys.S, GamepadControl.LEFT_STICK_DOWN, GamepadControl.DPAD_DOWN));
        map.put(
                Action.MOVE_LEFT,
                Binding.of(Keys.LEFT, Keys.A, GamepadControl.LEFT_STICK_LEFT, GamepadControl.DPAD_LEFT));
        map.put(
                Action.MOVE_RIGHT,
                Binding.of(Keys.RIGHT, Keys.D, GamepadControl.LEFT_STICK_RIGHT, GamepadControl.DPAD_RIGHT));
        map.put(Action.FIRE, Binding.of(Keys.SPACE, Keys.Z, GamepadControl.A, GamepadControl.RIGHT_TRIGGER));
        map.put(Action.SPECIAL, Binding.of(Keys.X, Keys.CONTROL_LEFT, GamepadControl.B));
        map.put(Action.PRECISION, Binding.of(Keys.SHIFT_LEFT, Keys.C, GamepadControl.RIGHT_BUMPER));
        map.put(Action.DASH, Binding.of(Binding.NO_KEY, Keys.V, GamepadControl.LEFT_BUMPER));
        map.put(Action.PAUSE, Binding.of(Keys.ESCAPE, Keys.P, GamepadControl.START));
        map.put(Action.MENU_CONFIRM, Binding.of(Keys.ENTER, Binding.NO_KEY, GamepadControl.A));
        map.put(Action.MENU_BACK, Binding.of(Keys.ESCAPE, Binding.NO_KEY, GamepadControl.B, GamepadControl.BACK));
        return new Bindings(map);
    }

    public Binding get(Action action) {
        return bindings.get(action);
    }

    /** A copy with {@code action} bound to {@code binding}. */
    public Bindings with(Action action, Binding binding) {
        Map<Action, Binding> changed = new EnumMap<>(bindings);
        changed.put(action, binding);
        return new Bindings(changed);
    }
}
