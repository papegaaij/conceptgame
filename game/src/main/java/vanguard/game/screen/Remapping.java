package vanguard.game.screen;

import com.badlogic.gdx.Input.Keys;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import vanguard.game.input.Action;
import vanguard.game.input.Binding;
import vanguard.game.input.BindingSlot;
import vanguard.game.input.Bindings;
import vanguard.game.input.Bindings.Assignment;
import vanguard.game.input.DeviceState;
import vanguard.game.input.GamepadControl;
import vanguard.game.input.KeyCapture;
import vanguard.game.input.MenuInput;

/**
 * A remapping in the Controls tab (design/ui/controls): "PRESS A KEY..." captures a key or a
 * gamepad button for one slot; when another action already has it, the conflict is shown and
 * confirm swaps the two, back keeps the old bindings.
 */
final class Remapping {
    private Optional<Assignment> target = Optional.empty();
    private Optional<KeyCapture> capture = Optional.empty();
    /** The captured key or button and the slot that already had it. */
    private Optional<Captured> conflict = Optional.empty();

    private record Captured(int key, Optional<GamepadControl> button, String holder) {}

    boolean active() {
        return target.isPresent();
    }

    /** Starts capturing for a slot; what is held now does not count. */
    void start(Assignment slot, DeviceState devices) {
        target = Optional.of(slot);
        capture = Optional.of(new KeyCapture(devices, slot.slot() == BindingSlot.GAMEPAD));
        conflict = Optional.empty();
    }

    /** Follows the capture; returns the changed bindings once a key or button is settled. */
    Optional<Bindings> update(DeviceState devices, MenuInput input, Bindings bindings) {
        if (target.isEmpty()) {
            return Optional.empty();
        }
        Assignment slot = target.get();
        if (conflict.isPresent()) {
            if (input.confirm()) {
                return finish(swapped(bindings, slot, conflict.get()));
            }
            if (input.back()) {
                cancel();
            }
            return Optional.empty();
        }
        switch (capture.orElseThrow().poll(devices)) {
            case KeyCapture.Result.Waiting waiting -> {}
            case KeyCapture.Result.Cancelled cancelled -> cancel();
            case KeyCapture.Result.Key key -> {
                Optional<Assignment> holder = bindings.keyHolder(slot, key.keyCode());
                if (holder.isEmpty()) {
                    return finish(bindings.withKey(slot, key.keyCode()));
                }
                conflict = Optional.of(new Captured(key.keyCode(), Optional.empty(), describe(holder.get())));
            }
            case KeyCapture.Result.Button button -> {
                Optional<Action> holder = bindings.buttonHolder(slot.action(), button.button());
                if (holder.isEmpty()) {
                    return finish(bindings.withButton(slot.action(), button.button()));
                }
                conflict = Optional.of(
                        new Captured(Binding.NO_KEY, Optional.of(button.button()), name(holder.get()) + " (GAMEPAD)"));
            }
        }
        return Optional.empty();
    }

    private static Bindings swapped(Bindings bindings, Assignment slot, Captured captured) {
        return captured.button()
                .map(button -> bindings.withButton(slot.action(), button))
                .orElseGet(() -> bindings.withKey(slot, captured.key()));
    }

    private Optional<Bindings> finish(Bindings changed) {
        cancel();
        return Optional.of(changed);
    }

    private void cancel() {
        target = Optional.empty();
        capture = Optional.empty();
        conflict = Optional.empty();
    }

    /** The line under the table while capturing: the prompt, or the conflict and how to settle it. */
    Optional<String> message() {
        if (target.isEmpty()) {
            return Optional.empty();
        }
        if (conflict.isEmpty()) {
            return Optional.of(
                    target.get().slot() == BindingSlot.GAMEPAD
                            ? "PRESS A GAMEPAD BUTTON...   ESC CANCELS"
                            : "PRESS A KEY...   ESC CANCELS");
        }
        Captured captured = conflict.get();
        String what = captured.button().map(GamepadControl::label).orElseGet(() -> keyName(captured.key()));
        return Optional.of("CONFLICT: '" + what + "' IS ALREADY " + captured.holder() + " - ENTER SWAPS THE TWO");
    }

    /** The slot being captured. */
    Optional<Assignment> target() {
        return target;
    }

    private static String describe(Assignment assignment) {
        return name(assignment.action()) + " (" + assignment.slot().name() + ")";
    }

    static String name(Action action) {
        return action.name().replace('_', ' ');
    }

    /** A key's name as the table shows it: {@code SPACE}, {@code L-CTRL}; a dash for none. */
    static String keyName(int key) {
        return key == Binding.NO_KEY ? "-" : Keys.toString(key).toUpperCase(Locale.ROOT);
    }

    /** A gamepad slot's controls as the table shows them; the move actions keep the D-pad and the stick. */
    static String gamepadName(Action action, Binding binding) {
        if (!action.gamepadRemappable()) {
            return "D-PAD / L-STICK";
        }
        return binding.gamepad().stream().sorted().map(GamepadControl::label).collect(Collectors.joining(" / "));
    }
}
