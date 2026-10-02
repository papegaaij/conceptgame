package vanguard.game.input;

import com.badlogic.gdx.Input.Keys;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The binding of every {@link Action}. Immutable: a remapping makes a changed copy. Remapping
 * swaps on a conflict (design/ui/controls): a key or button that another remappable action
 * already has goes to the new action, and the other action gets what the new one had.
 */
public final class Bindings {
    /** An action's slot: where a key or button is bound. */
    public record Assignment(Action action, BindingSlot slot) {}

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
     * The menus navigate with the arrows, the D-pad and the left stick, switch tabs with Q / E and
     * the bumpers, confirm with Enter / A and go back with Esc / B / Back.
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
        map.put(
                Action.MENU_UP,
                Binding.of(Keys.UP, Binding.NO_KEY, GamepadControl.DPAD_UP, GamepadControl.LEFT_STICK_UP));
        map.put(
                Action.MENU_DOWN,
                Binding.of(Keys.DOWN, Binding.NO_KEY, GamepadControl.DPAD_DOWN, GamepadControl.LEFT_STICK_DOWN));
        map.put(
                Action.MENU_LEFT,
                Binding.of(Keys.LEFT, Binding.NO_KEY, GamepadControl.DPAD_LEFT, GamepadControl.LEFT_STICK_LEFT));
        map.put(
                Action.MENU_RIGHT,
                Binding.of(Keys.RIGHT, Binding.NO_KEY, GamepadControl.DPAD_RIGHT, GamepadControl.LEFT_STICK_RIGHT));
        map.put(Action.MENU_CONFIRM, Binding.of(Keys.ENTER, Binding.NO_KEY, GamepadControl.A));
        map.put(Action.MENU_BACK, Binding.of(Keys.ESCAPE, Binding.NO_KEY, GamepadControl.B, GamepadControl.BACK));
        map.put(Action.MENU_PREVIOUS_TAB, Binding.of(Keys.Q, Binding.NO_KEY, GamepadControl.LEFT_BUMPER));
        map.put(Action.MENU_NEXT_TAB, Binding.of(Keys.E, Binding.NO_KEY, GamepadControl.RIGHT_BUMPER));
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

    /** The remappable key slot other than {@code target} that has {@code key}. */
    public Optional<Assignment> keyHolder(Assignment target, int key) {
        for (Action action : Action.REMAPPABLE) {
            for (BindingSlot slot : new BindingSlot[] {BindingSlot.PRIMARY, BindingSlot.ALTERNATIVE}) {
                var assignment = new Assignment(action, slot);
                if (!assignment.equals(target) && get(action).key(slot) == key) {
                    return Optional.of(assignment);
                }
            }
        }
        return Optional.empty();
    }

    /** A copy with {@code key} in the target's key slot; a slot that had it gets the target's old key. */
    public Bindings withKey(Assignment target, int key) {
        int old = get(target.action()).key(target.slot());
        Bindings changed = with(target.action(), get(target.action()).withKey(target.slot(), key));
        Optional<Assignment> holder = keyHolder(target, key);
        if (holder.isEmpty()) {
            return changed;
        }
        Assignment other = holder.get();
        return changed.with(other.action(), changed.get(other.action()).withKey(other.slot(), old));
    }

    /** The action other than {@code target} whose remappable gamepad slot has {@code button}. */
    public Optional<Action> buttonHolder(Action target, GamepadControl button) {
        return Action.REMAPPABLE.stream()
                .filter(action -> action != target && action.gamepadRemappable())
                .filter(action -> get(action).gamepad().contains(button))
                .findFirst();
    }

    /**
     * A copy with the target's gamepad slot set to {@code button} alone; an action that had the button
     * gets the target's old buttons instead.
     */
    public Bindings withButton(Action target, GamepadControl button) {
        if (!target.gamepadRemappable()) {
            throw new IllegalArgumentException(target + " keeps its gamepad controls");
        }
        Set<GamepadControl> old = get(target).gamepad();
        Bindings changed = with(target, get(target).withGamepad(Set.of(button)));
        Optional<Action> holder = buttonHolder(target, button);
        if (holder.isEmpty()) {
            return changed;
        }
        Action other = holder.get();
        Set<GamepadControl> swapped = EnumSet.noneOf(GamepadControl.class);
        swapped.addAll(old);
        get(other).gamepad().stream().filter(c -> c != button).forEach(swapped::add);
        return changed.with(other, get(other).withGamepad(swapped));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Bindings that && bindings.equals(that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindings);
    }
}
