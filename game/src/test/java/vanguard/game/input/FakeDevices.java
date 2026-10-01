package vanguard.game.input;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Devices whose pressed keys and gamepad controls the test sets. */
final class FakeDevices implements DeviceState {
    final Set<Integer> keys = new HashSet<>();
    final Set<GamepadControl> gamepad = EnumSet.noneOf(GamepadControl.class);

    @Override
    public boolean keyPressed(int keyCode) {
        return keys.contains(keyCode);
    }

    @Override
    public boolean gamepadPressed(GamepadControl control) {
        return gamepad.contains(control);
    }
}
