package vanguard.game.input;

import com.badlogic.gdx.Input.Keys;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * The press-a-key capture of the Controls tab: the first key, or for a gamepad slot the first
 * gamepad button, that goes down after the capture started. What was held when it started (the
 * confirm that opened it) counts only once released and pressed again. Esc and the gamepad's Back
 * cancel; the other fixed system keys (Enter, F11, see {@link Bindings#systemKey}) are never
 * captured.
 */
public final class KeyCapture {
    /** What a poll found. */
    public sealed interface Result {
        record Waiting() implements Result {}

        record Cancelled() implements Result {}

        record Key(int keyCode) implements Result {}

        record Button(GamepadControl button) implements Result {}
    }

    private static final Result WAITING = new Result.Waiting();

    private final boolean gamepad;
    private final Set<Integer> heldKeys = new HashSet<>();
    private final Set<GamepadControl> heldButtons = EnumSet.noneOf(GamepadControl.class);

    /** @param gamepad capture a gamepad button rather than a key */
    public KeyCapture(DeviceState devices, boolean gamepad) {
        this.gamepad = gamepad;
        for (int key = 1; key <= Keys.MAX_KEYCODE; key++) {
            if (devices.keyPressed(key)) {
                heldKeys.add(key);
            }
        }
        for (GamepadControl control : GamepadControl.values()) {
            if (devices.gamepadPressed(control)) {
                heldButtons.add(control);
            }
        }
    }

    public Result poll(DeviceState devices) {
        heldKeys.removeIf(key -> !devices.keyPressed(key));
        heldButtons.removeIf(button -> !devices.gamepadPressed(button));
        if (newly(devices, Keys.ESCAPE) || newly(devices, GamepadControl.BACK)) {
            return new Result.Cancelled();
        }
        if (gamepad) {
            for (GamepadControl button : GamepadControl.BUTTONS) {
                if (newly(devices, button)) {
                    return new Result.Button(button);
                }
            }
            return WAITING;
        }
        for (int key = 1; key <= Keys.MAX_KEYCODE; key++) {
            if (!Bindings.systemKey(key) && newly(devices, key)) {
                return new Result.Key(key);
            }
        }
        return WAITING;
    }

    private boolean newly(DeviceState devices, int key) {
        return devices.keyPressed(key) && !heldKeys.contains(key);
    }

    private boolean newly(DeviceState devices, GamepadControl button) {
        return devices.gamepadPressed(button) && !heldButtons.contains(button);
    }
}
