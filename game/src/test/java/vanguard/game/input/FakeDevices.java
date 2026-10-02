package vanguard.game.input;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Devices whose pressed keys, gamepad controls, gamepad count and focus the test sets. */
public final class FakeDevices implements DeviceState {
    public final Set<Integer> keys = new HashSet<>();
    public final Set<GamepadControl> gamepad = EnumSet.noneOf(GamepadControl.class);
    public int gamepads = 1;
    public boolean focused = true;

    @Override
    public boolean keyPressed(int keyCode) {
        return keys.contains(keyCode);
    }

    @Override
    public boolean gamepadPressed(GamepadControl control) {
        return gamepad.contains(control);
    }

    @Override
    public int gamepads() {
        return gamepads;
    }

    @Override
    public boolean focused() {
        return focused;
    }
}
