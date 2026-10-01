package vanguard.game.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import org.junit.jupiter.api.Test;

class ActionInputTest {
    private final FakeDevices devices = new FakeDevices();
    private final ActionInput input = new ActionInput(Bindings.defaults());

    @Test
    void primaryAlternativeKeyAndGamepadAllTriggerAnAction() {
        for (Runnable press : new Runnable[] {
            () -> devices.keys.add(Keys.SPACE),
            () -> devices.keys.add(Keys.Z),
            () -> devices.gamepad.add(GamepadControl.A),
            () -> devices.gamepad.add(GamepadControl.RIGHT_TRIGGER)
        }) {
            devices.keys.clear();
            devices.gamepad.clear();
            input.update(devices);
            press.run();

            input.update(devices);

            assertTrue(input.held(Action.FIRE));
        }
    }

    @Test
    void pressedIsTrueOnlyInTheFrameTheActionWentDown() {
        devices.keys.add(Keys.ESCAPE);
        input.update(devices);
        assertTrue(input.pressed(Action.PAUSE));
        assertTrue(input.pressed(Action.MENU_BACK));

        input.update(devices);

        assertTrue(input.held(Action.PAUSE));
        assertFalse(input.pressed(Action.PAUSE));
    }

    @Test
    void altEnterTogglesTheDisplayModeAndDoesNotConfirm() {
        devices.keys.add(Keys.ALT_LEFT);
        devices.keys.add(Keys.ENTER);
        input.update(devices);
        assertFalse(input.pressed(Action.MENU_CONFIRM));

        devices.keys.remove(Keys.ALT_LEFT);
        devices.keys.remove(Keys.ENTER);
        input.update(devices);
        devices.keys.add(Keys.ENTER);
        input.update(devices);

        assertTrue(input.pressed(Action.MENU_CONFIRM));
    }

    @Test
    void theStickMovesTheShip() {
        devices.gamepad.add(GamepadControl.LEFT_STICK_LEFT);

        input.update(devices);

        assertTrue(input.held(Action.MOVE_LEFT));
        assertFalse(input.held(Action.MOVE_RIGHT));
    }

    @Test
    void aRemappedActionFollowsItsNewKey() {
        var remapped = new ActionInput(Bindings.defaults().with(Action.FIRE, Binding.of(Keys.J, Keys.K)));
        devices.keys.add(Keys.SPACE);
        remapped.update(devices);
        assertFalse(remapped.held(Action.FIRE));

        devices.keys.add(Keys.K);
        remapped.update(devices);

        assertTrue(remapped.held(Action.FIRE));
    }
}
